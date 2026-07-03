package com.lifecell.diia.be.service;

import com.lifecell.diia.be.model.db.EFlow;
import com.lifecell.diia.be.model.db.EFlowHistory;
import com.lifecell.diia.be.repository.FlowsHistoryRepository;
import com.lifecell.diia.be.repository.FlowsRepository;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class FlowsService {
    private final FlowsRepository flowsRepository;
    private final FlowsHistoryRepository flowsHistoryRepository;

    public FlowsService(
            FlowsRepository flowsRepository,
            FlowsHistoryRepository flowsHistoryRepository) {
        this.flowsRepository = flowsRepository;
        this.flowsHistoryRepository = flowsHistoryRepository;
    }

    public EFlow create() {
        var dst = new EFlow();
        return dst;
    }

    @Transactional
    public EFlow getById(UUID id) {
        var entity = flowsRepository.findById(id);
        return entity.orElse(null);
    }

    @Transactional
    public EFlow getActiveByUserRef(String userRef) {
        var entity = flowsRepository.getActiveByUserRef(userRef);
        return entity.orElse(null);
    }

    @Transactional
    public void save(EFlow flow) {
        var now = OffsetDateTime.now();
        if (flow.getId() == null) {
            flow.setCreatedAt(now);
        }
        flow.setModifiedAt(now);
        flowsRepository.save(flow);
        var history = map(flow);
        flowsHistoryRepository.save(history);
    }

    private EFlowHistory map(EFlow src) {
        var dst = new EFlowHistory();
        dst.setFlow(AggregateReference.to(src.getId()));
        dst.setModifiedAt(src.getModifiedAt());
        dst.setUserRef(src.getUserRef());
        dst.setStateId(src.getStateId());
        dst.setStageRef(src.getStageRef());
        dst.setErrorCode(src.getErrorCode());
        dst.setErrorRef(src.getErrorRef());
        dst.setTariffRef(src.getTariffRef());
        dst.setTariffName(src.getTariffName());
        dst.setMsisdn(src.getMsisdn());
        dst.setToken(src.getToken());
        dst.setIccid(src.getIccid());
        dst.setDocument(src.getDocument());
        dst.setOrder(src.getOrder());
        return dst;
    }
}
