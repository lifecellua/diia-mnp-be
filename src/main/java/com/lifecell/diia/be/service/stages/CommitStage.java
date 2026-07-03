package com.lifecell.diia.be.service.stages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.exception.LifecellException;
import com.lifecell.diia.be.model.db.EOrder;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.model.fsm.State;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.OrdersService;
import com.lifecell.diia.be.util.FsmUtils;
import com.lifecell.diia.be.util.LifecellUtils;
import com.lifecell.diia.be.util.ResourceUtils;
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
public class CommitStage implements StageHandler {
    private static final String TARGET_NAME = "commit";

    private final DiiaService diiaService;
    private final LifecellService lifecellService;
    private OrdersService ordersService;

    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    private final Duration WAIT_TIME;
    private final Duration SLEEP_TIME;

    public CommitStage(
            DiiaService diiaService,
            LifecellService lifecellService,
            OrdersService ordersService,
            Properties properties) {
        this.diiaService = diiaService;
        this.lifecellService = lifecellService;
        this.ordersService = ordersService;

        this.executorService = Executors.newWorkStealingPool();
        this.objectMapper = ResourceUtils.getDefaultObjectMapper();

        this.WAIT_TIME = properties.getFlow().getStage().getCommit().getWait();
        this.SLEEP_TIME = properties.getFlow().getStage().getCommit().getTimeout();
    }


    @Override
    public Stage getStage() {
        return Stage.STAGE_COMMIT;
    }

    @Override
    public Response enter(Context context) {
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        FsmUtils.toRequest(request, RequestEmpty.class);

        //create order
        if (context.getFlow().getOrder() == null) {
            var cdd = createCustomerDocumentData(context);

            var entity = createOrderTask(context.getFlow().getId().toString(), context.getSession().getUserRef(),
                    context.getFlow().getMsisdn(), context.getFlow().getTariffRef(), context.getFlow().getTariffName(),
                    context.getFlow().getToken(), context.getFlow().getIccid(), null,
                    cdd);

            context.getFlow().setOrder(AggregateReference.to(entity.getId()));
            context.setOrder(entity);
            context.update();
        }

        //await order
        var oId = context.getFlow().getOrder().getId();
        var at = LocalDateTime.now();
        var await = WAIT_TIME.minus(SLEEP_TIME);
        log.info("[{}] Waiting for order (for {} every {})", context.getFlow().getId(), WAIT_TIME, SLEEP_TIME);
        while ((Duration.between(at, LocalDateTime.now()).compareTo(await) < 0)) {
            log.info("[{}] Getting order", context.getFlow().getId());
            var order = ordersService.getById(oId);
            if (order != null) {
                var state = State.fromId(order.getStateId());
                switch (state) {
                    case PROGRESS, SUCCESS: {
                        log.info("[{}] Got order", context.getFlow().getId());
                        return ResponseTransit.builder()
                                .stage(Stage.STAGE_SUCCESS)
                                .build();
                        //break;
                    }
                    case UNKNOWN: {
                        try {
                            Thread.sleep(SLEEP_TIME.toMillis());
                        } catch (InterruptedException e) {
                            //none
                        }
                        break;
                    }
                    default: {
                        var ec = order.getErrorCode();
                        var rProblem = Problem.FAIL;
                        if (ec != null) {
                            var rCode = ResultCode.fromCode(ec);
                            rProblem = LifecellUtils.toProblem(rCode);
                            if (!rProblem.isFatal()) {
                                rProblem = Problem.FAIL;
                            }
                        }
                        throw new FsmException(rProblem, "Fail creating order with id {}", oId);
                    }
                }
            } else {
                throw new FsmException(Problem.FAIL, "There is no order with id {}", oId);
            }
        }
        log.info("[{}] Retrying getting order required", context.getFlow().getId());
        return ResponseNotify.builder()
                .problem(Problem.RETRY)
                .build();
    }

    private String createCustomerDocumentData(Context context) {
        var pc = diiaService.userGetPcDocument(context.getFlow().getId().toString(), context.getSession().getUserRef());
        var xd = diiaService.userGetLatestIdDocument(context.getFlow().getId().toString(), context.getSession().getUserRef());
        if (xd == null) {
            throw new FsmException(Problem.FAIL, "Can't get latest identification document");
        }
        var data = Stream.of(xd, pc)
                .filter((it) -> it != null)
                .collect(Collectors.toMap((it) -> it.getType().getRef(), (it) -> it));
        var data64 = "";
        try {
            data64 = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(data));
        } catch (Exception e) {
            throw new FsmException(Problem.FAIL, "Can't serialize customer documents", e);
        }
        return data64;
    }

    private EOrder createOrderTask(String transactionId, String userRef, String msisdn, String tariffRef, String tariffName, String token, String iccid, String puk1, String data) {
        var entity = ordersService.create();
        entity.setUserRef(userRef);
        entity.setRef(null);
        entity.setCreatedOn(null);
        entity.setDueOn(null);
        entity.setMsisdn(msisdn);
        entity.setStateId(State.UNKNOWN.getId());
        entity.setStageRef(OrderState.UNKNOWN.getRef());
        entity.setCancelable(false);
        entity.setUpdatable(false);
        entity.setTariffRef(tariffRef);
        entity.setTariffName(tariffName);
        ordersService.save(entity);

        var orderId = entity.getId();

        executorService.submit(() -> {
            try {
                var oid = lifecellService.createOrder(transactionId, userRef, msisdn, tariffRef, token, iccid, puk1, data);
                var order = ordersService.getById(orderId);
                order.setRef(oid);
                order.setStateId(State.PROGRESS.getId());
                ordersService.save(order);
            } catch (LifecellException e) {
                log.error("[{}] Error creating order with id {}", transactionId, orderId, e);
                var order = ordersService.getById(orderId);
                order.setStateId(State.FAIL.getId());
                order.setStageRef(OrderState.UNKNOWN.getRef());
                order.setErrorCode(e.getResultCode().getCode());
                order.setErrorRef(e.getResultCode().name());
                ordersService.save(order);
            } catch (Exception e) {
                log.error("[{}] Error creating order with id {}", transactionId, orderId, e);
                var order = ordersService.getById(orderId);
                order.setStateId(State.FAIL.getId());
                order.setStageRef(OrderState.UNKNOWN.getRef());
                order.setErrorCode(ResultCode.UNKNOWN_ERROR.getCode());
                order.setErrorRef(ResultCode.UNKNOWN_ERROR.name());
                ordersService.save(order);
            }
        });

        return entity;
    }
}
