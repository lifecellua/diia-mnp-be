package com.lifecell.diia.be.exception;

import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import org.slf4j.helpers.MessageFormatter;

public class LifecellException extends GenericException {
    private ResultCode resultCode;

    public LifecellException(ResultCode resultCode) {
        super();
        this.resultCode = resultCode;
    }

    public LifecellException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public LifecellException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public LifecellException(ResultCode resultCode, Throwable cause) {
        super(cause);
        this.resultCode = resultCode;
    }

    public LifecellException(ResultCode resultCode, String messagePattern, Object ...params) {
        super(MessageFormatter.arrayFormat(messagePattern, params).getMessage(), MessageFormatter.getThrowableCandidate(params));
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
