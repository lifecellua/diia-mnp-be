package com.lifecell.diia.be.service.stages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.exception.LifecellException;
import com.lifecell.diia.be.model.db.EDocument;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseData;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.model.fsm.State;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.service.DocumentsService;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.TemplateService;
import com.lifecell.diia.be.util.FsmUtils;
import com.lifecell.diia.be.util.LifecellUtils;
import com.lifecell.diia.be.util.ResourceUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class AgreementStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class RequestData extends Request {

        public static enum Command {
            DOCUMENT,
            CONTINUE
        }

        private Command command;
    }

    @Getter
    @Builder
    private static class CustomerDocumentData {
        private String type;
        private String data;
    }

    private static final String TARGET_NAME = "agreement";
    private static final String TEMPLATE_NAME = "agreement";

    private final DiiaService diiaService;
    private final DocumentsService documentsService;
    private final LifecellService lifecellService;
    private final TemplateService templateService;

    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    private final Duration WAIT_TIME;
    private final Duration SLEEP_TIME;

    public AgreementStage(
            DiiaService diiaService,
            DocumentsService documentsService,
            LifecellService lifecellService,
            Properties properties,
            TemplateService templateService) {
        this.diiaService = diiaService;
        this.documentsService = documentsService;
        this.lifecellService = lifecellService;
        this.templateService = templateService;

        this.executorService = Executors.newWorkStealingPool();
        this.objectMapper = ResourceUtils.getDefaultObjectMapper();

        this.WAIT_TIME = properties.getFlow().getStage().getAgreement().getFile().getWait();
        this.SLEEP_TIME = properties.getFlow().getStage().getAgreement().getFile().getTimeout();
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_AGREEMENT;
    }

    @Override
    public Response enter(Context context) {
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response form(Context context) {
        var pc = diiaService.userGetPcDocument(context.getFlow().getId().toString(), context.getSession().getUserRef());
        var xd = diiaService.userGetLatestIdDocument(context.getFlow().getId().toString(), context.getSession().getUserRef());
        if (xd == null) {
            throw new FsmException(Problem.FAIL, "Can't get latest identification document");
        }
        var dx = lifecellService.getPortingDates(context.getFlow().getId().toString(), context.getFlow().getUserRef());
        return ResponseForm.builder()
                .form(templateService.generate(TEMPLATE_NAME,
                        TemplateService.mapOf("NAME",
                                        Stream.of(xd.getSecondName(), xd.getFirstName(), xd.getMiddleName())
                                                .filter((it) -> it != null)
                                                .collect(Collectors.joining(" ")),
                                "DOCUMENT_TYPE", xd.getType().getTitle(),
                                "DOCUMENT_ID",
                                        Stream.of(xd.getSeries(), xd.getNumber())
                                                .filter((it) -> it != null)
                                                .collect(Collectors.joining(" ")),
                                "PHONE_NUMBER", context.getFlow().getMsisdn(),
                                "PC", (pc == null)
                                    ? TemplateService.Operation.REMOVE
                                    : TemplateService.mapOf("DOCUMENT_TYPE_AND_ID",
                                                Stream.of(pc.getType().getTitle(), pc.getSeries(), pc.getNumber())
                                                        .filter((it) -> it != null)
                                                        .collect(Collectors.joining(" "))),
                                "PORTING_DATE",
                                        TemplateService.mapOf("DATE", dx.getPortingDate()),
                                "CANCELING_DATE", (dx.getCancelingDateAvailable()
                                    ? TemplateService.mapOf("DATE", dx.getCancelingDate())
                                    : TemplateService.Operation.REMOVE))
                        )
                    )
                .build();
    }

    private Response getDocument(Context context) {
        //create document
        if (context.getFlow().getDocument() == null) {
            var cdd = createCustomerDocumentData(context);

            var entity = createDocumentTask(context.getFlow().getId().toString(), context.getFlow().getUserRef(),
                    context.getFlow().getMsisdn(), context.getFlow().getTariffRef(), cdd);

            context.getFlow().setDocument(AggregateReference.to(entity.getId()));

            context.update();
        }

        //await docuemnt
        var dId = context.getFlow().getDocument().getId();
        var dRef = "";
        var at = LocalDateTime.now();
        var await = WAIT_TIME.minus(SLEEP_TIME);
        var step = 1;
        log.info("[{}] Waiting for agreement document (for {} every {})", context.getFlow().getId(), WAIT_TIME, SLEEP_TIME);
        while ((Duration.between(at, LocalDateTime.now()).compareTo(await) < 0)) {
            if (step == 1) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    break;
                }
                var entity = documentsService.getById(dId);
                if (entity != null) {
                    var state = State.fromId(entity.getStateId());
                    switch (state) {
                        case PROGRESS, SUCCESS: {
                            dRef = entity.getRef();
                            step = 2;
                            continue;
                            //break;
                        }
                        case UNKNOWN: {
                            continue;
                            //break;
                        }
                        default: {
                            var ec = entity.getErrorCode();
                            var rProblem = Problem.FAIL;
                            if (ec != null) {
                                var rCode = ResultCode.fromCode(ec);
                                rProblem = LifecellUtils.toProblem(rCode);
                                if (!rProblem.isFatal()) {
                                    rProblem = Problem.FAIL;
                                }
                            }
                            throw new FsmException(rProblem, "Fail creating document with id {}", dId);
                        }
                    }
                } else {
                    throw new FsmException(Problem.FAIL, "There is no document with id {}", dId);
                }
            } else if (step == 2) {
                try {
                    Thread.sleep(SLEEP_TIME.toMillis());
                } catch (InterruptedException e) {
                    break;
                }
                try {
                    log.info("[{}] Getting agreement document", context.getFlow().getId());
                    var data = lifecellService.getDocument(context.getFlow().getId().toString(), context.getSession().getUserRef(), context.getFlow().getMsisdn(), dRef);
                    log.info("[{}] Got agreement document", context.getFlow().getId());

                    var entity = documentsService.getById(dId);
                    if (entity != null) {
                        entity.setStateId(State.SUCCESS.getId());
                        documentsService.save(entity);
                    }

                    return ResponseData.builder()
                            .data(data)
                            .build();
                } catch (LifecellException e) {
                    log.warn("[{}] Unsuccessful getting document: {}", context.getFlow().getId(), e.getMessage(), e);
                }
            } else {
                throw new FsmException(Problem.FAIL, "Unknown getting document step", step);
            }
        }
        log.warn("[{}] Agreement document not found", context.getFlow().getId());

        var entity = documentsService.getById(dId);
        if (entity != null) {
            entity.setStateId(State.FAIL.getId());
            entity.setErrorCode(ResultCode.NOT_FOUND_DOCUMENT .getCode());
            entity.setErrorRef(ResultCode.NOT_FOUND_DOCUMENT.name());
            entity.setErrorInfo(StringUtils.truncate(ResultCode.NOT_FOUND_DOCUMENT.getDescription()));
            documentsService.save(entity);
        }

        return ResponseNotify.builder()
                .problem(Problem.INVALID_DOCUMENT)
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        switch (data.getCommand()) {
            case DOCUMENT: {
                return getDocument(context);
                //break;
            }
            case CONTINUE: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_SIM_INTRO)
                        .build();
                //break;
            }
            default: {
                throw new FsmException(Problem.FAIL, "Unknown command");
            }
        }
    }

    private CustomerDocumentData createCustomerDocumentData(Context context) {
        var xd = diiaService.userGetLatestIdDocument(context.getFlow().getId().toString(), context.getSession().getUserRef());
        if (xd == null) {
            throw new FsmException(Problem.FAIL, "Can't get latest identification document");
        }
        var data64 = "";
        try {
            data64 = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(xd));
        } catch (Exception e) {
            throw new FsmException(Problem.FAIL, "Can't serialize customer documents", e);
        }
        return CustomerDocumentData.builder()
                .type(xd.getType().getRef())
                .data(data64)
                .build();
    }

    private EDocument createDocumentTask(String transactionId, String userRef, String msisdn, String tariffRef, CustomerDocumentData documentData) {
        var entity = documentsService.create();
        entity.setUserRef(userRef);
        entity.setRef(null);
        entity.setStateId(State.UNKNOWN.getId());
        documentsService.save(entity);

        var documentId = entity.getId();

        executorService.submit(() -> {
            try {
                var ref = lifecellService.generateDocument(transactionId, userRef, msisdn, tariffRef, documentData.getType(), documentData.getData());
                var document = documentsService.getById(documentId);
                document.setRef(ref);
                document.setStateId(State.PROGRESS.getId());
                documentsService.save(document);
            } catch (LifecellException e) {
                log.error("[{}] Error creating document with id {}", transactionId, documentId, e);
                var document = documentsService.getById(documentId);
                document.setStateId(State.FAIL.getId());
                document.setErrorCode(e.getResultCode().getCode());
                document.setErrorRef(e.getResultCode().name());
                document.setErrorInfo(StringUtils.truncate(e.getResultCode().getDescription()));
                documentsService.save(document);
            } catch (Exception e) {
                log.error("[{}] Error creating document with id {}", transactionId, documentId, e);
                var document = documentsService.getById(documentId);
                document.setStateId(State.FAIL.getId());
                document.setErrorCode(ResultCode.UNKNOWN_ERROR.getCode());
                document.setErrorRef(ResultCode.UNKNOWN_ERROR.name());
                document.setErrorInfo(StringUtils.truncate(e.getMessage()));
                documentsService.save(document);
            }
        });

        return entity;
    }
}
