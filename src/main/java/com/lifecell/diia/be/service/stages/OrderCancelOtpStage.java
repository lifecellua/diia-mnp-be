package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.Properties;
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
import com.lifecell.diia.be.service.TemplateService;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.lifecell.diia.be.util.LifecellUtils.toProblem;

@Slf4j
@Service
public class OrderCancelOtpStage implements StageHandler {

    private final Properties properties;

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RequestData extends Request {

        public static enum Command {
            CHECK,
            SEND,
            SKIP
        }

        private Command command;
        private String otp;
    }

    private static final String TARGET_NAME = "order-cancel-otp";
    private static final String TEMPLATE_NAME = "otp";

    private final LifecellService lifecellService;
    private final TemplateService templateService;

    public OrderCancelOtpStage(
            LifecellService lifecellService,
            TemplateService templateService, Properties properties) {
        this.lifecellService = lifecellService;
        this.templateService = templateService;
        this.properties = properties;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_ORDER_CANCEL_OTP;
    }

    @Override
    public Response enter(Context context) {
        sendOtp(context);
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
        var data = FsmUtils.toRequest(request, RequestData.class);

        switch (data.getCommand()) {
            case CHECK: {
                var otp = data.getOtp();
                var tr = lifecellService.verifyOtpAndGetToken(context.getFlow().getId().toString(), context.getSession().getUserRef(), context.getFlow().getMsisdn(), otp);
                var rProblem = toProblem(tr.getResultCode());
                if (rProblem == Problem.SUCCESS) {
                    context.getFlow().setToken(tr.getToken());
                    return ResponseTransit.builder()
                            .stage(Stage.STAGE_ORDER_CANCEL)
                            .build();
                } else {
                    if (rProblem.isFatal()) {
                        throw new FsmException(rProblem, "Fail checking otp");
                    }
                    return ResponseNotify.builder()
                            .problem(rProblem)
                            .build();
                }
                //break;
            }
            case SEND: {
                sendOtp(context);
                return ResponseAwait.builder()
                        .target(TARGET_NAME)
                        .build();
                //break;
            }
            case SKIP: {
                return ResponseTransit.builder()
                        .stage(Stage.STAGE_ORDER)
                        .build();
                //break;
            }
            default: {
                throw new FsmException(Problem.FAIL, "Unknown command");
            }
        }
    }

    private void sendOtp(Context context) {
        lifecellService.sendOtp(context.getFlow().getId().toString(), context.getSession().getUserRef(), context.getFlow().getMsisdn());
    }
}
