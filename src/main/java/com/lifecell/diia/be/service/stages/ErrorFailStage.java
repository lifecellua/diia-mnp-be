package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.TemplateService;
import org.springframework.stereotype.Service;

@Service
public class ErrorFailStage implements StageHandler {
    private static final String TARGET_NAME = "error";

    private final TemplateService templateService;

    public ErrorFailStage(
            TemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_FAIL;
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
                .problem(Problem.fromCode(context.getFlow().getErrorCode()))
                .build();
    }
}
