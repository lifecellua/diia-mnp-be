package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.OrdersService;
import com.lifecell.diia.be.service.SupportService;
import com.lifecell.diia.be.service.TemplateService;
import com.lifecell.diia.be.util.FsmUtils;
import com.lifecell.diia.be.util.LifecellUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class OrderStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {

        public static enum Command {
            SIM,
            CANCEL;
        }

        private Command command;
    }

    private static final String TARGET_NAME = "order";
    private static final String TEMPLATE_NAME = "order";

    private final DiiaService diiaService;
    private final LifecellService lifecellService;
    private final OrdersService ordersService;
    private final SupportService supportService;
    private final TemplateService templateService;

    private final ZoneId LOCAL_ZONE_ID;

    public OrderStage(
            DiiaService diiaService,
            LifecellService lifecellService,
            OrdersService ordersService,
            Properties properties,
            SupportService supportService,
            TemplateService templateService) {
        this.diiaService = diiaService;
        this.lifecellService = lifecellService;
        this.ordersService = ordersService;
        this.supportService = supportService;
        this.templateService = templateService;

        this.LOCAL_ZONE_ID = properties.getLocal().getZoneId();
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ORDER;
    }

    @Override
    public Response enter(Context context) {
        supportService.refreshOrderByRef(context.getFlow().getId().toString(), context.getOrder().getRef());
        var order = ordersService.getById(context.getFlow().getOrder().getId());
        context.getFlow().setTariffRef(order.getTariffRef());
        context.getFlow().setTariffName(order.getTariffName());
        context.getFlow().setMsisdn(order.getMsisdn());
        context.getFlow().setToken(null);
        context.getFlow().setIccid(null);
        context.setOrder(order);
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response form(Context context) {
        var orderCreatedOn = Optional.ofNullable(context.getOrder().getCreatedOn())
                .map((odt) -> odt.atZoneSameInstant(LOCAL_ZONE_ID))
                .map(ZonedDateTime::toOffsetDateTime)
                .orElse(null);
        var orderDueOn = Optional.ofNullable(context.getOrder().getDueOn())
                .map((odt) -> odt.atZoneSameInstant(LOCAL_ZONE_ID))
                .map(ZonedDateTime::toOffsetDateTime)
                .orElse(null);
        return ResponseForm.builder()
                .form(templateService.generate(TEMPLATE_NAME,
                        TemplateService.mapOf(
                                "STATUS", TemplateService.mapOf(
                                        "ICON", OrderState.fromRef(context.getOrder().getStageRef()).getInfo().getIcon(),
                                        "TITLE", OrderState.fromRef(context.getOrder().getStageRef()).getInfo().getTitle(),
                                        "TEXT", OrderState.fromRef(context.getOrder().getStageRef()).getInfo().getText()

                                    ),
                                "INFO", TemplateService.mapOf(
                                        "DATE_AT", (orderCreatedOn != null)
                                                ? orderCreatedOn.toLocalDate().format(LifecellUtils.PRETTY_DATE)
                                                : "?",
                                        "NO", context.getOrder().getRef(),
                                        "DATE_WHEN", (OrderState.fromRef(context.getOrder().getStageRef()).getInfo().getDue() && (orderDueOn != null))
                                                ? TemplateService.mapOf("VALUE", toDueDateTime(orderDueOn))
                                                : TemplateService.Operation.REMOVE,
                                        "PHONE", context.getOrder().getMsisdn(),
                                        "TARIFF", context.getOrder().getTariffName()
                                    ),
                                "EXTRA", ((context.getOrder().getCancelable() == false) && (context.getOrder().getUpdatable() == false))
                                        ? TemplateService.Operation.REMOVE
                                        : TemplateService.mapOf(
                                                "SIM", (context.getOrder().getUpdatable() == false)
                                                        ? TemplateService.Operation.REMOVE
                                                        : TemplateService.Operation.KEEP,
                                                "CANCEL", (context.getOrder().getCancelable() == false)
                                                        ? TemplateService.Operation.REMOVE
                                                        : TemplateService.Operation.KEEP
                                            )

                            )
                    ))
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        switch (data.getCommand()) {
            case SIM: {
                if (context.getOrder().getUpdatable() == false) {
                    return ResponseNotify.builder()
                            .problem(Problem.FAIL)
                            .build();
                }
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_ORDER_SIM_OTP)
                        .build();
                //break;
            }
            case CANCEL: {
                if (context.getOrder().getCancelable() == false) {
                    return ResponseNotify.builder()
                            .problem(Problem.FAIL)
                            .build();
                }
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_ORDER_CANCEL_INTRO)
                        .build();
                //break;
            }
            default: {
                throw new FsmException(Problem.FAIL, "Unknown command");
            }
        }
    }

    private String toDueDateTime(OffsetDateTime dt) {
        var dx = dt.toLocalDateTime();
        var d1 = dx.minusHours(2);
        var d2 = dx.plusHours(1);
        var result = d1.toLocalDate().format(LifecellUtils.PRETTY_DATE)
                + " " + d1.toLocalTime().format(LifecellUtils.PRETTY_TIME)
                + " - " + d2.toLocalTime().format(LifecellUtils.PRETTY_TIME);
        return result;
    }
}
