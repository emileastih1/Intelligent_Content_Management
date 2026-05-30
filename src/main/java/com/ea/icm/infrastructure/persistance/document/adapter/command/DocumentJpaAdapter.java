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
import com.ea.icm.infrastructure.persistance.document.model.DocumentEntity;
import com.ea.icm.infrastructure.repository.document.DocumentJpaRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class DocumentJpaAdapter implements DocumentDomainJpaServicePort {

    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentInfrastructureMapper documentInfrastructureMapper;
    private final DocumentIndexMapper documentUploadMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DocumentDomainElasticServicePort documentDomainElasticServicePort;

    public DocumentJpaAdapter(DocumentInfrastructureMapper documentInfrastructureMapper,
                              DocumentIndexMapper documentUploadMapper,
                              DocumentJpaRepository documentJpaRepository,
                              ApplicationEventPublisher applicationEventPublisher,
                              DocumentDomainElasticServicePort documentDomainElasticServicePort) {
        this.documentInfrastructureMapper = documentInfrastructureMapper;
        this.documentUploadMapper = documentUploadMapper;
        this.documentJpaRepository = documentJpaRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
    }

    @Override
    public long addDocument(DocumentAggregate documentAggregate) {
        // The upload ingestion path provides a file: index it and send it to the vector store.
        // The authoring ingestion path provides only text content (no file), so those
        // file-based events are skipped here (ADR-0004 content-first Document).
        if (documentAggregate.getFile() != null) {
            DocumentFileCommand documentFileCommand = new DocumentFileCommand(
                    0L,
                    StringUtils.EMPTY,
                    documentAggregate.getDocumentName(),
                    documentAggregate.getDocumentType() != null ? documentAggregate.getDocumentType().toString() : null,
                    documentAggregate.getFile(),
                    null
            );
            //Create the indexation event by applying the command to the aggregate
            documentAggregate.indexDocument(documentFileCommand);
            //Save the document to the vector store (event registered on aggregate)
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
