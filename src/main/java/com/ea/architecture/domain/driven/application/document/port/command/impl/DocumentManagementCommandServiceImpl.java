package com.ea.architecture.domain.driven.application.document.port.command.impl;

import com.ea.architecture.domain.driven.application.document.port.command.DocumentManagementCommandService;
import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.events.event.DocumentUploadFileEvent;
import com.ea.architecture.domain.driven.domain.document.mapper.DocumentUploadMapper;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.model.DocumentUploadCommand;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainElasticServicePort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DocumentManagementCommandServiceImpl implements DocumentManagementCommandService {
    DocumentDomainElasticServicePort documentDomainElasticServicePort;
    DocumentUploadMapper documentUploadMapper;
    /**
     * note that here we are not storing the events in an event store, we are just publishing the events
     */
    ApplicationEventPublisher applicationEventPublisher;

    public DocumentManagementCommandServiceImpl(DocumentDomainElasticServicePort documentDomainElasticServicePort, DocumentUploadMapper documentUploadMapper, ApplicationEventPublisher applicationEventPublisher) {
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
        this.documentUploadMapper = documentUploadMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public DocumentResult addOrUpdateDocument(DocumentAggregate documentAggregate) {
        DocumentResult documentResult = documentDomainElasticServicePort.addOrUpdateDocument(documentAggregate);
        documentAggregate.updateState(documentResult);
        DocumentUploadCommand documentUploadCommand = documentUploadMapper.domainToCommand(documentAggregate);
        DocumentUploadFileEvent documentUploadFileEvent = documentAggregate.attachDocument(documentUploadCommand);
        //Manually publish an event
        applicationEventPublisher.publishEvent(documentUploadFileEvent);
        return documentResult;
    }
}
