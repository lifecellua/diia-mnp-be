package com.lifecell.diia.be.model.fsm;

import com.lifecell.diia.be.exception.GenericException;

public enum Stage {
    STAGE_START(State.PROGRESS),

    //Branch 1
    STAGE_CHECK(State.PROGRESS),
    STAGE_INTRO(State.PROGRESS),
    STAGE_TARIFFS(State.PROGRESS),
    STAGE_PHONE(State.PROGRESS),
    STAGE_OTP(State.PROGRESS),
    STAGE_AGREEMENT(State.PROGRESS),
    STAGE_SIM_INTRO(State.PROGRESS),
    STAGE_SIM(State.PROGRESS),
    STAGE_COMMIT(State.PROGRESS),
    STAGE_SUCCESS(State.SUCCESS),

    //Branch 2
    STAGE_ORDERS(State.PROGRESS),
    STAGE_ORDER(State.PROGRESS),
    STAGE_ORDER_SIM_OTP(State.PROGRESS),
    STAGE_ORDER_SIM(State.PROGRESS),
    STAGE_ORDER_SIM_SUCCESS(State.PROGRESS),
    STAGE_ORDER_CANCEL_INTRO(State.PROGRESS),
    STAGE_ORDER_CANCEL_OTP(State.PROGRESS),
    STAGE_ORDER_CANCEL(State.PROGRESS),
    STAGE_ORDER_CANCEL_SUCCESS(State.PROGRESS),

    //Error
    STAGE_FAIL(State.FAIL),
    STAGE_ERROR_NO_TAX_DOCUMENT(State.ERROR),
    STAGE_ERROR_NO_ID_DOCUMENT(State.ERROR),
    STAGE_ERROR_NO_SERVICE(State.ERROR);

    private State state;

    private Stage(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public static Stage fromRef(String id) {
        if ((id == null) || id.isEmpty()) {
            return null;
        } else {
            for (var stage : Stage.values()) {
                if (stage.toRef().equalsIgnoreCase(id)) {
                    return stage;
                }
            }
            throw new GenericException("Unknown stage " + id);
        }
    }

    public String toRef() {
        return name().toLowerCase();
    }
}
