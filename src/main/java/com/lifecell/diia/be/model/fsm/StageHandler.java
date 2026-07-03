package com.lifecell.diia.be.model.fsm;

import com.lifecell.diia.be.exception.GenericException;

public interface StageHandler {
    public Stage getStage();

    public default Response enter(Context context) {
        throw new GenericException("Not supported");
    }

    public default Response form(Context context) {
        throw new GenericException("Not supported");
    }

    public default Response leave(Context context, Request request) {
        throw new GenericException("Not supported");
    }
}
