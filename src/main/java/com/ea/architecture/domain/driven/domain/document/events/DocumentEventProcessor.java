package com.ea.architecture.domain.driven.domain.document.events;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.events.event.ai.DocumentSendToVectorStoreEvent;
import com.ea.architecture.domain.driven.domain.document.events.event.elastic.DocumentUploadFileEvent;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainElasticServicePort;
import com.ea.architecture.domain.driven.infrastructure.persistance.external.adapter.command.DocumentAiCommandServiceClientCommandAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

@Component
public class DocumentEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentEventProcessor.class);
    DocumentDomainElasticServicePort documentDomainElasticServicePort;

    DocumentAiCommandServiceClientCommandAdapter documentAiCommandServiceAdapter;

    public DocumentEventProcessor(DocumentDomainElasticServicePort documentDomainElasticServicePort,
                                  DocumentAiCommandServiceClientCommandAdapter documentAiCommandServiceAdapter) {
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
        this.documentAiCommandServiceAdapter = documentAiCommandServiceAdapter;
    }

    @Async
    @TransactionalEventListener
    public void processDocumentUploadFileEvent(DocumentUploadFileEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent event: {}", event);
        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent event aggregate: {}", event.getAggregate());

        DocumentAggregate aggregate = event.getAggregate();
        DocumentResult documentResult = documentDomainElasticServicePort.addOrUpdateDocument(aggregate);

        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent documentResult: {}", documentResult);
    }

    @Async
    @TransactionalEventListener(condition = "#event.aggregate != null")
    public void processDocumentSendToVectorStoreEvent(DocumentSendToVectorStoreEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentSendToVectorStoreEvent event: {}", event);
        LOGGER.info("DocumentEventProcessor.processDocumentSendToVectorStoreEvent event aggregate: {}", event.getAggregate());

        Assert.notNull(event.getAggregate(), "DocumentEventProcessor.processDocumentSendToVectorStoreEvent event aggregate is null");
        documentAiCommandServiceAdapter.sendToVectorStore(event.getAggregate());
    }
}
