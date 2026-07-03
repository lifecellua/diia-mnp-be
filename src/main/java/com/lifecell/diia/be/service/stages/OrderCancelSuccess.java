package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderCancelSuccess implements StageHandler {

    public OrderCancelSuccess() {
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ORDER_CANCEL_SUCCESS;
    }

    @Override
    public Response enter(Context context) {
        return ResponseNotify.builder()
                .problem(Problem.FORM_ORDER_CANCEL_SUCCESS)
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        FsmUtils.toRequest(request, RequestEmpty.class);

        return ResponseTransit.builder()
                .stage(Stage.STAGE_ORDER)
                .build();
    }
}
