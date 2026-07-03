package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.TemplateService;
import org.springframework.stereotype.Service;

@Service
public class ErrorNoTaxDocumentStage  implements StageHandler {
    private static final String TARGET_NAME = "error_no_tax_document";
    private static final String TEMPLATE_NAME = "no_tax_number";

    private final TemplateService templateService;

    public ErrorNoTaxDocumentStage(
            TemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ERROR_NO_TAX_DOCUMENT;
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
