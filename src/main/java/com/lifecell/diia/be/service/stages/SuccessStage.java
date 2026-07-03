package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.util.FsmUtils;
import org.springframework.stereotype.Service;

@Service
public class SuccessStage implements StageHandler {
    private static final String TARGET_NAME = "success";
    private static final String TEMPLATE_NAME = "success";

    public SuccessStage() {
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_SUCCESS;
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
                .problem(Problem.SUCCESS)
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        FsmUtils.toRequest(request, RequestEmpty.class);

        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }
}
