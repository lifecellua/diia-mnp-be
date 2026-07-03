package com.lifecell.diia.be.model.dto.lifecell;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@ToString
public abstract class Request{
    private String transactionId;
    @NotBlank(message = "cannot be blank")
    private String operationName;
}
