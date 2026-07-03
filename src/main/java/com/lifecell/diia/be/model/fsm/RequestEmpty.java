package com.lifecell.diia.be.model.fsm;

public class RequestEmpty extends Request {
    private static RequestEmpty INSTANCE = new RequestEmpty();

    private RequestEmpty() {
    }

    public static RequestEmpty getInstance() {
        return INSTANCE;
    }
}
