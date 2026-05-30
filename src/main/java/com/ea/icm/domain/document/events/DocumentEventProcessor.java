package com.ea.icm.domain.document.events;

import com.ea.icm.domain.document.entity.DocumentResult;
import com.ea.icm.domain.document.events.event.ai.DocumentClassifySentimentEvent;
import com.ea.icm.domain.document.events.event.ai.DocumentDeleteFromVectorStoreEvent;
import com.ea.icm.domain.document.events.event.ai.DocumentSendToVectorStoreEvent;
import com.ea.icm.domain.document.events.event.elastic.DocumentUploadFileEvent;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.repository.command.DocumentDomainElasticServicePort;
import com.ea.icm.infrastructure.persistance.document.adapter.command.DocumentJpaAdapter;
import com.ea.icm.infrastructure.persistance.external.adapter.command.DocumentAiCommandServiceClientCommandAdapter;
import com.ea.icm.infrastructure.persistance.external.adapter.query.DocumentAiQueryServiceClientCommandAdapter;
import com.ea.icm.infrastructure.repository.document.DocumentJpaRepository;
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
    DocumentAiQueryServiceClientCommandAdapter documentAiQueryServiceAdapter;
    DocumentJpaRepository documentJpaRepository;

    public DocumentEventProcessor(DocumentDomainElasticServicePort documentDomainElasticServicePort,
                                  DocumentAiCommandServiceClientCommandAdapter documentAiCommandServiceAdapter,
                                  DocumentAiQueryServiceClientCommandAdapter documentAiQueryServiceAdapter,
                                  DocumentJpaRepository documentJpaRepository) {
        this.documentDomainElasticServicePort = documentDomainElasticServicePort;
        this.documentAiCommandServiceAdapter = documentAiCommandServiceAdapter;
        this.documentAiQueryServiceAdapter = documentAiQueryServiceAdapter;
        this.documentJpaRepository = documentJpaRepository;
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
        Assert.notNull(event.getAggregate(), "event aggregate is null");
        documentAiCommandServiceAdapter.sendToVectorStore(event.getAggregate());
    }

    // Purge all chunks for a deleted document from the vector store (ADR-0007)
    @Async
    @TransactionalEventListener
    public void processDocumentDeleteFromVectorStoreEvent(DocumentDeleteFromVectorStoreEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentDeleteFromVectorStoreEvent documentId: {}", event.getDocumentId());
        try {
            documentAiCommandServiceAdapter.deleteFromVectorStore(event.getDocumentId());
        } catch (Exception e) {
            LOGGER.warn("Failed to delete vector-store chunks for documentId {}: {}", event.getDocumentId(), e.getMessage());
        }
    }

    // Classify sentiment asynchronously; persist the result (ADR-0007)
    @Async
    @TransactionalEventListener
    public void processDocumentClassifySentimentEvent(DocumentClassifySentimentEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentClassifySentimentEvent documentId: {}", event.getDocumentId());
        try {
            String sentiment = documentAiQueryServiceAdapter.classifySentiment(event.getContent());
            documentJpaRepository.findById(event.getDocumentId()).ifPresent(entity -> {
                entity.setSentiment(sentiment);
                documentJpaRepository.save(entity);
            });
        } catch (Exception e) {
            LOGGER.warn("Sentiment classification failed for documentId {}: {}", event.getDocumentId(), e.getMessage());
        }
    }
}
