package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class CheckOrderResponse extends Response {
    private List<CheckOrderResponse> orders;
    private String textUa;
    private String titleUa;
    private String resultCodeType;
    private String orderId;
    private Boolean cancelPossibility;
    private String tariffCode;
    private OrderState orderState;
    private String rejectCode;
    private String iccid;
    private String tariffUa;
    private String msisdn;
    private OffsetDateTime dueDate;
    private String activationLink;
    private OffsetDateTime orderCreateDate;
}
