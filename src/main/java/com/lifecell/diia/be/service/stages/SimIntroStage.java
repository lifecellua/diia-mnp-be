package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class SimIntroStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {

        public static enum Command {
            SIM,
            SKIP;
        }

        private Command command;
    }

    public SimIntroStage() {
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_SIM_INTRO;
    }

    @Override
    public Response enter(Context context) {
        return ResponseNotify.builder()
                .problem(Problem.FORM_SIM_INTRO)
                .build();
    }

    @Override
    public Response form(Context context) {
        return ResponseNotify.builder()
                .problem(Problem.FORM_SIM_INTRO)
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        switch (data.getCommand()) {
            case SIM: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_SIM)
                        .build();
                //break;
            }
            case SKIP: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_COMMIT)
                        .build();
                //break;
            }
            default: {
                throw new FsmException(Problem.FAIL, "Unknown command");
            }
        }
    }
}
