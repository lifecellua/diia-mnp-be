package com.lifecell.diia.be.service.stages;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.fsm.Context;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import com.lifecell.diia.be.model.fsm.RequestEmpty;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseContinue;
import com.lifecell.diia.be.model.fsm.ResponseTransit;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.StageHandler;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.util.FsmUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CheckStage implements StageHandler {
    private final DiiaService diiaService;

    public CheckStage(
            DiiaService diiaService) {
        this.diiaService = diiaService;
    }

    @Override
    public Stage getStage() {
        return Stage.STAGE_CHECK;
    }

    @Override
    public Response enter(Context context) {
        context.getFlow().setTariffRef(null);
        context.getFlow().setTariffName(null);
        context.getFlow().setMsisdn(null);
        context.getFlow().setToken(null);
        context.getFlow().setIccid(null);
        context.getFlow().setDocument(null);
        context.getFlow().setOrder(null);
        context.setOrder(null);
        return ResponseContinue.builder()
                .request(RequestEmpty.getInstance())
                .build();
    }

    @Override
    public Response leave(Context context, Request request) {
        FsmUtils.toRequest(request, RequestEmpty.class);

        //var hasTaxDocument = diiaService.userHasTaxDocument(context.getSession().getUserRef());
        var td = diiaService.userGetTaxDocument( context.getFlow().getId().toString(), context.getSession().getUserRef());
        if (td == null) {
            return ResponseTransit.builder()
                    .stage(Stage.STAGE_ERROR_NO_TAX_DOCUMENT)
                    .build();
        }

        //var hasIdDocument = diiaService.userHasSomeIdDocument();
        var xd = diiaService.userGetLatestIdDocument(context.getFlow().getId().toString(), context.getSession().getUserRef());
        if (xd == null) {
            return ResponseTransit.builder()
                    .stage(Stage.STAGE_ERROR_NO_ID_DOCUMENT)
                    .build();
        }

        return ResponseTransit.builder()
                .stage(Stage.STAGE_INTRO)
                .build();
    }
}
