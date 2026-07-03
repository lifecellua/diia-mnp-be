package com.lifecell.diia.be.components;

import com.google.protobuf.Empty;
import com.lifecell.diia.be.model.dto.ApplicationStatus;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseData;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.service.FsmGrpcService;
import com.lifecell.diia.be.service.FsmService;
import com.lifecell.diia.be.service.SupportService;
import com.lifecell.diia.be.service.ValidateService;
import com.lifecell.diia.be.service.stages.AgreementStage;
import com.lifecell.diia.be.service.stages.OrderCancelIntroStage;
import com.lifecell.diia.be.service.stages.OrderCancelOtpStage;
import com.lifecell.diia.be.service.stages.OrderSimOtpStage;
import com.lifecell.diia.be.service.stages.OrderSimStage;
import com.lifecell.diia.be.service.stages.OrderStage;
import com.lifecell.diia.be.service.stages.OrdersStage;
import com.lifecell.diia.be.service.stages.OtpStage;
import com.lifecell.diia.be.service.stages.PhoneStage;
import com.lifecell.diia.be.service.stages.SimIntroStage;
import com.lifecell.diia.be.service.stages.SimStage;
import com.lifecell.diia.be.service.stages.TariffsStage;
import com.lifecell.diia.grpc.model.ApplicationFilePostRequest;
import com.lifecell.diia.grpc.model.ApplicationStatusPostRequest;
import com.lifecell.diia.grpc.model.DataAgreementFileGetResponse;
import com.lifecell.diia.grpc.model.FormAnyGetResponse;
import com.lifecell.diia.grpc.model.FormAnyPostResponse;
import com.lifecell.diia.grpc.model.FormOrderGetRequest;
import com.lifecell.diia.grpc.model.FormOtpPostRequest;
import com.lifecell.diia.grpc.model.FormPhonePostRequest;
import com.lifecell.diia.grpc.model.FormSimPostRequest;
import com.lifecell.diia.grpc.model.FormTariffsPostRequest;
import com.lifecell.diia.grpc.service.MnpGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
public class GrpcServer extends MnpGrpc.MnpImplBase {
    private final FsmService fsmService;
    private final FsmGrpcService fsmGrpcService;
    private final SupportService supportService;
    private final ValidateService validateService;

    public GrpcServer(
            FsmService fsmService,
            FsmGrpcService fsmGrpcService,
            SupportService supportService,
            ValidateService validateService) {
        this.fsmService = fsmService;
        this.fsmGrpcService = fsmGrpcService;
        this.supportService = supportService;
        this.validateService = validateService;
    }

    // Branch 0 - start

