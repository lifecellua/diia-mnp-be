package com.lifecell.diia.be.test.service;

import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.model.db.EOrder;
import com.lifecell.diia.be.model.dto.ApplicationStatus;
import com.lifecell.diia.be.model.dto.lifecell.response.CheckOrderResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.CheckProcessResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.service.DiiaService;
import com.lifecell.diia.be.service.DocumentsService;
import com.lifecell.diia.be.service.LifecellService;
import com.lifecell.diia.be.service.OrdersService;
import com.lifecell.diia.be.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock
    private DiiaService diiaService;
    @Mock
    private DocumentsService documentsService;
    @Mock
    private LifecellService lifecellService;
    @Mock
    private OrdersService ordersService;
    @Mock
    private Properties properties;

    private SupportService supportService;

    private final String transactionId = "fccadb2c-56e7-43b1-94bf-df3d1e90fd97";
    private final String userRef = "user1";
    private final String orderRef = "123456";
    private final UUID orderId = UUID.fromString("fccadb3c-44e7-43b1-94bf-df3d1e90fd44");

    @BeforeEach
    void setUp() {
        Properties.Service service = mock(Properties.Service.class);
        Properties.Service.Orders orders = mock(Properties.Service.Orders.class);
        Properties.Service.PushNotifications pushes = mock(Properties.Service.PushNotifications.class);

        when(properties.getService()).thenReturn(service);
        when(service.getOrders()).thenReturn(orders);
        when(service.getPushNotifications()).thenReturn(pushes);

        when(orders.getRefreshOrdersActiveHoursLimit()).thenReturn(24L);
        when(orders.getMaxSize()).thenReturn(100);
        when(pushes.getDonorAcceptWaitSimCard()).thenReturn("PUSH_WAIT_SIM");
        when(pushes.getDonorAccept()).thenReturn("PUSH_ACCEPT");
        when(pushes.getActivatedMsisdn()).thenReturn("PUSH_ACTIVATED");
        when(pushes.getBroadcast()).thenReturn("PUSH_BROADCAST");

        supportService = new SupportService(properties, diiaService, documentsService, lifecellService, ordersService);
    }

    @Test
    void testRefreshOrderByRefSuccess() {
        EOrder order = new EOrder();
        order.setUserRef(userRef);
        order.setRef(orderRef);
        order.setMsisdn("380631234567");
        order.setStageRef("OLD_STAGE");

        CheckOrderResponse response = new CheckOrderResponse();
        response.setResultCode(ResultCode.SUCCESS);
        response.setOrderState(OrderState.DONOR_ACCEPT);
        response.setTariffCode("new-tariff");
        response.setOrderCreateDate(OffsetDateTime.now());

        when(ordersService.getByRef(orderRef)).thenReturn(Optional.of(order));
        when(lifecellService.checkOrder(anyString(), eq(userRef), eq(orderRef), anyString())).thenReturn(response);

        supportService.refreshOrderByRef(transactionId, orderRef);

        verify(ordersService).save(order);
        assertEquals("new-tariff", order.getTariffRef());
        assertEquals(OrderState.DONOR_ACCEPT.getRef(), order.getStageRef());
    }

    @Test
    void testCallbackWithNotify() {
        EOrder order = new EOrder();
        order.setId(orderId);
        order.setRef(orderRef);
        order.setUserRef(userRef);
        order.setStageRef(OrderState.IN_PROCESS.getRef());

        ApplicationStatus status = new ApplicationStatus();
        status.setApplicationId(orderRef);
        status.setStatus(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD);
        ApplicationStatus.ApplicationData data = new ApplicationStatus.ApplicationData();
        data.setTariffCode("tariff");
        status.setApplicationData(data);

        when(ordersService.getByRef(orderRef)).thenReturn(Optional.of(order));

        supportService.callbackOrder(status);

        verify(ordersService).save(order);
        verify(diiaService).notify(eq(userRef), eq("PUSH_WAIT_SIM"), anyString());
        assertEquals("tariff", order.getTariffRef());
    }

    @Test
    void testCallbackWithOutNotify() {
        EOrder order = new EOrder();
        order.setId(orderId);
        order.setRef(orderRef);
        order.setUserRef(userRef);
        order.setStageRef(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD.getRef());

        ApplicationStatus status = new ApplicationStatus();
        status.setApplicationId(orderRef);
        status.setStatus(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD);
        ApplicationStatus.ApplicationData data = new ApplicationStatus.ApplicationData();
        data.setTariffCode("tariff");
        status.setApplicationData(data);

        when(ordersService.getByRef(orderRef)).thenReturn(Optional.of(order));

        supportService.callbackOrder(status);

        verify(ordersService).save(order);
        verify(diiaService, never()).notify(eq(userRef), anyString(), anyString());
        assertEquals("tariff", order.getTariffRef());
    }

    @Test
    void testRefreshOrdersActiveUpdatable() {
        EOrder order = new EOrder();
        order.setRef(orderRef);

        when(ordersService.getAllActiveByStateId(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(order)));
        when(ordersService.getByRef(orderRef)).thenReturn(Optional.of(order));

        supportService.refreshOrdersActive(transactionId);

        verify(ordersService, atLeastOnce()).getAllActiveByStateId(any());
    }

    @Test
    void testRefreshOrdersActiveNotUpdatable() {

        when(ordersService.getAllActiveByStateId(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        supportService.refreshOrdersActive(transactionId);

        verify(ordersService, atLeastOnce()).getAllActiveByStateId(any());
        verify(ordersService, never()).getByRef(anyString());
        verify(ordersService, never()).save(any());
        verify(diiaService, never()).notify(eq(userRef), anyString(), anyString());
    }

    @Test
    void testFindMissingOrdersAddNewOrderAndUpdate() {
        when(ordersService.existsByUserRefAndStateIdIn(userRef)).thenReturn(true);

        // Lifecell каже, що є замовлення order-123
        CheckProcessResponse.Order remoteOrder = new CheckProcessResponse.Order();
        remoteOrder.setOrderId("123");
        remoteOrder.setMsisdn("380930000000");
        when(lifecellService.checkProcess(transactionId, userRef)).thenReturn(List.of(remoteOrder));

        when(ordersService.getLatestByUserRefForSyncMissing(userRef)).thenReturn(List.of());

        EOrder order = new EOrder();
        order.setId(orderId);
        order.setRef("123");
        order.setUserRef(userRef);
        order.setStageRef(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD.getRef());
        when(ordersService.getAllActiveByUserRefAndStateId(eq(userRef), any())).thenReturn(new PageImpl<>(List.of(order)));

        CheckOrderResponse checkResponse = new CheckOrderResponse();
        checkResponse.setResultCode(ResultCode.SUCCESS);
        checkResponse.setOrderState(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD);
        checkResponse.setOrderCreateDate(OffsetDateTime.now());
        when(lifecellService.checkOrder(transactionId, userRef, "123", "380930000000")).thenReturn(checkResponse);

        supportService.refreshOrdersByUserRef(transactionId, userRef);

        ArgumentCaptor<EOrder> orderCaptor = ArgumentCaptor.forClass(EOrder.class);
        verify(ordersService, atLeastOnce()).save(orderCaptor.capture());
        assertEquals("123", orderCaptor.getValue().getRef());
    }

    @Test
    void testFindMissingOrdersAddNewOrderWithOutUpdate() {
        when(ordersService.existsByUserRefAndStateIdIn(userRef)).thenReturn(true);

        CheckProcessResponse.Order remoteOrder = new CheckProcessResponse.Order();
        remoteOrder.setOrderId("123");
        remoteOrder.setMsisdn("380930000000");
        when(lifecellService.checkProcess(transactionId, userRef)).thenReturn(List.of(remoteOrder));

        when(ordersService.getLatestByUserRefForSyncMissing(userRef)).thenReturn(List.of());
        when(ordersService.getAllActiveByUserRefAndStateId(eq(userRef), any())).thenReturn(new PageImpl<>(List.of()));

        CheckOrderResponse checkResponse = new CheckOrderResponse();
        checkResponse.setResultCode(ResultCode.SUCCESS);
        checkResponse.setOrderState(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD);
        checkResponse.setOrderCreateDate(OffsetDateTime.now());
        when(lifecellService.checkOrder(transactionId, userRef, "123", "380930000000")).thenReturn(checkResponse);

        supportService.refreshOrdersByUserRef(transactionId, userRef);

        ArgumentCaptor<EOrder> orderCaptor = ArgumentCaptor.forClass(EOrder.class);
        verify(ordersService, atLeastOnce()).save(orderCaptor.capture());
        assertEquals("123", orderCaptor.getValue().getRef());
    }
}