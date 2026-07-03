package com.lifecell.diia.be.test;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.lifecell.diia.be.model.dto.ApplicationStatus;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.grpc.model.ApplicationStatusPostRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProtoUtilsTest {

    @Test
    public void tesCallbackOrderStatus() {
        var data = ApplicationStatusPostRequest.newBuilder()
                .setStatus(OrderState.BROADCAST.getRef())
                .setReason("OK")
                .setApplicationData(Struct.newBuilder()
                        .putFields("cancelPossibility", Value.newBuilder().setBoolValue(true).build())
                        .putFields("tariffUa",  Value.newBuilder().setStringValue("TARIFF_NAME").build())
                        .putFields("tariffCode",  Value.newBuilder().setStringValue("TARIFF_CODE").build())
                        .putFields("orderCreateDate", Value.newBuilder().setStringValue(OffsetDateTime.now().toString()).build())
                        .putFields("dueDate", Value.newBuilder().setStringValue(OffsetDateTime.now().toString()).build())
                        .build())
                .build();
        var result = new ApplicationStatus(data);
        assertEquals(data.getStatus(), result.getStatus().getRef());
        assertNotNull(result.getApplicationData());
        assertEquals(data.getApplicationData().getFieldsOrThrow("cancelPossibility").getBoolValue(), result.getApplicationData().getCancelPossibility());
        assertEquals(data.getApplicationData().getFieldsOrThrow("tariffUa").getStringValue(), result.getApplicationData().getTariffUa());
        assertEquals(data.getApplicationData().getFieldsOrThrow("tariffCode").getStringValue(), result.getApplicationData().getTariffCode());
        assertEquals(
                OffsetDateTime.parse(data.getApplicationData().getFieldsOrThrow("orderCreateDate").getStringValue()).toInstant(),
                result.getApplicationData().getOrderCreateDate().toInstant()
        );
        assertEquals(
                OffsetDateTime.parse(data.getApplicationData().getFieldsOrThrow("dueDate").getStringValue()).toInstant(),
                result.getApplicationData().getDueDate().toInstant()
        );
          }
}
