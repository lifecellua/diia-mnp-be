package com.lifecell.diia.be.service;

import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.model.db.EOrder;
import com.lifecell.diia.be.model.dto.ApplicationStatus;
import com.lifecell.diia.be.model.dto.lifecell.response.CheckOrderResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.CheckProcessResponse;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.model.fsm.State;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SupportService {
    private final DiiaService diiaService;
    private final DocumentsService documentsService;
    private final LifecellService lifecellService;
    private final OrdersService ordersService;

    private final Long REFRESH_ORDERS_ACTIVE_HOURS_LIMIT;
    private final Integer ORDERS_MAX_SIZE;
    private final String DONOR_ACCEPT_WAIT_SIM_CARD;
    private final String DONOR_ACCEPT;
    private final String ACTIVATED_MSISDN;
    private final String BROADCAST;

    public SupportService(
            Properties properties,
            DiiaService diiaService,
            DocumentsService documentsService,
            LifecellService lifecellService,
            OrdersService ordersService) {
        this.diiaService = diiaService;
        this.documentsService = documentsService;
        this.lifecellService = lifecellService;
        this.ordersService = ordersService;

        this.REFRESH_ORDERS_ACTIVE_HOURS_LIMIT = properties.getService().getOrders().getRefreshOrdersActiveHoursLimit();
        this.ORDERS_MAX_SIZE = properties.getService().getOrders().getMaxSize();
        this.DONOR_ACCEPT_WAIT_SIM_CARD = properties.getService().getPushNotifications().getDonorAcceptWaitSimCard();
        this.DONOR_ACCEPT = properties.getService().getPushNotifications().getDonorAccept();
        this.ACTIVATED_MSISDN = properties.getService().getPushNotifications().getActivatedMsisdn();
        this.BROADCAST = properties.getService().getPushNotifications().getBroadcast();
    }

    public void refreshOrderByRef(String transactionId, String ref) {
        log.info("SupportService.refreshOrderByRef() ref={}", ref);
        ordersService.getByRef(ref).ifPresent(order -> {
            var checkOrderResponse = lifecellService.checkOrder(transactionId, order.getUserRef(), order.getRef(), order.getMsisdn());
            if (Objects.nonNull(checkOrderResponse) && checkOrderResponse.getResultCode() == ResultCode.SUCCESS) {
                updateAndNotifyOrder(
                    order,
                    checkOrderResponse.getOrderState(),
                    checkOrderResponse.getTariffCode(),
                    checkOrderResponse.getTariffUa(),
                    checkOrderResponse.getCancelPossibility(),
                    checkOrderResponse.getOrderCreateDate(),
                    checkOrderResponse.getDueDate(),
                    checkOrderResponse.getRejectCode(),
                    false
                );
            }
        });
    }

    public void refreshOrdersByUserRef(String transactionId, String userRef) {
        log.info("SupportService.refreshOrdersByUserRef() userRef={}", userRef);
        findMissingOrders(transactionId, userRef);
        prepareOrdersForUpdate(transactionId, userRef);
    }

    public void refreshOrdersActive(String transactionId) {
        log.info("SupportService.refreshOrdersActive()");
        prepareOrdersForUpdate(transactionId, ordersService::getAllActiveByStateId);
    }

    public void prepareOrdersForUpdate(String transactionId, String userRef) {
        log.info("SupportService.prepareOrdersForUpdate() userRef={}", userRef);
        prepareOrdersForUpdate(transactionId, pageable -> ordersService.getAllActiveByUserRefAndStateId(userRef, pageable));
    }

    private void prepareOrdersForUpdate(String transactionId, Function<Pageable, Page<EOrder>> fetchPage){
        int size = ORDERS_MAX_SIZE;
        var deadline = OffsetDateTime.now().plusHours(REFRESH_ORDERS_ACTIVE_HOURS_LIMIT);
        int pageNumber = 0;
        var sort = Sort.by(Sort.Direction.ASC, "createdAt");

        Page<EOrder> page;
        do {
            if (OffsetDateTime.now().isAfter(deadline)) {
                log.warn("SupportService.prepareOrdersForUpdate() stopped by timeout ({} hours limit)", REFRESH_ORDERS_ACTIVE_HOURS_LIMIT);
                break;
            }

            Pageable pageable = PageRequest.of(pageNumber, size, sort);
            page = fetchPage.apply(pageable);
            try {
                List<EOrder> orders = page.getContent();
                if (orders.size() == 1) {
                    refreshOrderByRef(transactionId, orders.get(0).getRef());
                } else {
                    syncOrders(transactionId, orders);
                }
            }catch (Exception e){
                log.error("SupportService.prepareOrdersForUpdate() error - {}{}",e, e.getMessage());
            }
            pageNumber++;
        } while (page.hasNext());
    }


    public void callbackFile(String applicationId, String documentId, String documentData, String documentName) {
        try {
            if ((documentId != null) && (!documentId.isBlank())) {
                var entity = documentsService.getByRef(documentId);
                if (entity != null) {
                    entity.setStateId(State.SUCCESS.getId());
                    documentsService.save(entity);
                }
            }
        } catch (Exception e) {
            log.error("File callback error: {}",e.getMessage(), e);
        }
    }

    public void callbackOrder(ApplicationStatus applicationStatus) {
        log.info("SupportService.callbackOrder() applicationStatus={}", applicationStatus);
        ordersService.getByRef(applicationStatus.getApplicationId())
            .ifPresent(currentOrder -> updateAndNotifyOrder(
                currentOrder,
                applicationStatus.getStatus(),
                applicationStatus.getApplicationData().getTariffCode(),
                applicationStatus.getApplicationData().getTariffUa(),
                applicationStatus.getApplicationData().getCancelPossibility(),
                applicationStatus.getApplicationData().getOrderCreateDate(),
                applicationStatus.getApplicationData().getDueDate(),
                null,
                true
            ));
    }

    private void findMissingOrders(String transactionId, String userRef){
        log.info("SupportService.findMissingOrders() Start transactionId={} userRef={}", transactionId, userRef);
        if (ordersService.existsByUserRefAndStateIdIn(userRef)) {
            try {
                var processOrders = lifecellService.checkProcess(transactionId, userRef);
                var existingOrders = ordersService.getLatestByUserRefForSyncMissing(userRef);
                var existingOrderRefs = existingOrders.stream()
                        .map(EOrder::getRef)
                        .collect(Collectors.toSet());

                processOrders.stream()
                    .filter(order -> !existingOrderRefs.contains(order.getOrderId()))
                    .forEach(missingOrder -> {
                        try {
                            addMissingOrder(transactionId, userRef, missingOrder.getOrderId(), missingOrder.getMsisdn());
                        } catch (Exception e) {
                            log.error("SupportService.findMissingOrders() Failed to add order: {} - {}",
                                    missingOrder.getOrderId(), e.getMessage());
                        }
                    });

                var processOrdersRefs = processOrders.stream()
                        .map(CheckProcessResponse.Order::getOrderId)
                        .collect(Collectors.toSet());

                existingOrders.stream()
                    .filter(existingOrder -> !processOrdersRefs.contains(existingOrder.getRef()))
                    .forEach(excessive -> {
                        try {
                            excessiveOrder(transactionId, userRef, excessive.getRef());
                        } catch (Exception e) {
                            log.error("SupportService.findMissingOrders() Failed to remove order: {} - {}",
                                    excessive.getId(), e.getMessage());
                        }
                    });
            } catch (Exception e) {
                log.error("SupportService.findMissingOrders() error transactionId={} userRef={} - {} {}", transactionId, userRef, e, e.getMessage());
            }
        } else {
            log.info("SupportService.findMissingOrders() The user has no active orders, transactionId={} userRef={}", transactionId, userRef);
        }
        log.info("SupportService.findMissingOrders() Finish transactionId={} userRef={}", transactionId, userRef);
    }

    private void addMissingOrder(String transactionId, String userRef, String orderId, String msisdn){
        log.info("SupportService.addMissingOrder() transactionId={}, userRef={}, orderId={}, msisdn={}", transactionId, userRef, orderId, msisdn);
        var missingOrder = lifecellService.checkOrder(transactionId, userRef, orderId, msisdn);
        if (Objects.nonNull(missingOrder) && missingOrder.getResultCode() == ResultCode.SUCCESS) {
            var entity = new EOrder();
            entity.setCreatedAt(missingOrder.getOrderCreateDate());
            entity.setUserRef(userRef);
            entity.setRef(orderId);
            entity.setCreatedOn(missingOrder.getOrderCreateDate());
            entity.setDueOn(missingOrder.getDueDate());
            entity.setMsisdn(msisdn);
            entity.setStateId(missingOrder.getOrderState().getInfo().getState().getId());
            entity.setStageRef(missingOrder.getOrderState().getRef());
            entity.setCancelable(missingOrder.getCancelPossibility());
            entity.setUpdatable(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD == missingOrder.getOrderState());
            entity.setTariffRef(missingOrder.getTariffCode());
            entity.setTariffName(missingOrder.getTariffUa());
            ordersService.save(entity);
        }
    }

    private void excessiveOrder(String transactionId, String userRef, String orderId){
        log.info("SupportService.excessiveOrder() transactionId={}, userRef={}, orderId={}", transactionId, userRef, orderId);
        var excessiveOrder = lifecellService.checkOrder(transactionId, userRef, orderId, "");
        if (Objects.nonNull(excessiveOrder) && excessiveOrder.getResultCode() != ResultCode.SUCCESS) {
            ordersService.getByRefAndUserRef(orderId, userRef).ifPresent(order -> {
                order.setStateId(State.UNKNOWN.getId());
                ordersService.save(order);
            });
        }
    }

    private void syncOrders(String transactionId, List<EOrder> orders) {
        if (orders.size() > 1) {
            var refs = orders.stream()
                .map(EOrder::getRef)
                .toList();
            log.info("SupportService.syncOrders() refs={}", refs);
            var currentOrders = lifecellService.checkOrders(transactionId, refs);

            if (Objects.nonNull(currentOrders)) {
                Map<String, CheckOrderResponse> currentOrdersMap = currentOrders.stream()
                    .collect(Collectors.toMap(CheckOrderResponse::getOrderId, Function.identity()));

                orders.forEach(order ->
                    Optional.ofNullable(currentOrdersMap.get(order.getRef()))
                        .ifPresent(currentOrder -> updateAndNotifyOrder(
                            order,
                            currentOrder.getOrderState(),
                            currentOrder.getTariffCode(),
                            currentOrder.getTariffUa(),
                            currentOrder.getCancelPossibility(),
                            currentOrder.getOrderCreateDate(),
                            currentOrder.getDueDate(),
                            currentOrder.getRejectCode(),
                            false
                        )));
            }
        }

    }

    private void updateAndNotifyOrder(EOrder order, OrderState orderState, String tariffRef, String tariffName, Boolean cancelable, OffsetDateTime createdOn, OffsetDateTime cancelableDueOn, String rejectCode, Boolean isCallback) {
        log.info("SupportService.updateAndNotifyOrder() order={}, orderState={}, tariffRef={}, tariffName={}", order, orderState, tariffRef, tariffName);

        if(OrderState.fromRef(order.getStageRef()) != orderState && isCallback) {
            notifyOrder(order, orderState);
        }

        if (createdOn != null) { order.setCreatedOn(createdOn); }
        if (cancelableDueOn != null) { order.setDueOn(cancelableDueOn); }
        order.setStateId(orderState.getInfo().getState().getId());
        order.setStageRef(orderState.getRef());
        order.setUpdatable(OrderState.DONOR_ACCEPT_WAIT_SIM_CARD == orderState);
        order.setCancelable(Boolean.TRUE.equals(cancelable));
        if (tariffRef != null)  { order.setTariffRef(tariffRef); }
        if (tariffName != null) { order.setTariffName(tariffName); }
        if (rejectCode != null)  { order.setErrorInfo(rejectCode); }
        ordersService.save(order);
    }

    @Scheduled(cron = "${com.lifecell.diia.be.service.orders.refresh-orders-active-cron}")
    @SchedulerLock(
            name = "RefreshOrdersActive",
            lockAtLeastFor = "${com.lifecell.diia.be.service.orders.refresh-orders-active-lock-at-least-for}",
            lockAtMostFor = "${com.lifecell.diia.be.service.orders.refresh-orders-active-lock-at-most-for}"
    )
    public void task() {
        log.info("Refresh orders");
        refreshOrdersActive(UUID.randomUUID().toString());
    }

    private void notifyOrder(EOrder order, OrderState state) {
        log.info("SupportService.notifyOrder() orderId={}, stateCode={}", order.getId(), state.getRef());
        if (OrderState.DONOR_ACCEPT_WAIT_SIM_CARD == state) {
            log.info("SupportService.notifyOrder() SEND PUSH DONOR_ACCEPT_WAIT_SIM_CARD orderId={}, stateCode={}", order.getId(), state.getRef());
            diiaService.notify(order.getUserRef(), DONOR_ACCEPT_WAIT_SIM_CARD, order.getId().toString());
        } else if (OrderState.DONOR_ACCEPT == state) {
            log.info("SupportService.notifyOrder() SEND PUSH DONOR_ACCEPT orderId={}, stateCode={}", order.getId(), state.getRef());
            diiaService.notify(order.getUserRef(), DONOR_ACCEPT, order.getId().toString());
        } else if (OrderState.ACTIVATED_MSISDN == state) {
            log.info("SupportService.notifyOrder() SEND PUSH ACTIVATED_MSISDN orderId={}, stateCode={}", order.getId(), state.getRef());
            diiaService.notify(order.getUserRef(), ACTIVATED_MSISDN, order.getId().toString());
        } else if (OrderState.BROADCAST == state) {
            log.info("SupportService.notifyOrder() SEND PUSH BROADCAST orderId={}, stateCode={}", order.getId(), state.getRef());
            diiaService.notify(order.getUserRef(), BROADCAST, order.getId().toString());
        }
    }
}
