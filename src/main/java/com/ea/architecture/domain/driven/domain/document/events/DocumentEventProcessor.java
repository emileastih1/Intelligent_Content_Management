package com.ea.architecture.domain.driven.domain.document.events;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.events.event.ai.DocumentSendToVectorStoreEvent;
import com.ea.architecture.domain.driven.domain.document.events.event.elastic.DocumentUploadFileEvent;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainAiServicePort;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainElasticServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DocumentEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentEventProcessor.class);
    DocumentDomainElasticServicePort documentDomainElasticServicePort;

    DocumentDomainAiServicePort documentDomainAiServicePort;

    public DocumentEventProcessor(DocumentDomainElasticServicePort documentDomainElasticServicePort, DocumentDomainAiServicePort documentDomainAiServicePort) {
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
        this.documentDomainAiServicePort = documentDomainAiServicePort;
    }

    @TransactionalEventListener
    public void processDocumentUploadFileEvent(DocumentUploadFileEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent event: {}", event);
        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent event aggregate: {}", event.getAggregate());

        DocumentAggregate aggregate = event.getAggregate();
        DocumentResult documentResult = documentDomainElasticServicePort.addOrUpdateDocument(aggregate);

        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent documentResult: {}", documentResult);
    }

    @TransactionalEventListener
    public void processDocumentSendToVectorStoreEvent(DocumentSendToVectorStoreEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentSendToVectorStoreEvent event: {}", event);
        LOGGER.info("DocumentEventProcessor.processDocumentSendToVectorStoreEvent event aggregate: {}", event.getAggregate());

        documentDomainAiServicePort.addDocumentToVectorStore(event.getAggregate());
    }
}
