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
public class VerifyOtp extends Request {
    private String msisdn;
    private String posId;
    @NotBlank(message = "cannot be blank")
    private String otp;
}
