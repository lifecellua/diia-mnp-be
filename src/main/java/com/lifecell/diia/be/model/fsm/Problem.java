package com.lifecell.diia.be.model.fsm;

public enum Problem {
    FAIL(88101003, true),
    FAIL_INVALID_TARIFF(88101003, true),
    FAIL_INVALID_ORDER(88101001, true),

    FAIL_INVALID_PHONE_TRACKED(88101012, true),
    FAIL_INVALID_PHONE_DUPLICATED(88101010, true),
    FAIL_INVALID_PHONE_BLOCKED(88101009, true),
    FAIL_CAN_NOT_CANCEL_ORDER(88101018, true),

    INVALID_PHONE(88101013, false),
    INVALID_PHONE_OUR(88131002, false),
    INVALID_PHONE_EARLY(88101011, false),
    INVALID_OTP(88101008, false),
    INVALID_ICCID(88101007, false),
    INVALID_ICCID_BLOCKED(88101005, false),
    INVALID_ICCID_USED(88101006, false),
    INVALID_PUK1(88101014, false),
    INVALID_SIM(88101015, false),
    INVALID_DOCUMENT(88101004, false),

    FORM_SIM_INTRO(88131001, false),
    FORM_ORDER_CANCEL(88111002, false),
    FORM_ORDER_SIM_SUCCESS(88121001, false),
    FORM_ORDER_CANCEL_SUCCESS(88111001, false),

    RETRY(88101003, false),

    SUCCESS(88121002, false);

    private int code;
    private boolean fatal;

    private Problem(int code, boolean fatal) {
        this.code = code;
        this.fatal = fatal;
    }

    public int getCode() {
        return code;
    }

    public static Problem fromCode(int code) {
        for (Problem problem : Problem.values()) {
            if (problem.code == code) {
                return problem;
            }
        }
        return Problem.FAIL;
    }

    public boolean isFatal() {
        return fatal;
    }
}
