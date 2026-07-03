package com.lifecell.diia.be.test;

import com.lifecell.diia.be.service.JsonService;
import com.lifecell.diia.grpc.model.FormAnyGetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JsonTest {
    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

    @Test
    public void testFormAgreement() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/agreement.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(7, grpc.getBodyCount());
    }

    @Test
    public void testFormOrder() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/order.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(3, grpc.getBodyCount());
    }

    @Test
    public void testFormNoPassport() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/no_passport.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(2, grpc.getBodyCount());
    }


    @Test
    public void testFormNoTaxNumber() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/no_tax_number.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(2, grpc.getBodyCount());
    }

    @Test
    public void testOtp() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/otp.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(5, grpc.getBodyCount());
    }

    @Test
    public void testFormSim() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/sim.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(4, grpc.getBodyCount());
    }

    @Test
    public void testFormIntro() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/intro.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(4, grpc.getBodyCount());
    }

    @Test
    public void testFormOrders() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/orders.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(5, grpc.getBodyCount());
    }

    @Test
    public void testFormTariffs() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/tariffs.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(4, grpc.getBodyCount());
    }

    @Test
    public void testFormPhone() {
        var gs = new JsonService();
        var text = assertDoesNotThrow(() -> resourceLoader.getResource("classpath:forms/phone.json").getContentAsString(StandardCharsets.UTF_8));
        var grpc = assertDoesNotThrow(() -> gs.convert(text, FormAnyGetResponse.class));
        assertEquals(1, grpc.getTopGroupCount());
        assertEquals(4, grpc.getBodyCount());
    }
}
