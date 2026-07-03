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
public class CreateOrder extends Request {
    @NotBlank(message = "cannot be blank")
    private String msisdn;
    @NotBlank(message = "cannot be blank")
    private String tariff;
    @NotBlank(message = "cannot be blank")
    private String token;
    @NotBlank(message = "cannot be blank")
    private String posId;
    private String pdfAgreement;
    private RegistrationAtDonor registrationAtDonor;
    @NotBlank(message = "cannot be blank")
    private String customerData;
    private String iccid;
    private String puk1;
}
