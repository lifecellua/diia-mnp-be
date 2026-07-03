package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
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

import java.util.Optional;
import java.util.regex.Pattern;

import static com.lifecell.diia.be.util.LifecellUtils.toProblem;

@Slf4j
@Service
public class PhoneStage implements StageHandler {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {
        private String country;
        private String phone;
    }

    private static final String TARGET_NAME = "phone";
    private static final String TEMPLATE_NAME = "phone";
    private static final Pattern PATTERN_PHONE = Pattern.compile("(?<p0>\\+)?(?<p1>380)?(?<p2>\\d{2})(?<p3>\\d{7})");

    private final OrdersService ordersService;
    private final TemplateService templateService;
    private final LifecellService lifecellService;

    public PhoneStage(
            OrdersService ordersService,
            TemplateService templateService, LifecellService lifecellService) {
        this.ordersService = ordersService;
        this.templateService = templateService;
        this.lifecellService = lifecellService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_PHONE;
    }

    @Override
    public Response enter(Context context) {
        return ResponseAwait.builder()
                .target(TARGET_NAME)
                .build();
    }

    @Override
    public Response form(Context context) {
        var country = "ukr";
        var phone = context.getSession().getUserPhone();
        var matcher = PATTERN_PHONE.matcher(context.getSession().getUserPhone());
        if (matcher.matches()) {
            var p2 = matcher.group("p2");
            var p3 = matcher.group("p3");
            phone = p2 + p3;
        } else {
            phone = "";
        }
        return ResponseForm.builder()
                .form(templateService.generate(TEMPLATE_NAME,
                        TemplateService.mapOf(
                                "COUNTRY", country,
                                "PHONE", phone)))
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        var data = FsmUtils.toRequest(request, RequestData.class);

        var country = Optional.ofNullable(data.getCountry()).orElse("");
        var phone = Optional.ofNullable(data.getPhone()).orElse("");

        phone = country + phone;
        var matcher = PATTERN_PHONE.matcher(phone);
        if (matcher.matches()) {
            var p1 = matcher.group("p1");
            if (p1 == null) {
                p1 = "380";
            }
            var p2 = matcher.group("p2");
            var p3 = matcher.group("p3");
            phone =  p1 + p2 + p3;
            var rCode = lifecellService.checkPossibility(context.getFlow().getId().toString(), context.getSession().getUserRef(), phone);
            var rProblem = toProblem(rCode);
            if (rProblem == Problem.SUCCESS) {
                context.getFlow().setMsisdn(phone);
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_OTP)
                        .build();
            } else {
                if (rProblem.isFatal()) {
                    throw new FsmException(rProblem, "Fail checking phone");
                }
                return ResponseNotify.builder()
                        .problem(rProblem)
                        .build();
            }
        } else {
            return ResponseNotify.builder()
                    .problem(Problem.INVALID_PHONE)
                    .build();
        }
    }
}
