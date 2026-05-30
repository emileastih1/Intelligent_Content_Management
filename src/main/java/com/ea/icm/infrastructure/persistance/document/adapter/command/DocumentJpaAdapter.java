package com.ea.icm.infrastructure.persistance.document.adapter.command;

import com.ea.icm.domain.document.mapper.DocumentIndexMapper;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.model.DocumentFileCommand;
import com.ea.icm.domain.document.repository.command.DocumentDomainJpaServicePort;
import com.ea.icm.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.icm.infrastructure.persistance.document.model.DocumentEntity;
import com.ea.icm.infrastructure.repository.document.DocumentJpaRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DocumentJpaAdapter implements DocumentDomainJpaServicePort {

    private final DocumentJpaRepository documentJpaRepository;
    private final DocumentInfrastructureMapper documentInfrastructureMapper;
    private final DocumentIndexMapper documentUploadMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DocumentJpaAdapter(DocumentInfrastructureMapper documentInfrastructureMapper,
                              DocumentIndexMapper documentUploadMapper,
                              DocumentJpaRepository documentJpaRepository,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.documentInfrastructureMapper = documentInfrastructureMapper;
        this.documentUploadMapper = documentUploadMapper;
        this.documentJpaRepository = documentJpaRepository;
        this.applicationEventPublisher = applicationEventPublisher;
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
}
