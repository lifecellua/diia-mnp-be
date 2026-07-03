package com.lifecell.diia.be.repository;

import com.lifecell.diia.be.model.db.EOrderHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrdersHistoryRepository extends CrudRepository<EOrderHistory, UUID> {
}
