package com.ea.architecture.domain.driven.application.document.port.command.impl;

import com.ea.architecture.domain.driven.application.document.port.command.DocumentManagementCommandService;
import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.mapper.DocumentIndexMapper;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainElasticServicePort;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainJpaServicePort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DocumentManagementCommandServiceImpl implements DocumentManagementCommandService {
    DocumentDomainElasticServicePort documentDomainElasticServicePort;
    DocumentIndexMapper documentIndexMapper;
    /**
     * note that here we are not storing the events in an event store, we are just publishing the events
     */
    ApplicationEventPublisher applicationEventPublisher;

    DocumentDomainJpaServicePort documentDomainJpaServicePort;

    public DocumentManagementCommandServiceImpl(DocumentDomainElasticServicePort documentDomainElasticServicePort,
                                                DocumentIndexMapper documentIndexMapper,
                                                ApplicationEventPublisher applicationEventPublisher,
                                                DocumentDomainJpaServicePort documentDomainJpaServicePort) {
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
        this.documentIndexMapper = documentIndexMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.documentDomainJpaServicePort = documentDomainJpaServicePort;
    }

    /*  @Override
      public DocumentResult addOrUpdateDocument(DocumentAggregate documentAggregate) {
          DocumentResult documentResult = documentDomainElasticServicePort.addOrUpdateDocument(documentAggregate);
          documentAggregate.updateState(documentResult);
          DocumentUploadCommand documentUploadCommand = documentUploadMapper.domainToCommand(documentAggregate);
          DocumentUploadFileEvent documentUploadFileEvent = documentAggregate.attachDocument(documentUploadCommand);
          //Manually publish an event
          applicationEventPublisher.publishEvent(documentUploadFileEvent);
          return documentResult;
      }*/
    @Override
    public DocumentResult addDocument(DocumentAggregate documentAggregate) {
        long documentId = documentDomainJpaServicePort.addDocument(documentAggregate);
        return new DocumentResult(String.valueOf(documentId));
    }
}
