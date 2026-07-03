package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseContinue;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderCancelStage implements StageHandler {
    private static final String TARGET_NAME = "order-cancel";
    private static final String TEMPLATE_NAME = "order-cancel";

    private final LifecellService lifecellService;

    public OrderCancelStage(
            LifecellService lifecellService) {
        this.lifecellService = lifecellService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ORDER_CANCEL;
    }

    @Override
    public Response enter(Context context) {
        return ResponseContinue.builder()
                .request(RequestEmpty.getInstance())
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        lifecellService.cancelOrder(context.getFlow().getId().toString(), context.getFlow().getMsisdn(), context.getOrder().getUserRef(), context.getOrder().getRef(), context.getFlow().getToken());
        return ResponseTransit.builder()
                .stage(Stage.STAGE_ORDER_CANCEL_SUCCESS)
                .build();
    }
}
