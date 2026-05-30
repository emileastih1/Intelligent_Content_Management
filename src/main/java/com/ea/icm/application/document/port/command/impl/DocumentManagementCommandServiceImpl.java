package com.ea.icm.application.document.port.command.impl;

import com.ea.icm.application.document.port.command.DocumentManagementCommandService;
import com.ea.icm.domain.document.entity.DocumentResult;
import com.ea.icm.domain.document.mapper.DocumentIndexMapper;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.repository.command.DocumentDomainElasticServicePort;
import com.ea.icm.domain.document.repository.command.DocumentDomainJpaServicePort;
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

    @Override
    public DocumentAggregate updateDocument(DocumentAggregate documentAggregate) {
        return documentDomainJpaServicePort.updateDocument(documentAggregate);
    }

    @Override
    public void deleteDocument(long id) {
        documentDomainJpaServicePort.deleteDocument(id);
    }

    @Override
    public void batchUpdate(java.util.List<Long> documentIds, java.util.List<String> tagsToAdd, String category) {
        documentDomainJpaServicePort.batchUpdate(documentIds, tagsToAdd, category);
    }
}
