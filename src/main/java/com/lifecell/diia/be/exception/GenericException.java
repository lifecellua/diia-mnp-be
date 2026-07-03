package com.lifecell.diia.be.exception;

import org.slf4j.helpers.MessageFormatter;

public class GenericException extends RuntimeException {

    public GenericException() {
        super();
    }

    public GenericException(String message) {
        super(message);
    }

    public GenericException(String message, Throwable cause) {
        super(message, cause);
    }

    public GenericException(Throwable cause) {
        super(cause);
    }

    public GenericException(String messagePattern, Object ...params) {
        super(MessageFormatter.arrayFormat(messagePattern, params).getMessage(), MessageFormatter.getThrowableCandidate(params));
    }
}
