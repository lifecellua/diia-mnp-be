package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.model.fsm.State;
import com.lifecell.diia.be.service.OrdersService;
import com.lifecell.diia.be.service.TemplateService;
import com.lifecell.diia.be.util.FsmUtils;
import com.lifecell.diia.be.util.LifecellUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrdersStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {

        public static enum Command {
            ORDER,
            CREATE;
        }

        private Command command;
        private String orderId;
    }

    private static final String TARGET_NAME = "orders";
    private static final String TEMPLATE_NAME = "orders";

    private final OrdersService ordersService;
    private final TemplateService templateService;

    public OrdersStage(
            OrdersService ordersService,
            TemplateService templateService) {
        this.ordersService = ordersService;
        this.templateService = templateService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ORDERS;
    }

    @Override
    public Response enter(Context context) {
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response form(Context context) {
        var orders = ordersService.getLatestByUserRef(context.getSession().getUserRef());
        return ResponseForm.builder()
                .form(templateService.generate(TEMPLATE_NAME,
                        TemplateService.mapOf(
                                "ORDERS", orders.stream()
                                        .map((order) -> TemplateService.mapOf(
                                                "CODE", OrderState.fromRef(order.getStageRef()).getRef(),
                                                "NAME", OrderState.fromRef(order.getStageRef()).getInfo().getTitle(),
                                                "TYPE", OrderState.fromRef(order.getStageRef()).getInfo().getStatus(),
                                                "LABEL", order.getMsisdn(),
                                                "DESCRIPTION", TemplateService.listOf("Заява від " + order.getCreatedAt().toLocalDate().format(LifecellUtils.PRETTY_DATE)),
                                                "RESOURCE", order.getId()
                                        ))
                                .toList())))
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        switch (data.getCommand()) {
            case ORDER: {
                var orderId = UUID.fromString(data.getOrderId());
                var order = ordersService.getById(orderId);
                if (order.getUserRef().equals(context.getSession().getUserRef())) {
                    context.getFlow().setTariffRef(order.getTariffRef());
                    context.getFlow().setTariffName(order.getTariffName());
                    context.getFlow().setMsisdn(order.getMsisdn());
                    context.getFlow().setToken(null);
                    context.getFlow().setIccid(null);
                    context.getFlow().setDocument(null);
                    context.getFlow().setOrder(AggregateReference.to(orderId));
                    context.setOrder(order);
                    return ResponseTransit.builder()
                            .stage(Stage.STAGE_ORDER)
                            .build();
                } else {
                    throw new FsmException(Problem.FAIL_INVALID_ORDER, "Order not found");
                }
                //break;
            }
            case CREATE: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_CHECK)
                        .build();
                //break;
            }
            default: {
                throw new FsmException(Problem.FAIL, "Unknown command");
            }
        }
    }
}
