package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.dto.lifecell.response.TariffInfo;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.OrdersService;
import com.lifecell.diia.be.service.TemplateService;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TariffsStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {
        private String tariffId;
    }

    private static final String TARGET_NAME = "tariffs";
    private static final String TEMPLATE_NAME = "tariffs";

    private final DiiaService diiaService;
    private final OrdersService ordersService;
    private final TemplateService templateService;
    private final LifecellService lifecellService;

    public TariffsStage(
            DiiaService diiaService,
            OrdersService ordersService,
            TemplateService templateService, LifecellService lifecellService) {
        this.diiaService = diiaService;
        this.ordersService = ordersService;
        this.templateService = templateService;
        this.lifecellService = lifecellService;
    }

    private List<TariffInfo> getTariffs(Context context) {
        var pc = diiaService.userGetPcDocument(context.getFlow().getId().toString(), context.getSession().getUserRef());
        var tariffs = lifecellService.getTariffs();
        return tariffs.stream()
                .filter((tariff) -> (tariff.getHasPc() == false) || (tariff.getHasPc() == (pc != null)))
                .toList();
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_TARIFFS;
    }

    @Override
    public Response form(Context context) {
        var tariffs = getTariffs(context);
        return ResponseForm.builder()
                .form(templateService.generate(TEMPLATE_NAME, TemplateService.mapOf(
                        "TARIFFS", tariffs.stream()
                                .map((tariff) ->
                                        TemplateService.mapOf(
                                                "INFO", TemplateService.mapOf(
                                                        "STATUS", tariff.getHasPc()
                                                                ? TemplateService.Operation.KEEP
                                                                : TemplateService.Operation.REMOVE,
                                                        "ID", tariff.getId(),
                                                        "NAME", TemplateService.mapOf(
                                                                "NAME", tariff.getName(),
                                                                "PRICE", tariff.getPrice()),
                                                        "DESCRIPTION", TemplateService.listOf(tariff.getDescription()),
                                                        "BENEFITS", tariff.getBenefits().stream()
                                                                .map((benefit) ->
                                                                        TemplateService.mapOf(
                                                                                "CODE", benefit,
                                                                                "NAME", benefit)
                                                                )
                                                                .toList(),
                                                        "ICON", tariff.getIcon()
                                                ),
                                                "REF", TemplateService.mapOf(
                                                        "DESCRIPTION", TemplateService.mapOf(
                                                                        "NAME", tariff.getName()),
                                                                "LABEL", TemplateService.mapOf(
                                                                "NAME", tariff.getName()),
                                                                "URL", tariff.getUrl()
                                                )
                                        )
                                )
                                .toList())))
                .build();
    }

    @Override
    public Response enter(Context context) {
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        var tariffId = data.getTariffId();
        var tariffs = getTariffs(context);
        var tariff = tariffs.stream()
                .filter((t) -> t.getId().equals(tariffId))
                .findAny()
                .orElse(null);
        if (tariff != null) {
            context.getFlow().setTariffRef(tariffId);
            context.getFlow().setTariffName(tariff.getName());
            return ResponseTransit.builder()
                    .stage(Stage.STAGE_PHONE)
                    .build();
        } else {
            throw new FsmException(Problem.FAIL_INVALID_TARIFF, "Tariff not found");
        }
    }
}
