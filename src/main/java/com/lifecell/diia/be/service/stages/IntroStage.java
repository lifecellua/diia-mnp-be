package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.service.OrdersService;
import com.lifecell.diia.be.service.TemplateService;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IntroStage implements StageHandler {
    private static final String TARGET_NAME = "intro";
    private static final String TEMPLATE_NAME = "intro";

    private final DiiaService diiaService;
    private final OrdersService ordersService;
    private final TemplateService templateService;

    public IntroStage(
            DiiaService diiaService,
            OrdersService ordersService,
            TemplateService templateService) {
        this.diiaService = diiaService;
        this.ordersService = ordersService;
        this.templateService = templateService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_INTRO;
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
                .form(templateService.generate(TEMPLATE_NAME, null))
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        FsmUtils.toRequest(request, RequestEmpty.class);

        return ResponseTransit.builder()
                .stage(Stage.STAGE_TARIFFS)
                .build();
    }
}
