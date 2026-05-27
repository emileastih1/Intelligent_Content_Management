package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.command;

import com.ea.architecture.domain.driven.domain.document.mapper.DocumentIndexMapper;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.model.DocumentFileCommand;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainJpaServicePort;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import com.ea.architecture.domain.driven.infrastructure.repository.document.DocumentJpaRepository;
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
        //Create the command to index the document
        DocumentFileCommand documentFileCommand = new DocumentFileCommand(
                0L,
                StringUtils.EMPTY,
                documentAggregate.getDocumentName(),
                documentAggregate.getDocumentType().toString(),
                documentAggregate.getFile(),
                null
        );
        //Create the indexation event by applying the command to the aggregate
        documentAggregate.indexDocument(documentFileCommand);
        //Save the document to the vector store (event registered on aggregate)
        documentAggregate.sendDocumentToEventStore(documentFileCommand);

        // Persist JPA entity mapped from domain aggregate
        DocumentEntity entity = documentInfrastructureMapper.domainToJpaEntity(documentAggregate);
        long id = documentJpaRepository.save(entity).getId();

        // Explicitly publish domain events registered on the aggregate (since we didn't save the aggregate itself)
//        documentAggregate.domainEvents().forEach(applicationEventPublisher::publishEvent);
//        documentAggregate.clearDomainEvents();

        return id;
    }
}
