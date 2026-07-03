package com.lifecell.diia.be.exception;

import com.lifecell.diia.be.model.fsm.Problem;
import org.slf4j.helpers.MessageFormatter;

public class FsmException extends GenericException {
    private Problem problem;

    public FsmException(Problem problem) {
        super();
        this.problem = problem;
    }

    public FsmException(Problem problem, String message) {
        super(message);
        this.problem = problem;
    }

    public FsmException(Problem problem, String message, Throwable cause) {
        super(message, cause);
        this.problem = problem;
    }

    public FsmException(Problem problem, Throwable cause) {
        super(cause);
    }

    public FsmException(Problem problem, String messagePattern, Object ...params) {
        super(MessageFormatter.arrayFormat(messagePattern, params).getMessage(), MessageFormatter.getThrowableCandidate(params));
        this.problem = problem;
    }

    public Problem getProblem() {
        return problem;
    }
}
