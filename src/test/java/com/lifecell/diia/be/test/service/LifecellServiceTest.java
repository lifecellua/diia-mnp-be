package com.lifecell.diia.be.test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifecell.diia.be.exception.LifecellException;
import com.lifecell.diia.be.model.dto.CustomerIdDocument;
import com.lifecell.diia.be.model.dto.CustomerIdDocumentType;
import com.lifecell.diia.be.model.dto.lifecell.CancelOrder;
import com.lifecell.diia.be.model.dto.lifecell.CheckIccid;
import com.lifecell.diia.be.model.dto.lifecell.CheckOrder;
import com.lifecell.diia.be.model.dto.lifecell.CheckPossibility;
import com.lifecell.diia.be.model.dto.lifecell.CheckProcess;
import com.lifecell.diia.be.model.dto.lifecell.CreateOrder;
import com.lifecell.diia.be.model.dto.lifecell.GenerateDocument;
import com.lifecell.diia.be.model.dto.lifecell.GetDocument;
import com.lifecell.diia.be.model.dto.lifecell.GetPortingDates;
import com.lifecell.diia.be.model.dto.lifecell.SendOtp;
import com.lifecell.diia.be.model.dto.lifecell.UpdateOrder;
import com.lifecell.diia.be.model.dto.lifecell.VerifyOtp;
import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.MessageService;
import com.lifecell.diia.be.service.ValidateService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecellServiceTest {

    @Mock
    private MessageService messageService;
    @Mock
    private ValidateService validateService;

    private LifecellService lifecellService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String transactionId = "fccadb2c-56e7-43b1-94bf-df3d1e90fd97";
    private final String userRef = "user1";
    private final String msisdn = "0931112233";
    private final String otp = "123456";
    private final String icid = "12341956039556";
    private final String puc1 = "19560395";
    private final String tariff = "IND_PRE_MAXI";
    private final String documentId = "E_DOC_PPREG_EDS:9258121f-a64b-4a13-aa49-e41421968a4d";
    private final String token = "6O0b1lS746us";
    private final String orderData = "ewrCoCAiaW50ZXJuYWwtcGFzc3BvcnQiOiB7CsKgIMKgICJm";
    private final String orderId = "983243";

    @BeforeEach
    void setUp() {
        lifecellService = new LifecellService(messageService, validateService);
    }

    @SneakyThrows
    @Test
    void checkPossibilitySuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "resultDescription": "Success",
                    "operatorName": "LIFECELL_DUMMY",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("checkPossibility"), any(CheckPossibility.class))).thenReturn(mockNode);
        var result = lifecellService.checkPossibility(transactionId, userRef, msisdn);

        assertEquals(ResultCode.SUCCESS, result);
    }

    @SneakyThrows
    @Test
    void checkPossibilityError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                 "operationResult": {
                     "resultCode": -48,
                     "resultDescription": "Msisdn can not portation becouse 30 days not ended adter last portation",
                     "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                  }
            }
        """);
        when(messageService.send(eq("checkPossibility"), any(CheckPossibility.class))).thenReturn(mockNode);
        var result = lifecellService.checkPossibility(transactionId, userRef, msisdn);

        assertEquals(ResultCode.MSISDN_CANNOT_PORT, result);
    }

    @SneakyThrows
    @Test
    void sendOtpSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "resultDescription": "Success",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                 }
            }
        """);
        when(messageService.send(eq("sendOtp"), any(SendOtp.class))).thenReturn(mockNode);

        assertDoesNotThrow(() -> lifecellService.sendOtp(transactionId, userRef, msisdn));
    }

    @SneakyThrows
    @Test
    void sendOtpError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -56,
                    "resultDescription": "Msisdn is blocked",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
               }
            }
        """);
        when(messageService.send(eq("sendOtp"), any(SendOtp.class))).thenReturn(mockNode);

        var ex = assertThrows(LifecellException.class, () ->
            lifecellService.sendOtp(transactionId, userRef, msisdn)
        );
        assertEquals(ResultCode.MSISDN_IS_BLOCKED, ex.getResultCode());
    }

    @SneakyThrows
    @Test
    void verifyOtpAndGetTokenSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "resultDescription": "Success check otp",
                    "token": "token123",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("verifyOtp"), any(VerifyOtp.class))).thenReturn(mockNode);

        var result = lifecellService.verifyOtpAndGetToken(transactionId, userRef, msisdn, otp);
        assertEquals("token123", result.getToken());
        assertEquals(ResultCode.SUCCESS, result.getResultCode());
    }

    @SneakyThrows
    @Test
    void verifyOtpAndGetTokenError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -7,
                    "resultDescription": "Not valid otp",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("verifyOtp"), any(VerifyOtp.class))).thenReturn(mockNode);

        var result = lifecellService.verifyOtpAndGetToken(transactionId, userRef, msisdn, otp);
        assertNull(result.getToken());
        assertEquals(ResultCode.NOT_VALID_OTP, result.getResultCode());
    }

    @SneakyThrows
    @Test
    void ckeckIccdAndPuk1Success() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "resultDescription": "Success",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("checkIccid"), any(CheckIccid.class))).thenReturn(mockNode);

        var result = lifecellService.ckeckIccdAndPuk1(transactionId, userRef, msisdn, icid, puc1);
        assertEquals(ResultCode.SUCCESS, result);
    }

    @SneakyThrows
    @Test
    void ckeckIccdAndPuk1Error() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -61,
                    "resultDescription": "Incorrect puk1",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("checkIccid"), any(CheckIccid.class))).thenReturn(mockNode);

        var result = lifecellService.ckeckIccdAndPuk1(transactionId, userRef, msisdn, icid, puc1);
        assertEquals(ResultCode.INCORRECT_PUK_ONE, result);
    }

    @SneakyThrows
    @Test
    void checkProcessSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "orders": [
                        {
                            "orderId": 119098,
                            "msisdn": 380624100099,
                            "orderCreateDate": "2025-08-28T14:34:24.000+03:00",
                            "orderState": "cancelByUser"
                        },
                        {
                            "orderId": 119099,
                            "msisdn": 380624100100,
                            "orderCreateDate": "2025-08-28T14:43:53.000+03:00",
                            "orderState": "cancelByUser"
                        },
                        {
                            "orderId": 119102,
                            "msisdn": 380624100100,
                            "orderCreateDate": "2025-08-28T16:47:15.000+03:00",
                            "orderState": "cancelByUser"
                        }
                    ],
                    "resultDescription": "Success",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("checkProcess"), any(CheckProcess.class))).thenReturn(mockNode);

        var orders = lifecellService.checkProcess(transactionId, userRef);
        assertNotNull(orders);
        assertEquals(3, orders.size());
    }

    @SneakyThrows
    @Test
    void checkProcessNotFoundOrdersByUser() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "resultDescription": "Success",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("checkProcess"), any(CheckProcess.class))).thenReturn(mockNode);

        var result = lifecellService.checkProcess(transactionId, userRef);
        assertEquals(0, result.size());
    }

    @SneakyThrows
    @Test
    void checkProcessError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -2,
                    "resultDescription": "Internal error",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("checkProcess"), any(CheckProcess.class))).thenReturn(mockNode);

        var ex = assertThrows(LifecellException.class, () ->
                lifecellService.checkProcess(transactionId, userRef)
        );
        assertEquals(ResultCode.UNKNOWN_ERROR, ex.getResultCode());
    }

    @SneakyThrows
    @Test
    void getPortingDatesSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                 "operationResult": {
                     "cancelProhibition": true,
                     "resultCode": 0,
                     "resultDescription": "Success get porting dates",
                     "nearestPortingDate": "2025-08-28T14:00:00.000+03:00",
                     "cancelProhibition":"true",
                     "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                 }
             }
        """);
        when(messageService.send(eq("getPortingDates"), any(GetPortingDates.class))).thenReturn(mockNode);

        var result = lifecellService.getPortingDates(transactionId, userRef);
        assertEquals("28.08.2025 14:00", result.getPortingDate());
        assertEquals("Недоступно", result.getCancelingDate());
        assertFalse(result.getCancelingDateAvailable());
    }

    @SneakyThrows
    @Test
    void getPortingDatesError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -2,
                    "resultDescription": "Internal error",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("getPortingDates"), any(GetPortingDates.class))).thenReturn(mockNode);

        var result = lifecellService.getPortingDates(transactionId, userRef);
        assertEquals("Недоступно", result.getPortingDate());
        assertEquals("Недоступно", result.getCancelingDate());
        assertFalse(result.getCancelingDateAvailable());
    }

    @SneakyThrows
    @Test
    void generateDocumentSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "documentId": "E_DOC_PPREG_EDS:9258121f-a64b-4a13-aa49-e41421968a4d",
                    "resultDescription": "SUCCESS",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                 }
            }
        """);
        when(messageService.send(eq("generateDocument"), any(GenerateDocument.class))).thenReturn(mockNode);

        var result = lifecellService.generateDocument(transactionId, userRef, msisdn, tariff, "documentType", "documentData");
        assertEquals(documentId, result);
    }

    @SneakyThrows
    @Test
    void generateDocumentError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -2,
                    "resultDescription": "Internal error",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("generateDocument"), any(GenerateDocument.class))).thenReturn(mockNode);

        var document = new CustomerIdDocument();
        document.setType(CustomerIdDocumentType.PASSPORT_INT);

        var ex = assertThrows(LifecellException.class, () ->
                lifecellService.generateDocument(transactionId, userRef, msisdn, tariff, "documentType", "documentData")
        );
        assertEquals(ResultCode.UNKNOWN_ERROR, ex.getResultCode());
    }

    @SneakyThrows
    @Test
    void getDocumentSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": "0",
                    "resultDescription": "Success",
                    "documentName": "380632107380_registration_doc.pdf",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97" ,
                     "documentData": "JVBERi0xLjUKJ"
                }
            }
        """);
        when(messageService.send(eq("getDocument"), any(GetDocument.class))).thenReturn(mockNode);

        var result = lifecellService.getDocument(transactionId, userRef, msisdn, documentId);
        assertEquals("JVBERi0xLjUKJ", result);
    }

    @SneakyThrows
    @Test
    void getDocumentError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -2,
                    "resultDescription": "Internal error",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("getDocument"), any(GetDocument.class))).thenReturn(mockNode);

        var ex = assertThrows(LifecellException.class, () ->
                lifecellService.getDocument(transactionId, userRef, msisdn, documentId)
        );
        assertEquals(ResultCode.UNKNOWN_ERROR, ex.getResultCode());
    }

    @SneakyThrows
    @Test
    void createOrderSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                    "orderId": "983243",
                    "resultDescription": "Success",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                 }
            }
        """);
        when(messageService.send(eq("createOrder"), any(CreateOrder.class))).thenReturn(mockNode);

        var result = lifecellService.createOrder(transactionId, userRef, msisdn, tariff, token, icid, puc1, orderData);
        assertEquals(orderId, result);
    }

    @SneakyThrows
    @Test
    void createOrderError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                 "operationResult": {
                     "resultCode": -47,
                     "resultDescription": "Msisdn is lifecell",
                     "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                  }
             }
        """);
        when(messageService.send(eq("createOrder"), any(CreateOrder.class))).thenReturn(mockNode);

        var ex = assertThrows(LifecellException.class, () ->
                lifecellService.createOrder(transactionId, userRef, msisdn, tariff, token, icid, puc1, orderData)
        );
        assertEquals(ResultCode.MSISDN_IS_LIFECELL, ex.getResultCode());
    }

    @SneakyThrows
    @Test
    void updateOrderSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                   "resultDescription": "Success",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                 }
            }
        """);
        when(messageService.send(eq("updateOrder"), any(UpdateOrder.class))).thenReturn(mockNode);

        var result = lifecellService.updateOrder(transactionId, userRef, orderId, msisdn, icid, puc1, token);
        assertEquals(ResultCode.SUCCESS, result);
    }

    @SneakyThrows
    @Test
    void updateOrderError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -6,
                    "resultDescription": "Bad iccid",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
               }
            }
        """);
        when(messageService.send(eq("updateOrder"), any(UpdateOrder.class))).thenReturn(mockNode);

        var result = lifecellService.updateOrder(transactionId, userRef, orderId, msisdn, icid, puc1, token);
        assertEquals(ResultCode.BAD_ICCID, result);
    }

    @SneakyThrows
    @Test
    void checkOrderSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult":{
                    "orderId":983243,
                    "cancelPossibility":false,
                    "resultCode":0,
                    "transactionId":"fccadb2c-56e7-43b1-94bf-df3d1e90fd97",
                    "orderState":"activatedMsisdn",
                    "iccid":8938006230062352302,
                    "orderCreateDate": "2025-08-29T09:06:11.000+03:00",
                    "dueDate": "2025-08-29T09:06:11.000+03:00",
                     "tariffUa":"Максі",
                    "tariffCode":"IND_PRE_MAXI",
                    "resultDescription":"Success check order status",
                    "msisdn":380624561443
                }
            }
        """);
        when(messageService.send(eq("checkOrder"), any(CheckOrder.class))).thenReturn(mockNode);

        var result = lifecellService.checkOrder(transactionId, userRef, orderId, msisdn);
        assertEquals(orderId, result.getOrderId());
        assertEquals(ResultCode.SUCCESS, result.getResultCode());
    }

    @SneakyThrows
    @Test
    void checkOrdersSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                        "operationResult": {
                            "resultCode": 0,
                            "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97",
                            "orders": [
                                {
                                    "orderId": 118360,
                                    "msisdn": 380622074282,
                                    "orderCreateDate": "2025-08-29T09:06:11.000+03:00",
                                    "dueDate": "2025-08-29T09:06:11.000+03:00",
                                    "resultCode": 0,
                                    "resultDescription": "Success check order status",
                                    "orderState": "cancel",
                                    "tariffCode": "IND_PRE_MAXI",
                                    "tariffUa": "Максі",
                                    "rejectCode": 418,
                                    "cancelPossibility": false
                                },
                                {
                                    "orderId": 118359,
                                    "resultCode": -49,
                                    "resultDescription": "Incorret user for this order"
                                }
                            ],
                            "resultDescription": "Success"
                        }
                    }
        """);
        when(messageService.send(eq("checkOrder"), any(CheckOrder.class))).thenReturn(mockNode);

        var result = lifecellService.checkOrders(transactionId, List.of("118360","118359"));
        assertEquals(1, result.size());
    }

    @SneakyThrows
    @Test
    void cancelOrderSuccess() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": 0,
                   "resultDescription": "Success",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                 }
            }
        """);
        when(messageService.send(eq("cancelOrder"), any(CancelOrder.class))).thenReturn(mockNode);

        assertDoesNotThrow(() -> lifecellService.cancelOrder(transactionId, msisdn, userRef, orderId, token));
    }

    @SneakyThrows
    @Test
    void cancelOrderError() {
        JsonNode mockNode = objectMapper.readTree("""
            {
                "operationResult": {
                    "resultCode": -51,
                    "resultDescription": "Msisdn is not in this order",
                    "transactionId": "fccadb2c-56e7-43b1-94bf-df3d1e90fd97"
                }
            }
        """);
        when(messageService.send(eq("cancelOrder"), any(CancelOrder.class))).thenReturn(mockNode);

        var ex = assertThrows(LifecellException.class, () ->
                lifecellService.cancelOrder(transactionId, msisdn, userRef, orderId, token)
        );
        assertEquals(ResultCode.MSISDN_IS_NOT_IN_THIS_ORDER, ex.getResultCode());
    }
}
