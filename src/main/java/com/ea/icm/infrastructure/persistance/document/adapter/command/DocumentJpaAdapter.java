package com.ea.icm.infrastructure.persistance.document.adapter.command;

import com.ea.icm.domain.document.mapper.DocumentIndexMapper;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.events.event.elastic.DocumentUploadFileEvent;
import com.ea.icm.domain.document.model.DocumentFileCommand;
import com.ea.icm.domain.document.repository.command.DocumentDomainElasticServicePort;
import com.ea.icm.domain.document.repository.command.DocumentDomainJpaServicePort;
import com.ea.icm.domain.exception.FunctionalException;
import com.ea.icm.domain.exception.MessageCode;
import com.ea.icm.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.icm.infrastructure.persistance.document.adapter.modules.extractor.DocumentExtractor;
import com.ea.icm.infrastructure.persistance.document.model.DocumentEntity;
import com.ea.icm.infrastructure.repository.document.DocumentJpaRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class DocumentJpaAdapter implements DocumentDomainJpaServicePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentJpaAdapter.class);

    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentInfrastructureMapper documentInfrastructureMapper;
    private final DocumentIndexMapper documentUploadMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DocumentDomainElasticServicePort documentDomainElasticServicePort;
    private final DocumentExtractor documentExtractor;

    public DocumentJpaAdapter(DocumentInfrastructureMapper documentInfrastructureMapper,
                              DocumentIndexMapper documentUploadMapper,
                              DocumentJpaRepository documentJpaRepository,
                              ApplicationEventPublisher applicationEventPublisher,
                              DocumentDomainElasticServicePort documentDomainElasticServicePort,
                              DocumentExtractor documentExtractor) {
        this.documentInfrastructureMapper = documentInfrastructureMapper;
        this.documentUploadMapper = documentUploadMapper;
        this.documentJpaRepository = documentJpaRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
        this.documentExtractor = documentExtractor;
    }

    @Override
    public long addDocument(DocumentAggregate documentAggregate) {
        // Upload ingestion path: extract text from file into content (ADR-0004),
        // then fire index + vector-store events. Authoring path has no file, skip events.
        if (documentAggregate.getFile() != null) {
            // Extract text into TEXT_CONTENT so the document is content-first
            if (documentAggregate.getContent() == null) {
                try {
                    String extracted = documentExtractor.extract(documentAggregate.getFile()).getLeft();
                    if (extracted != null && !extracted.isBlank()) {
                        documentAggregate.setContent(extracted.trim());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Tika extraction failed for '{}', storing without text content: {}",
                            documentAggregate.getDocumentName(), e.getMessage());
                }
            }

            DocumentFileCommand documentFileCommand = new DocumentFileCommand(
                    0L,
                    StringUtils.EMPTY,
                    documentAggregate.getDocumentName(),
                    documentAggregate.getDocumentType() != null ? documentAggregate.getDocumentType().toString() : null,
                    documentAggregate.getFile(),
                    null
            );
            documentAggregate.indexDocument(documentFileCommand);
            documentAggregate.sendDocumentToEventStore(documentFileCommand);
        }

        // Persist JPA entity mapped from domain aggregate
        DocumentEntity entity = documentInfrastructureMapper.domainToJpaEntity(documentAggregate);
        long id = documentJpaRepository.save(entity).getId();

        documentAggregate.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);

        return id;
    }

    @Override
    public DocumentAggregate updateDocument(DocumentAggregate documentAggregate) {
        long id = documentAggregate.getId();
        DocumentEntity existing = documentJpaRepository.findById(id)
                .orElseThrow(() -> new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND,
                        "Document not found: " + id));

        if (documentAggregate.getDocumentName() != null) {
            existing.setName(documentAggregate.getDocumentName());
        }
        if (documentAggregate.getContent() != null) {
            existing.setTextContent(documentAggregate.getContent());
        }
        if (documentAggregate.getTags() != null) {
            existing.setTags(documentAggregate.getTags());
        }
        if (documentAggregate.getCategory() != null) {
            existing.setCategory(documentAggregate.getCategory());
        }
        existing.setModificationDate(ZonedDateTime.now());

        DocumentEntity saved = documentJpaRepository.save(existing);

        // Re-index in Elasticsearch if content changed (ADR-0004)
        DocumentAggregate updated = documentInfrastructureMapper.jpaEntityToDomain(saved);
        if (documentAggregate.getContent() != null) {
            DocumentUploadFileEvent event = new DocumentUploadFileEvent(updated, null);
            applicationEventPublisher.publishEvent(event);
        }

        return updated;
    }

    @Override
    public void deleteDocument(long id) {
        if (!documentJpaRepository.existsById(id)) {
            throw new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND, "Document not found: " + id);
        }
        documentJpaRepository.deleteById(id);
        documentDomainElasticServicePort.deleteDocumentById(id);
    }
}
