package com.lifecell.diia.be.model.fsm;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ResponseAwait extends Response {
    private String target;
}
