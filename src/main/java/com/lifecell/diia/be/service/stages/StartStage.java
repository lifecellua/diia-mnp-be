package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseContinue;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.OrdersService;
import com.lifecell.diia.be.service.SupportService;
import com.lifecell.diia.be.util.FsmUtils;
import org.springframework.stereotype.Service;

@Service
public class StartStage implements StageHandler {
    private final DiiaService diiaService;
    private final LifecellService lifecellService;
    private final OrdersService ordersService;
    private final SupportService supportService;

    public StartStage(
            DiiaService diiaService,
            LifecellService lifecellService,
            OrdersService ordersService,
            SupportService supportService) {
        this.diiaService = diiaService;
        this.lifecellService = lifecellService;
        this.ordersService = ordersService;
        this.supportService = supportService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_START;
    }

    @Override
    public Response enter(Context context) {
        return ResponseContinue.builder()
                .request(RequestEmpty.getInstance())
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        FsmUtils.toRequest(request, RequestEmpty.class);

        var healthy = lifecellService.checkHealth();
        if (healthy) {
            supportService.refreshOrdersByUserRef(context.getFlow().getId().toString(), context.getSession().getUserRef());
            var orders = ordersService.getLatestByUserRef(context.getSession().getUserRef());
            if (orders.isEmpty()) {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_CHECK)
                        .build();
            } else {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_ORDERS)
                        .build();
            }
        }
        return ResponseTransit.builder()
                .stage(Stage.STAGE_ERROR_NO_SERVICE)
                .build();
    }
}
