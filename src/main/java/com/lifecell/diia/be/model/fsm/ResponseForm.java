package com.lifecell.diia.be.model.fsm;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ResponseForm extends Response {
    private String form;
}
