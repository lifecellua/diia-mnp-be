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
public class GenerateDocument extends Request {
    @NotBlank(message = "cannot be blank")
    private String posId;
    @NotBlank(message = "cannot be blank")
    private String msisdn;
    @NotBlank(message = "cannot be blank")
    private String customerData;
    private String documentType;
    private String tariff;
    private Language language;
}
