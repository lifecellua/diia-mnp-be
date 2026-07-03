package com.lifecell.diia.be.test.service;

import com.lifecell.diia.be.model.dto.lifecell.CheckOrder;
import com.lifecell.diia.be.service.ValidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidateServiceTest {

    private ValidateService validateService;
    private final String transactionId = "fccadb2c-56e7-43b1-94bf-df3d1e90fd97";
    private final String userRef = "user1";
    private final String msisdn = "0931112233";
    private final String orderId = "983243";
    private final String operationName = "checkOrderMnp";

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        validateService = new ValidateService(validator);
    }

    @Test
    void validateSuccess() {
        assertDoesNotThrow(() -> validateService.validate(CheckOrder.builder()
                .transactionId(transactionId)
                .operationName(operationName)
                .orderId(orderId)
                .msisdn(msisdn)
                .posId(userRef)
                .build()));
    }

    @Test
    void validateError() {
        var ex = assertThrows(IllegalArgumentException.class, () ->
                validateService.validate(CheckOrder.builder()
                        .transactionId(transactionId)
                        .orderId(orderId)
                        .msisdn(msisdn)
                        .posId(userRef)
                        .build())
        );

        assertEquals("operationName - cannot be blank", ex.getMessage());
        assertEquals(IllegalArgumentException.class, ex.getClass());
    }
}
