package com.ea.architecture.domain.driven.domain.document.events;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.events.event.DocumentUploadFileEvent;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainElasticServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DocumentEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentEventProcessor.class);
    DocumentDomainElasticServicePort documentDomainElasticServicePort;

    public DocumentEventProcessor(DocumentDomainElasticServicePort documentDomainElasticServicePort) {
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
    }

    @TransactionalEventListener
    public void processDocumentUploadFileEvent(DocumentUploadFileEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent event: {}", event);
        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent event aggregate: {}", event.getAggregate());

        DocumentAggregate aggregate = event.getAggregate();
        DocumentResult documentResult = documentDomainElasticServicePort.addOrUpdateDocument(aggregate);

        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent documentResult: {}", documentResult);
    }
}
