package com.lifecell.diia.be.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifecell.diia.be.exception.LifecellException;
import com.lifecell.diia.be.model.dto.DatesInfo;
import com.lifecell.diia.be.model.dto.TokenInfo;
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
import com.lifecell.diia.be.model.dto.lifecell.response.CheckIccidResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.CheckOrderResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.CheckPossibilityResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.CheckProcessResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.CreateOrderResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.GenerateDocumentResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.GetDocumentResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.GetPortingDatesResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.OperationResult;
import com.lifecell.diia.be.model.dto.lifecell.response.Response;
import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.model.dto.lifecell.response.TariffInfo;
import com.lifecell.diia.be.model.dto.lifecell.response.VerifyOtpResponse;
import com.lifecell.diia.be.util.LifecellUtils;
import com.lifecell.diia.be.util.ResourceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LifecellService {
    private static final String CHECK_MNP_PROCESS = "checkMNPprocess";
    private static final String CHECK_MNP_POSSIBILITY = "checkMnpPosssibility";
    private static final String SEND_OTP_MNP = "sendOtpMNP";
    private static final String VERIFY_OTP_MNP = "verifyOtpMNP";
    private static final String GET_PORTING_DATES_MNP = "getPortingDatesMNP";
    private static final String GENERATE_DOCUMENT = "generateDocument";
    private static final String GET_DOCUMENT = "getDocument";
    private static final String CREATE_ORDER_MNP = "createOrderMNP";
    private static final String UPDATE_ORDER_MNP = "updateOrderMNP";
    private static final String CHECK_ORDER_MNP = "checkOrderMNP";
    private static final String CANCEL_ORDER_MNP = "cancelOrderMNP";
    private static final String CHECK_ICCID_FOR_MNP = "checkIccidForMNP";

    private final MessageService messageService;
    private final ObjectMapper mapper;
    private final ValidateService validateService;

    public LifecellService(
            MessageService messageService,
            ValidateService validateService) {
        this.messageService = messageService;
        this.validateService = validateService;
        this.mapper = ResourceUtils.getDefaultObjectMapper();
    }

    public List<TariffInfo> getTariffs() {
        var json = ResourceUtils.loadDataJson("tariffs");
        try {
            return mapper.readValue(json, new TypeReference<List<TariffInfo>>(){});
        } catch (IOException e) {
            throw new LifecellException(ResultCode.BAD_INPUT, "Can't read tariffs", e);
        }
    }

    public boolean checkHealth() {
        return true;
    }

    public ResultCode checkPossibility(String transactionId, String userRef, String msisdn) {
        var req = CheckPossibility.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .msisdn(msisdn)
                .build();
        var rsp = checkPossibilityRaw(req);
        return rsp.getResultCode();
    }

    private CheckPossibilityResponse checkPossibilityRaw(CheckPossibility checkPossibility) {
        checkPossibility.setOperationName(CHECK_MNP_POSSIBILITY);
        log.info("LifecellService.checkPossibility() request: {}", checkPossibility);
        validateService.validate(checkPossibility);
        var data = messageService.send("checkPossibility", checkPossibility);
        var response = mapToResponse(data, new TypeReference<OperationResult<CheckPossibilityResponse>>() {});
        log.info("LifecellService.checkPossibility response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public void sendOtp(String transactionId, String userRef, String msisdn) {
        var req = SendOtp.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .msisdn(msisdn)
                .build();
        var rsp = sendOtpRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            throw new LifecellException(rsp.getResultCode(), rsp.getResultDescription());
        }
    }

    private Response sendOtpRaw(SendOtp sendOtp) {
        sendOtp.setOperationName(SEND_OTP_MNP);
        log.info("LifecellService.sendOtp() request: {}", sendOtp);
        validateService.validate(sendOtp);
        var data = messageService.send("sendOtp", sendOtp);
        var response = mapToResponse(data, new TypeReference<OperationResult<Response>>() {});
        log.info("LifecellService.sendOtp response() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public TokenInfo verifyOtpAndGetToken(String transactionId, String userRef, String msisdn, String otp) {
        var req = VerifyOtp.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .msisdn(msisdn)
                .otp(otp)
                .build();
        var rsp = verifyOtpRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            return TokenInfo.builder()
                    .resultCode(rsp.getResultCode())
                    .token(null)
                    .build();
        }
        return TokenInfo.builder()
                .resultCode(rsp.getResultCode())
                .token(rsp.getToken())
                .build();
    }

    private VerifyOtpResponse verifyOtpRaw(VerifyOtp verifyOtp) {
        verifyOtp.setOperationName(VERIFY_OTP_MNP);
        log.info("LifecellService.verifyOtp() request: {}, ", verifyOtp);
        validateService.validate(verifyOtp);
        var data = messageService.send("verifyOtp", verifyOtp);
        var response = mapToResponse(data, new TypeReference<OperationResult<VerifyOtpResponse>>() {});
        log.info("LifecellService.verifyOtp response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public ResultCode ckeckIccdAndPuk1(String transactionId, String userRef, String msisdn, String iccd, String puk1) {
        var req = CheckIccid.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .msisdn(msisdn)
                .iccid(iccd)
                .puk1(puk1)
                .build();
        var rsp = checkIccidRaw(req);
        return rsp.getResultCode();
    }

    private CheckIccidResponse checkIccidRaw(CheckIccid checkIccid) {
        checkIccid.setOperationName(CHECK_ICCID_FOR_MNP);
        log.info("LifecellService.checkIccid() request: {}", checkIccid);
        validateService.validate(checkIccid);
        var data = messageService.send("checkIccid", checkIccid);
        var response = mapToResponse(data, new TypeReference<OperationResult<CheckIccidResponse>>() {});
        log.info("LifecellService.checkIccid() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public List<CheckProcessResponse.Order> checkProcess(String transactionId, String userRef) {
        var req = CheckProcess.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .build();
        var rsp = checkProcessRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            throw new LifecellException(rsp.getResultCode(), rsp.getResultDescription());
        }
        return rsp.getOrders();
    }

    private CheckProcessResponse checkProcessRaw(CheckProcess checkProcess) {
        checkProcess.setOperationName(CHECK_MNP_PROCESS);
        log.info("LifecellService.checkProcess() request: {}", checkProcess);
        validateService.validate(checkProcess);
        var data = messageService.send("checkProcess", checkProcess);
        var response = mapToResponse(data, new TypeReference<OperationResult<CheckProcessResponse>>() {});
        log.info("LifecellService.checkProcess() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public DatesInfo getPortingDates(String transactionId, String userRef) {
        final var UNKNOWN_DATE = "Недоступно";
        var req = GetPortingDates.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .build();
        var rsp = getPortingDatesRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            return DatesInfo.builder()
                    .portingDate(UNKNOWN_DATE)
                    .cancelingDateAvailable(false)
                    .cancelingDate(UNKNOWN_DATE)
                    .build();
        }
        return DatesInfo.builder()
                .portingDate(rsp.getNearestPortingDate() != null
                        ? rsp.getNearestPortingDate().toLocalDateTime().format(LifecellUtils.PRETTY_DATE_TIME)
                        : UNKNOWN_DATE)
                .cancelingDateAvailable(
                        Boolean.FALSE.equals(rsp.getCancelProhibition())
                                || (Boolean.TRUE.equals(rsp.getCancelProhibition()) && (rsp.getCancelProhibitionDate() != null)))
                .cancelingDate(rsp.getCancelProhibitionDate() != null
                        ? rsp.getCancelProhibitionDate().toLocalDateTime().format(LifecellUtils.PRETTY_DATE_TIME)
                        : UNKNOWN_DATE)
                .build();
    }

    private GetPortingDatesResponse getPortingDatesRaw(GetPortingDates getPortingDates) {
        getPortingDates.setOperationName(GET_PORTING_DATES_MNP);
        log.info("LifecellService.getPortingDates() request: {}", getPortingDates);
        validateService.validate(getPortingDates);
        var data = messageService.send("getPortingDates", getPortingDates);
        var response = mapToResponse(data, new TypeReference<OperationResult<GetPortingDatesResponse>>() {});
        log.info("LifecellService.getPortingDates() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public String generateDocument(String transactionId, String userRef, String msisdn, String tariff, String documentType, String documentData) {
        var req = GenerateDocument.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .msisdn(msisdn)
                .documentType(documentType)
                .tariff(tariff)
                .customerData(documentData)
                .build();
        var rsp = generateDocumentRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            throw new LifecellException(rsp.getResultCode(), rsp.getResultDescription());
        }
        return rsp.getDocumentId();
    }

    private GenerateDocumentResponse generateDocumentRaw(GenerateDocument generateDocument) {
        generateDocument.setOperationName(GENERATE_DOCUMENT);
        log.info("LifecellService.generateDocument() request: {}", generateDocument);
        validateService.validate(generateDocument);
        var data = messageService.send("generateDocument", generateDocument);
        var response = mapToResponse(data, new TypeReference<OperationResult<GenerateDocumentResponse>>() {});
        log.info("LifecellService.generateDocument() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public String getDocument(String transactionId, String userRef, String msisdn, String documentId) {
        var req = GetDocument.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .msisdn(msisdn)
                .documentId(documentId)
                .build();
        var rsp = getDocumentRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            throw new LifecellException(rsp.getResultCode(), rsp.getResultDescription());
        }
        return rsp.getDocumentData();
    }

    private GetDocumentResponse getDocumentRaw(GetDocument getDocument) {
        getDocument.setOperationName(GET_DOCUMENT);
        log.info("LifecellService.getDocument() request: {}", getDocument);
        validateService.validate(getDocument);
        var data = messageService.send("getDocument", getDocument);
        var response = mapToResponse(data, new TypeReference<OperationResult<GetDocumentResponse>>() {});
        log.info("LifecellService.getDocument() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public String createOrder(String transactionId, String userRef, String msisdn, String tariff, String token, String iccid, String puk1, String data) {
        var req = CreateOrder.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .msisdn(msisdn)
                .tariff(tariff)
                .token(token)
                .iccid(iccid)
                .puk1(puk1)
                .customerData(data)
                .build();
        var rsp = createOrderRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            throw new LifecellException(rsp.getResultCode(), rsp.getResultDescription());
        }
        return rsp.getOrderId();
    }

    private CreateOrderResponse createOrderRaw(CreateOrder createOrder) {
        createOrder.setOperationName(CREATE_ORDER_MNP);
        log.info("LifecellService.createOrder() request: {}", createOrder);
        validateService.validate(createOrder);
        var data = messageService.send("createOrder", createOrder);
        var response = mapToResponse(data, new TypeReference<OperationResult<CreateOrderResponse>>() {});
        log.info("LifecellService.createOrder() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public ResultCode updateOrder(String transactionId, String userRef, String orderId, String msisdn, String iccid, String puk1, String token) {
        var req = UpdateOrder.builder()
                .transactionId(transactionId)
                .orderId(orderId)
                .posId(userRef)
                .msisdn(msisdn)
                .iccid(iccid)
                .puk1(puk1)
                //.tariff(null)
                .token(token)
                .build();
        var rsp = updateOrderRaw(req);
        return rsp.getResultCode();
    }

    private Response updateOrderRaw(UpdateOrder updateOrder) {
        updateOrder.setOperationName(UPDATE_ORDER_MNP);
        log.info("LifecellService.updateOrder() request: {}", updateOrder);
        validateService.validate(updateOrder);
        var data = messageService.send("updateOrder", updateOrder);
        var response = mapToResponse(data, new TypeReference<OperationResult<Response>>() {});
        log.info("LifecellService.updateOrder() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public CheckOrderResponse checkOrder(String transactionId, String userRef, String orderId, String msisdn) {
        var req = CheckOrder.builder()
                .transactionId(transactionId)
                .posId(userRef)
                .orderId(orderId)
                .msisdn(msisdn)
                .build();
        return checkOrderRaw(req);
    }

    public List<CheckOrderResponse> checkOrders(String transactionId, List<String> orderIds) {
        var oIds = orderIds.stream().collect(Collectors.joining(","));
        var req = CheckOrder.builder()
                .transactionId(transactionId)
                .orderId(oIds)
                .build();
        var rsp = checkOrderRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            return null;
        }
        if (rsp.getOrders() == null) {
            return null;
        }
        var orders = rsp.getOrders().stream()
                .filter(o -> o.getResultCode() == ResultCode.SUCCESS)
                .toList();
        return orders;
    }

    private CheckOrderResponse checkOrderRaw(CheckOrder checkOrder) {
        checkOrder.setOperationName(CHECK_ORDER_MNP);
        log.info("LifecellService.checkOrder() request: {}", checkOrder);
        validateService.validate(checkOrder);
        var data = messageService.send("checkOrder", checkOrder);
        var response = mapToResponse(data, new TypeReference<OperationResult<CheckOrderResponse>>() {});
        log.info("LifecellService.checkOrder() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    public void cancelOrder(String transactionId, String msisdn, String userRef, String orderId, String token) {
        var req = CancelOrder.builder()
                .transactionId(transactionId)
                .msisdn(msisdn)
                .posId(userRef)
                .orderId(orderId)
                .token(token)
                .build();
        var rsp = cancelOrderRaw(req);
        if (rsp.getResultCode() != ResultCode.SUCCESS) {
            throw new LifecellException(rsp.getResultCode(), rsp.getResultDescription());
        }
    }

    private Response cancelOrderRaw(CancelOrder cancelOrder) {
        cancelOrder.setOperationName(CANCEL_ORDER_MNP);
        log.info("LifecellService.cancelOrder() request: {}", cancelOrder);
        validateService.validate(cancelOrder);
        var data = messageService.send("cancelOrder", cancelOrder);
        var response = mapToResponse(data, new TypeReference<OperationResult<Response>>() {});
        log.info("LifecellService.cancelOrder() response: {}", response.getOperationResult());
        return response.getOperationResult();
    }

    private  <T> T mapToResponse(JsonNode j, TypeReference<T> typeReference) {
        return mapper.convertValue(j, typeReference);
    }
}
