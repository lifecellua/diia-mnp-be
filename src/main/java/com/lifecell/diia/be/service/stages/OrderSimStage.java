package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.TemplateService;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import static com.lifecell.diia.be.util.LifecellUtils.toProblem;

@Service
public class OrderSimStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {

        public static enum Command {
            COMMIT,
            SKIP
        }

        private Command command;
        private String iccid;
        private String puk1;
    }

    private static final String TARGET_NAME = "order-sim";
    private static final String TEMPLATE_NAME = "sim";
    private static final Pattern PATTERN_ICCD = Pattern.compile("\\d+");
    private static final Pattern PATTERN_PUK1 = Pattern.compile("\\d+");

    private final LifecellService lifecellService;
    private final TemplateService templateService;

    public OrderSimStage(
            LifecellService lifecellService,
            TemplateService templateService) {
        this.lifecellService = lifecellService;
        this.templateService = templateService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ORDER_SIM;
    }

    @Override
    public Response enter(Context context) {
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response form(Context context) {
        return ResponseForm.builder()
                .form(templateService.generate(TEMPLATE_NAME,
                        TemplateService.mapOf("CANCEL", TemplateService.Operation.REMOVE)))
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        switch (data.getCommand()) {
            case COMMIT: {
                var iccid = data.getIccid();
                var p1m = PATTERN_ICCD.matcher(iccid);
                if (!p1m.matches()) {
                    return ResponseNotify.builder()
                            .problem(Problem.INVALID_ICCID)
                            .build();
                }
                var puk1 = data.getPuk1();
                var p2m = PATTERN_PUK1.matcher(puk1);
                if (!p2m.matches()) {
                    return ResponseNotify.builder()
                            .problem(Problem.INVALID_PUK1)
                            .build();
                }
                var rCode = lifecellService.updateOrder(context.getFlow().getId().toString(), context.getSession().getUserRef(), context.getOrder().getRef(),
                        context.getOrder().getMsisdn(), iccid, puk1, context.getFlow().getToken());
                var rProblem = toProblem(rCode);
                if (rProblem == Problem.SUCCESS) {
                    return ResponseTransit.builder()
                            .stage(Stage.STAGE_ORDER_SIM_SUCCESS)
                            .build();
                } else {
                    if (rProblem.isFatal()) {
                        throw new FsmException(rProblem, "Fail updating order");
                    }
                    return ResponseNotify.builder()
                            .problem(rProblem)
                            .build();
                }
                //break;
            }
            case SKIP: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_ORDER)
                        .build();
                //break;
            }
            default: {
                throw new FsmException(Problem.FAIL, "Unknown command");
            }
        }
    }
}
