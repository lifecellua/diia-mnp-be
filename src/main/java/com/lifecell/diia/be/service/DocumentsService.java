package com.lifecell.diia.be.service;

import com.lifecell.diia.be.model.db.EDocument;
import com.lifecell.diia.be.repository.DocumentsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DocumentsService {
    private final DocumentsRepository documentsRepository;

    public DocumentsService(
            DocumentsRepository documentsRepository) {
        this.documentsRepository = documentsRepository;
    }

    public EDocument create() {
        var dst = new EDocument();
        return dst;
    }

    @Transactional
    public EDocument getById(UUID id) {
        var entity = documentsRepository.findById(id);
        return entity.orElse(null);
    }

    @Transactional
    public EDocument getByRef(String ref) {
        var entity = documentsRepository.findTopByRefOrderByCreatedAtDesc(ref);
        return entity.orElse(null);
    }

    @Transactional
    public void save(EDocument document) {
        var now = OffsetDateTime.now();
        if (document.getId() == null) {
            document.setCreatedAt(now);
        }
        document.setModifiedAt(now);
        documentsRepository.save(document);
    }
}
