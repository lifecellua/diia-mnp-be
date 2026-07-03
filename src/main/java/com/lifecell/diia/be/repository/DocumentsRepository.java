package com.lifecell.diia.be.repository;

import com.lifecell.diia.be.model.db.EDocument;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentsRepository extends CrudRepository<EDocument, UUID> {
    Optional<EDocument> findTopByRefOrderByCreatedAtDesc(String ref);
}
