package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderCancelIntroStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {

        public static enum Command {
            COMMIT,
            SKIP;
        }

        private Command command;
    }

    private static final String TARGET_NAME = "order-cancel-intro";
    private static final String TEMPLATE_NAME = "order-cancel-intro";

    public OrderCancelIntroStage() {
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ORDER_CANCEL_INTRO;
    }

    @Override
    public Response enter(Context context) {
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response form(Context context) {
        return ResponseNotify.builder()
                .problem(Problem.FORM_ORDER_CANCEL)
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        switch (data.getCommand()) {
            case COMMIT: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_ORDER_CANCEL_OTP)
                        .build();
                //break;
            }
            case SKIP: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_ORDER)
                        .build();
                //break;
            }
            default:
                throw new FsmException(Problem.FAIL, "Unknown command");
        }
    }
}
