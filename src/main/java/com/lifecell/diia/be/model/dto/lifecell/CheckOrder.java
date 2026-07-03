package com.lifecell.diia.be.model.dto.lifecell;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@ToString(callSuper = true)
public class CheckOrder extends Request {
    private String msisdn;
    @NotBlank(message = "cannot be blank")
    private String orderId;
    //@NotBlank(message = "cannot be blank")
    private String posId;
}
