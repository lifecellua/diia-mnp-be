package com.lifecell.diia.be.service;

import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.util.ProtoUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
public class FsmGrpcService {
    private final FsmService fsmService;
    private final JsonService jsonService;

    public FsmGrpcService(
            FsmService fsmService,
            JsonService jsonService) {
        this.fsmService = fsmService;
        this.jsonService = jsonService;
    }

    public Response fetch(Response prev, Context ctx, Stage stage) {
        var rsp = prev;
        if (rsp instanceof ResponseAwait) {
            log.info("GET {}", stage);
            fsmService.validateContext(true, ctx, stage);
            try {
                rsp = fsmService.form(ctx);
            } catch (Exception e) {
                rsp = fsmService.processException(ctx, e);
            }
        }
        return rsp;
    }

    public Response fetchIf(Response prev, Context ctx, Stage stage) {
        var rsp = prev;
        if (rsp instanceof ResponseAwait) {
            log.info("GET-IF {}", stage);
            var same = fsmService.validateContext(false, ctx, stage);
            if (same) {
                try {
                    rsp = fsmService.form(ctx);
                } catch (Exception e) {
                    rsp = fsmService.processException(ctx, e);
                }
            }
        }
        return rsp;
    }

    public Response walk(Response prev, Context ctx, Stage stage, Supplier<Request> requestSupplier) {
        var rsp = prev;
        if (rsp instanceof ResponseAwait) {
            log.info("POST {}", stage);
            fsmService.validateContext(true, ctx, stage);
            try {
                var req = requestSupplier.get();
                rsp = fsmService.process(ctx, req);
            } catch (Exception e) {
                rsp = fsmService.processException(ctx, e);
            }
        }
        return rsp;
    }

    public Response walkIf(Response prev, Context ctx, Stage stage, Supplier<Request> requestSupplier) {
        var rsp = prev;
        if (rsp instanceof ResponseAwait) {
            log.info("POST-IF {}", stage);
            var same = fsmService.validateContext(false, ctx, stage);
            if (same) {
                try {
                    var req = requestSupplier.get();
                    rsp = fsmService.process(ctx, req);
                } catch (Exception e) {
                    rsp = fsmService.processException(ctx, e);
                }
            }
        }
        return rsp;
    }

    public <T> void handleResponse(Response response,  Class<T> clazz, StreamObserver<T> responseObserver) {
        var rsp = ProtoUtils.convert(jsonService, response, clazz);
        responseObserver.onNext(rsp);
        responseObserver.onCompleted();
    }
}
