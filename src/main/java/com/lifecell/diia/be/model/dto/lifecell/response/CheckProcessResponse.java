package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class CheckProcessResponse extends Response {
    private List<Order> orders = new ArrayList<>();

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString
    public static class Order {
        private String orderId;
        private String msisdn;
        private OffsetDateTime orderCreateDate;
        private String orderState;
    }
}
