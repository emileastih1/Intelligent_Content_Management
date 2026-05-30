package com.ea.icm.infrastructure.persistance.document.adapter.command;

import com.ea.icm.domain.document.mapper.DocumentIndexMapper;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.events.event.ai.DocumentClassifySentimentEvent;
import com.ea.icm.domain.document.events.event.ai.DocumentDeleteFromVectorStoreEvent;
import com.ea.icm.domain.document.events.event.ai.DocumentEmbedContentEvent;
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

        // Content-first embedding: always embed via TEXT_CONTENT, not raw file (ADR-0004)
        String contentToEmbed = documentAggregate.getContent();
        if (contentToEmbed != null && !contentToEmbed.isBlank()) {
            applicationEventPublisher.publishEvent(
                    new DocumentEmbedContentEvent(id, documentAggregate.getDocumentName(), contentToEmbed));
            // Async sentiment classification (ADR-0007)
            applicationEventPublisher.publishEvent(
                    new DocumentClassifySentimentEvent(id, contentToEmbed));
        }

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

        DocumentAggregate updated = documentInfrastructureMapper.jpaEntityToDomain(saved);

        if (documentAggregate.getContent() != null) {
            String newContent = documentAggregate.getContent();
            // Re-index in Elasticsearch (ADR-0004)
            applicationEventPublisher.publishEvent(new DocumentUploadFileEvent(updated, null));
            // Delete old chunks then re-embed with new content (ADR-0007)
            applicationEventPublisher.publishEvent(new DocumentDeleteFromVectorStoreEvent(id));
            applicationEventPublisher.publishEvent(
                    new DocumentEmbedContentEvent(id, updated.getDocumentName(), newContent));
            // Re-classify sentiment for updated content (ADR-0007)
            applicationEventPublisher.publishEvent(
                    new DocumentClassifySentimentEvent(id, newContent));
        }

        return updated;
    }

    @Override
    public void batchUpdate(java.util.List<Long> documentIds, java.util.List<String> tagsToAdd, String category) {
        for (Long id : documentIds) {
            documentJpaRepository.findById(id).ifPresent(entity -> {
                if (tagsToAdd != null && !tagsToAdd.isEmpty()) {
                    java.util.List<String> existing = entity.getTags() == null || entity.getTags().isBlank()
                            ? new java.util.ArrayList<>()
                            : new java.util.ArrayList<>(java.util.Arrays.asList(entity.getTags().split(",")));
                    for (String t : tagsToAdd) {
                        String trimmed = t.trim();
                        if (!trimmed.isEmpty() && !existing.contains(trimmed)) {
                            existing.add(trimmed);
                        }
                    }
                    entity.setTags(String.join(",", existing));
                }
                if (category != null) {
                    entity.setCategory(category);
                }
                entity.setModificationDate(java.time.ZonedDateTime.now());
                documentJpaRepository.save(entity);
            });
        }
    }

    @Override
    public void deleteDocument(long id) {
        if (!documentJpaRepository.existsById(id)) {
            throw new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND, "Document not found: " + id);
        }
        documentJpaRepository.deleteById(id);
        documentDomainElasticServicePort.deleteDocumentById(id);
        // Purge vector-store chunks for this document (ADR-0007)
        applicationEventPublisher.publishEvent(new DocumentDeleteFromVectorStoreEvent(id));
    }
}
