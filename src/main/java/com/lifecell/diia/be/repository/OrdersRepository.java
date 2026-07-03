package com.lifecell.diia.be.repository;

import com.lifecell.diia.be.model.db.EOrder;
import com.lifecell.diia.be.model.fsm.Stage;
import com.lifecell.diia.be.model.fsm.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdersRepository extends CrudRepository<EOrder, UUID> {

    @Query("SELECT * FROM orders AS o " +
            "WHERE (o.user_ref = :user_ref) AND " +
                "((o.state_id <> -3) AND (o.state_id <> 0) AND (o.created_at >= :created_at)) " +
            "" +
            "ORDER BY o.created_at DESC")
    public Iterable<EOrder> getLatestByUserRef(@Param("user_ref") String userRef, @Param("created_at") OffsetDateTime createdAt);

    public Page<EOrder> findAllByStateIdInAndRefIsNotNullAndCreatedAtGreaterThanEqual(List<Integer> stateIds, OffsetDateTime createdAt, Pageable pageable);

    public Page<EOrder> findAllByUserRefAndStateIdInAndRefIsNotNullAndCreatedAtGreaterThanEqual(String userRef, List<Integer> stateIds, OffsetDateTime createdAt, Pageable pageable);

    public Optional<EOrder> findTopByRef(String ref);

    public Optional<EOrder> findTopByRefAndUserRef(String ref, String userRef);

    public List<EOrder> findAllByUserRefAndCreatedAtGreaterThanEqual(String userRef, OffsetDateTime createdAt);

    public boolean existsByUserRefAndStateIdInAndCreatedAtGreaterThanEqual(String userRef, List<Integer> stateIds, OffsetDateTime createdAt);
}
