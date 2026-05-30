package com.ea.icm.infrastructure.persistance.document.adapter.query;

import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.repository.query.DocumentDomainQueryServicePort;
import com.ea.icm.domain.exception.FunctionalException;
import com.ea.icm.domain.exception.MessageCode;
import com.ea.icm.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.icm.infrastructure.persistance.document.model.DocumentElasticEntity;
import com.ea.icm.infrastructure.repository.document.DocumentESConnectorRepository;
import com.ea.icm.infrastructure.repository.document.DocumentJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentElasticSearchQueryAdapter implements DocumentDomainQueryServicePort {

    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentElasticSearchQueryAdapter.class);

    DocumentESConnectorRepository documentESConnectorRepository;
    DocumentInfrastructureMapper documentInfrastructureMapper;
    DocumentJpaRepository documentJpaRepository;


    public DocumentElasticSearchQueryAdapter(DocumentESConnectorRepository documentESConnectorRepository,
                                             DocumentInfrastructureMapper documentInfrastructureMapper,
                                             DocumentJpaRepository documentJpaRepository) {
        this.documentESConnectorRepository = documentESConnectorRepository;
        this.documentInfrastructureMapper = documentInfrastructureMapper;
        this.documentJpaRepository = documentJpaRepository;
    }

    @Override
    public List<DocumentAggregate> list() {
        // Content-first Documents are the source of truth in PostgreSQL (incl. editable text content),
        // so listing reads from JPA rather than the Elasticsearch projection.
        return documentJpaRepository.findAll().stream()
                .map(documentInfrastructureMapper::jpaEntityToDomain)
                .toList();
    }

    @Override
    public DocumentAggregate retrieveDocumentById(String documentId) {
        // Content-first Documents are the source of truth in Postgres (ADR-0004).
        // Try JPA first (gives us textContent); fall back to Elasticsearch for legacy elastic-id lookups.
        try {
            long jpaId = Long.parseLong(documentId);
            return documentJpaRepository.findById(jpaId)
                    .map(documentInfrastructureMapper::jpaEntityToDomain)
                    .orElseThrow(() -> new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND,
                            "Document not found: " + documentId));
        } catch (NumberFormatException ignored) {
            // Not a numeric JPA id — try Elasticsearch (elastic string id)
        }
        try {
            DocumentElasticEntity documentElasticEntity = documentESConnectorRepository
                    .getDocumentById(documentId);
            return documentInfrastructureMapper.entityToDomain(documentElasticEntity);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving document by ID: {}", e.getMessage());
            throw new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND, "Document not found: " + e.getMessage());
        }
    }

    @Override
    public DocumentAggregate retrieveDocumentByName(String documentName) {
        return null;
    }

    @Override
    public DocumentAggregate retrieveDocumentByFilter(DocumentAggregate documentAggregate) {
        return null;
    }

    @Override
    public DocumentAggregate extractDocumentByName(String documentName) {
        return null;
    }

    @Override
    public DocumentAggregate extractDocumentByFilter(DocumentAggregate documentAggregate) {
        return null;
    }
}