    @Override
    public void formStartGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.createContext(Stage.STAGE_START);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_START, () -> RequestEmpty.getInstance());
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_ORDERS);
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_INTRO);
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_ERROR_NO_TAX_DOCUMENT);
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_ERROR_NO_ID_DOCUMENT);
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_ERROR_NO_SERVICE);
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_FAIL);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    // Branch 0 - intro from orders

    @Override
    public void formIntroGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDERS, () -> OrdersStage.RequestData.builder().command(OrdersStage.RequestData.Command.CREATE).build());
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_INTRO);
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_ERROR_NO_TAX_DOCUMENT);
            response = fsmGrpcService.fetchIf(response, ctx, Stage.STAGE_ERROR_NO_ID_DOCUMENT);
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_FAIL);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    // Branch 1 - tariffs

    @Override
    public void formTariffsGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_INTRO, () -> RequestEmpty.getInstance());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_TARIFFS);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formTariffsPost(FormTariffsPostRequest request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_TARIFFS, () -> TariffsStage.RequestData.builder().tariffId(request.getTariffId()).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    // Branch 1 - phone

    @Override
    public void formPhoneGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_PHONE);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formPhonePost(FormPhonePostRequest request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_PHONE, () -> PhoneStage.RequestData.builder().country(request.getCountry()).phone(request.getPhone()).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    // Branch 1 - otp

    @Override
    public void formOtpGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_OTP);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formOtpCheckPost(FormOtpPostRequest request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_OTP, () -> OtpStage.RequestData.builder().command(OtpStage.RequestData.Command.CHECK).otp(request.getOtp()).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    @Override
    public void formOtpSendPost(Empty request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_OTP, () -> OtpStage.RequestData.builder().command(OtpStage.RequestData.Command.SEND).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    // Branch 1 - contract

    @Override
    public void formAgreementGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_AGREEMENT);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void dataAgreementFileGet(Empty request, StreamObserver<DataAgreementFileGetResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_AGREEMENT, () -> AgreementStage.RequestData.builder().command(AgreementStage.RequestData.Command.DOCUMENT).build());
            if (response instanceof ResponseData<?> data) {
                responseObserver.onNext(DataAgreementFileGetResponse.newBuilder()
                        .setAgreement(data.getData().toString())
                        .build());
                responseObserver.onCompleted();
            } else {
                fsmGrpcService.handleResponse(response, DataAgreementFileGetResponse.class, responseObserver);
            }
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), DataAgreementFileGetResponse.class, responseObserver);
        }
    }

    // Branch 1 - sim-intro

    @Override
    public void formSimIntroGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_AGREEMENT, () -> AgreementStage.RequestData.builder().command(AgreementStage.RequestData.Command.CONTINUE).build());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_SIM_INTRO);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    // Branch 1 - sim

    @Override
    public void formSimGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_SIM_INTRO, () -> SimIntroStage.RequestData.builder().command(SimIntroStage.RequestData.Command.SIM).build());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_SIM);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formSimPost(FormSimPostRequest request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_SIM, () -> SimStage.RequestData.builder().command(SimStage.RequestData.Command.COMMIT).iccid(request.getIccid()).puk1(request.getPuk1()).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    // Branch 1 - success

    @Override
    public void formCommitGet(Empty request, StreamObserver<FormAnyGetResponse> responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_SIM_INTRO, () -> SimIntroStage.RequestData.builder().command(SimIntroStage.RequestData.Command.SKIP).build());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_SIM, () -> SimStage.RequestData.builder().command(SimStage.RequestData.Command.SKIP).build());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_COMMIT, () -> RequestEmpty.getInstance());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_SUCCESS);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    // Branch 2 - order

    @Override
    public void formOrderByIdGet(FormOrderGetRequest request, StreamObserver<FormAnyGetResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(Stage.STAGE_ORDERS);
            if (ctx == null) {
                ctx = fsmService.createContext(Stage.STAGE_ORDERS);
            }
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDERS, () -> OrdersStage.RequestData.builder().command(OrdersStage.RequestData.Command.ORDER).orderId(request.getOrderId()).build());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_ORDER);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderGet(Empty request, StreamObserver<FormAnyGetResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_SIM_OTP, () -> OrderSimOtpStage.RequestData.builder().command(OrderSimOtpStage.RequestData.Command.SKIP).build());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_SIM, () -> OrderSimStage.RequestData.builder().command(OrderSimStage.RequestData.Command.SKIP).build());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_SIM_SUCCESS, () -> RequestEmpty.getInstance());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_CANCEL_INTRO, () -> OrderCancelIntroStage.RequestData.builder().command(OrderCancelIntroStage.RequestData.Command.SKIP).build());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_CANCEL_OTP, () -> OrderCancelOtpStage.RequestData.builder().command(OrderCancelOtpStage.RequestData.Command.SKIP).build());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_CANCEL, () -> RequestEmpty.getInstance());
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_CANCEL_SUCCESS, () -> RequestEmpty.getInstance());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_ORDER);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderSimOtpGet(Empty request, StreamObserver<FormAnyGetResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER, () -> OrderStage.RequestData.builder().command(OrderStage.RequestData.Command.SIM).build());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_ORDER_SIM_OTP);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderSimOtpCheckPost(FormOtpPostRequest request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_ORDER_SIM_OTP, () -> OrderSimOtpStage.RequestData.builder().command(OrderSimOtpStage.RequestData.Command.CHECK).otp(request.getOtp()).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderSimOtpSendPost(Empty request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_ORDER_SIM_OTP, () -> OrderSimOtpStage.RequestData.builder().command(OrderSimOtpStage.RequestData.Command.SEND).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderSimGet(Empty request, StreamObserver<FormAnyGetResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_ORDER_SIM);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderSimPost(FormSimPostRequest request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_ORDER_SIM, () -> OrderSimStage.RequestData.builder().command(OrderSimStage.RequestData.Command.COMMIT).iccid(request.getIccid()).puk1(request.getPuk1()).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderCancelGet(Empty request, StreamObserver<FormAnyGetResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER, () -> OrderStage.RequestData.builder().command(OrderStage.RequestData.Command.CANCEL).build());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_ORDER_CANCEL_INTRO);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderCancelOtpGet(Empty request, StreamObserver<FormAnyGetResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walkIf(response, ctx, Stage.STAGE_ORDER_CANCEL_INTRO, () -> OrderCancelIntroStage.RequestData.builder().command(OrderCancelIntroStage.RequestData.Command.COMMIT).build());
            response = fsmGrpcService.fetch(response, ctx, Stage.STAGE_ORDER_CANCEL_OTP);
            fsmGrpcService.handleResponse(response, FormAnyGetResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyGetResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderCancelOtpCheckPost(FormOtpPostRequest request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_ORDER_CANCEL_OTP, () -> OrderCancelOtpStage.RequestData.builder().command(OrderCancelOtpStage.RequestData.Command.CHECK).otp(request.getOtp()).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    @Override
    public void formOrderCancelOtpSendPost(Empty request, StreamObserver<FormAnyPostResponse>  responseObserver) {
        var ctx = (Context) null;
        try {
            ctx = fsmService.restoreContext(null);
            var response = (Response) ResponseAwait.builder().target("#").build();
            response = fsmGrpcService.walk(response, ctx, Stage.STAGE_ORDER_CANCEL_OTP, () -> OrderCancelOtpStage.RequestData.builder().command(OrderCancelOtpStage.RequestData.Command.SEND).build());
            fsmGrpcService.handleResponse(response, FormAnyPostResponse.class, responseObserver);
        } catch (Exception e) {
            fsmGrpcService.handleResponse(fsmService.processException(ctx, e), FormAnyPostResponse.class, responseObserver);
        }
    }

    // Callbacks

    @Override
    public void applicationFile(ApplicationFilePostRequest request, StreamObserver<Empty>  responseObserver) {
        try {
            supportService.callbackFile(request.getApplicationId(), request.getDocumentId(), request.getDocumentData(), request.getDocumentName());
        } catch (Exception e) {
            log.error("Error processing callback: document", e);
        }
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void applicationStatus(ApplicationStatusPostRequest request, StreamObserver<Empty>  responseObserver) {
        try {
            var applicationStatus = new ApplicationStatus(request);
            validateService.validate(applicationStatus);
            validateService.validate(applicationStatus.getApplicationData());
            supportService.callbackOrder(applicationStatus);
        } catch (IllegalArgumentException e) {
            log.error("GrpcServer.ApplicationStatus() invalid arguments - {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid argument.")
                    .asRuntimeException());
            return;
        }
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
