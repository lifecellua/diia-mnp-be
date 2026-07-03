package com.lifecell.diia.be.service;

import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.model.db.EOrder;
import com.lifecell.diia.be.model.db.EOrderHistory;
import com.lifecell.diia.be.model.fsm.State;
import com.lifecell.diia.be.repository.OrdersHistoryRepository;
import com.lifecell.diia.be.repository.OrdersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrdersService {
    private final OrdersRepository ordersRepository;
    private final OrdersHistoryRepository ordersHistoryRepository;

    private final Duration ORDERS_TIMEOUT;
    private final Duration ORDERS_MISSING_TIMEOUT;

    public OrdersService(
            Properties properties,
            OrdersRepository ordersRepository,
            OrdersHistoryRepository ordersHistoryRepository) {
        this.ordersRepository = ordersRepository;
        this.ordersHistoryRepository = ordersHistoryRepository;

        this.ORDERS_TIMEOUT = properties.getService().getOrders().getTimeout();
        this.ORDERS_MISSING_TIMEOUT = properties.getService().getOrders().getMissingTimeout();
    }

    public EOrder create() {
        var dst =  new EOrder();
        return dst;
    }

    @Transactional
    public EOrder getById(UUID id) {
        var entity = ordersRepository.findById(id);
        return entity.orElse(null);
    }

    @Transactional
    public List<EOrder> getLatestByUserRef(String userRef) {
        var fromAt = OffsetDateTime.now().minus(ORDERS_TIMEOUT);
        return Streamable.of(ordersRepository.getLatestByUserRef(userRef, fromAt)).toList();
    }

    public List<EOrder> getLatestByUserRefForSyncMissing(String userRef) {
        var fromAt = OffsetDateTime.now().minus(ORDERS_MISSING_TIMEOUT);
        return ordersRepository.findAllByUserRefAndCreatedAtGreaterThanEqual(userRef, fromAt);
    }

    public boolean existsByUserRefAndStateIdIn(String userRef) {
        var statsIds = List.of(State.PROGRESS.getId(), State.UNKNOWN.getId(), State.FAIL.getId());
        var fromAt = OffsetDateTime.now().minus(ORDERS_MISSING_TIMEOUT);
        return ordersRepository.existsByUserRefAndStateIdInAndCreatedAtGreaterThanEqual(userRef, statsIds, fromAt);
    }

    @Transactional
    public Page<EOrder> getAllActiveByStateId(Pageable pageable) {
        var statsIds = List.of(State.PROGRESS.getId(), State.UNKNOWN.getId());
        var fromAt = OffsetDateTime.now().minus(ORDERS_TIMEOUT);
        return ordersRepository.findAllByStateIdInAndRefIsNotNullAndCreatedAtGreaterThanEqual(statsIds, fromAt, pageable);
    }

    @Transactional
    public Page<EOrder> getAllActiveByUserRefAndStateId(String userRef, Pageable pageable) {
        var statsIds = List.of(State.PROGRESS.getId(), State.UNKNOWN.getId());
        var fromAt = OffsetDateTime.now().minus(ORDERS_TIMEOUT);
        return ordersRepository.findAllByUserRefAndStateIdInAndRefIsNotNullAndCreatedAtGreaterThanEqual(userRef, statsIds, fromAt, pageable);
    }

    @Transactional
    public Optional<EOrder> getByRef(String ref) {
        return ordersRepository.findTopByRef(ref);
    }

    @Transactional
    public Optional<EOrder> getByRefAndUserRef(String ref, String userRef) {
        return ordersRepository.findTopByRefAndUserRef(ref, userRef);
    }

    @Transactional
    public void save(EOrder order) {
        var now = OffsetDateTime.now();
        if (order.getId() == null) {
            order.setCreatedAt(now);
        }
        order.setModifiedAt(now);
        ordersRepository.save(order);
        var history = map(order);
        ordersHistoryRepository.save(history);
    }

    private EOrderHistory map(EOrder src) {
        var dst = new EOrderHistory();
        dst.setOrder(AggregateReference.to(src.getId()));
        dst.setModifiedAt(src.getModifiedAt());
        dst.setUserRef(src.getUserRef());
        dst.setRef(src.getRef());
        dst.setCreatedOn(src.getCreatedOn());
        dst.setDueOn(src.getDueOn());
        dst.setMsisdn(src.getMsisdn());
        dst.setStateId(src.getStateId());
        dst.setStageRef(src.getStageRef());
        dst.setCancelable(src.getCancelable());
        dst.setUpdatable(src.getUpdatable());
        dst.setErrorCode(src.getErrorCode());
        dst.setErrorRef(src.getErrorRef());
        dst.setErrorInfo(src.getErrorInfo());
        dst.setTariffRef(src.getTariffRef());
        dst.setTariffName(src.getTariffName());
        return dst;
    }
}
