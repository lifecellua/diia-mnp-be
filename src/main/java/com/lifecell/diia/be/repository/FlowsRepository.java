package com.lifecell.diia.be.repository;

import com.lifecell.diia.be.model.db.EFlow;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlowsRepository extends CrudRepository<EFlow, UUID> {
    @Query("SELECT * FROM flows AS f " +
            "WHERE f.user_ref = :user_ref AND f.state_id = 1 " +
            "ORDER BY created_at DESC " +
            "LIMIT 1")
    public Optional<EFlow> getActiveByUserRef(@Param("user_ref") String userRef);
}
