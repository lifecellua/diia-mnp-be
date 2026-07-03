package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ErrorNoServiceStage implements StageHandler {
    private static final String TARGET_NAME = "error_no_service";
    private static final String TEMPLATE_NAME = "no_service";

    private final TemplateService templateService;

    public ErrorNoServiceStage(
            TemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ERROR_NO_SERVICE;
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
                        TemplateService.mapOf("HELLO", "Вітаємо, " + context.getSession().getUserName() + "!")
                    ))
                .build();
    }
}
