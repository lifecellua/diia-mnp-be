package com.lifecell.diia.be.service;

import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.exception.GenericException;
import com.lifecell.diia.be.exception.LifecellException;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseContinue;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Session;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.model.fsm.State;
import com.lifecell.diia.be.util.DiiaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.gov.diia.grpc.SessionUtils;
import ua.gov.diia.grpc.interceptor.server.SessionServerInterceptor;
import ua.gov.diia.types.token.UserTokenDataMsg;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.lifecell.diia.be.util.LifecellUtils.toProblem;

@Slf4j
@Service
public class FsmService {
    private final FlowsService flowsService;
    private final OrdersService ordersService;
    private final Properties properties;
    private final Map<Stage, StageHandler> stageHandlers;

    public FsmService(
            FlowsService flowsService,
            OrdersService ordersService,
            Properties properties,
            SupportService supportService,
            Collection<StageHandler> stageHandlers) {
        this.flowsService = flowsService;
        this.ordersService = ordersService;
        this.properties = properties;
        this.stageHandlers = stageHandlers.stream()
                .collect(Collectors.toMap(StageHandler::getStage, stageHandler -> stageHandler));
    }

    public Context createContext(Stage stage) {
        var su = DiiaUtils.getSessionUser();
        var userRef = su.getIdentifier();

        var context = new Context(this::updateContext);
        context.setSession(Session.builder()
                .userRef(su.getIdentifier())
                .userPhone(su.getPhoneNumber())
                .userName(su.getFName())
                .build());

        var flow = flowsService.getActiveByUserRef(userRef);
        if (flow != null) {
            if (State.fromId(flow.getStateId()) == State.PROGRESS) {
                var eid = UUID.randomUUID().toString();
                log.warn("[{}] [{}] Flow is cancelled", eid, flow.getId());
                flow.setStateId(State.CANCEL.getId());
                flow.setStageRef(Stage.STAGE_FAIL.toRef());
                flow.setErrorCode(Problem.FAIL.getCode());
                flow.setErrorRef(eid);
                flowsService.save(flow);
            }
        }
        flow = flowsService.create();
        flow.setUserRef(userRef);
        flow.setStateId(State.PROGRESS.getId());
        flow.setStageRef(stage.toRef());
        flowsService.save(flow);

        context.setFlow(flow);

        return context;
    }

    public Context restoreContext(Stage stage) {
        var su = DiiaUtils.getSessionUser();
        var userRef = su.getIdentifier();

        var context = new Context(this::updateContext);
        context.setSession(Session.builder()
                .userRef(su.getIdentifier())
                .userPhone(su.getPhoneNumber())
                .userName(su.getFName())
                .build());

        var flow = flowsService.getActiveByUserRef(userRef);
        if (flow != null) {
            if (Duration.between(flow.getModifiedAt(), OffsetDateTime.now()).compareTo(properties.getFlow().getSession().getTimeout()) > 0) {
                var eid = UUID.randomUUID().toString();
                log.warn("[{}] [{}] Flow is expired", eid, flow.getId());
                flow.setStateId(State.FAIL.getId());
                flow.setStageRef(Stage.STAGE_FAIL.toRef());
                flow.setErrorCode(Problem.FAIL.getCode());
                flow.setErrorRef(eid);
                flowsService.save(flow);
                flow = null;
            }
        }
        if (flow != null) {
            if ((stage != null) && (stage != Stage.fromRef(flow.getStageRef()))) {
                return null;
            }
        }
        if (flow == null) {
            if (stage == null) {
                throw new FsmException(Problem.FAIL, "Can't restore context");
            }
            return null;
        }
        context.setFlow(flow);

        if (context.getFlow().getOrder() != null) {
            var order = ordersService.getById(context.getFlow().getOrder().getId());
            context.setOrder(order);
        }

        return context;
    }

    public boolean validateContext(boolean mandatory, Context context, Stage... stages) {
        if (context == null) {
            if (mandatory) {
                throw new FsmException(Problem.FAIL, "Can't validate context");
            } else {
                return false;
            }
        }
        var same = Arrays.stream(stages).anyMatch((it) -> it == Stage.fromRef(context.getFlow().getStageRef()));
        if (!same && mandatory) {
            throw new FsmException(Problem.FAIL, "Current stage: {}, Request from stage {}",
                    Stage.fromRef(context.getFlow().getStageRef()),
                    Arrays.stream(stages).map(Stage::name).collect(Collectors.joining(",")));
        }
        return same;
    }

    private void updateContext(Context context) {
        if (context.getFlow() != null) {
            flowsService.save(context.getFlow());
        }
    }

    public Response form(Context context) {
        log.info("[{}] FORM {}", context.getFlow().getId(), Stage.fromRef(context.getFlow().getStageRef()));
        var response = stageHandlers.get(Stage.fromRef(context.getFlow().getStageRef())).form(context);
        return response;
    }

    public Response process(Context context, Request request) {
        var stage = Stage.fromRef(context.getFlow().getStageRef());
        log.info("[{}] LEAVE {}", context.getFlow().getId(), stage);
        var response = stageHandlers.get(stage).leave(context, request);
        context.update();
        while (response instanceof ResponseTransit rt) {
            context.getFlow().setStageRef(rt.getStage().toRef());
            context.getFlow().setStateId(rt.getStage().getState().getId());
            log.info("[{}] ENTER {}", context.getFlow().getId(), rt.getStage());
            response = stageHandlers.get(rt.getStage()).enter(context);
            context.update();
            if (response instanceof ResponseContinue rc) {
                log.info("[{}] LEAVE {}", context.getFlow().getId(), rt.getStage());
                response = stageHandlers.get(rt.getStage()).leave(context, rc.getRequest());
                context.update();
            }
        }
        return response;
    }

    public Response processException(Context context, Exception exception) {
        var eid = UUID.randomUUID().toString();
        log.error("[{}] [{}] Error processing flow at {}",
                eid,
                (context != null) ? context.getFlow().getId() : "?",
                (context != null) ? Stage.fromRef(context.getFlow().getStageRef()) : "?",
                exception);

        var problem = Problem.FAIL;
        if (exception instanceof FsmException fe) {
            problem = fe.getProblem();
        }
        if (exception instanceof LifecellException le) {
            problem = toProblem(le.getResultCode());
            if (!problem.isFatal()) {
                problem = Problem.FAIL;
            }
        }

        if (context != null) {
        context.getFlow().setStageRef(Stage.STAGE_FAIL.toRef());
        context.getFlow().setStateId(Stage.STAGE_FAIL.getState().getId());
        context.getFlow().setErrorCode(problem.getCode());
        context.getFlow().setErrorRef(eid);
        updateContext(context);
        }

        var response = ResponseNotify.builder()
                .problem(problem)
                .build();
        return response;
    }
}
