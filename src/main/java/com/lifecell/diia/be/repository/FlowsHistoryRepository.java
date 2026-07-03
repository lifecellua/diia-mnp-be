package com.lifecell.diia.be.repository;

import com.lifecell.diia.be.model.db.EFlowHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FlowsHistoryRepository extends CrudRepository<EFlowHistory, UUID> {
}
