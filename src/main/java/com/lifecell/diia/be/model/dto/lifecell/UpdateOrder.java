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
public class UpdateOrder extends Request {
    //@NotBlank(message = "cannot be blank")
    private String msisdn;
    @NotBlank(message = "cannot be blank")
    private String posId;
    @NotBlank(message = "cannot be blank")
    private String orderId;
    //@NotBlank(message = "cannot be blank")
    private String token;
    @NotBlank(message = "cannot be blank")
    private String iccid;
    private String tariff;
    private String puk1;
}
