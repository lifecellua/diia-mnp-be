package com.lifecell.diia.be.model.fsm;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum State {
    FAIL(-3),
    ERROR(-2),
    CANCEL(-1),
    UNKNOWN(0),
    PROGRESS(1),
    SUCCESS(2);

    private int id;

    private State(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static State fromId(int id) {
        for (State state : State.values()) {
            if (state.id == id) {
                return state;
            }
        }
        return UNKNOWN;
    }

    @JsonCreator
    public static State fromRef(String ref) {
        for (State state : State.values()) {
            if (state.name().equalsIgnoreCase(ref)) {
                return state;
            }
        }
        throw new IllegalArgumentException();
    }
}