package com.lifecell.diia.be.model.dto.lifecell.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperationResult<T> {
    private T operationResult;
}
