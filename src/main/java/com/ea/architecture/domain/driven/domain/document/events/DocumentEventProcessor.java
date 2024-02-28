package com.ea.architecture.domain.driven.domain.document.events;

import com.ea.architecture.domain.driven.domain.document.events.event.DocumentUploadFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentEventProcessor.class);
    @EventListener
    public void processDocumentUploadFileEvent(DocumentUploadFileEvent event) {
        LOGGER.info("DocumentEventProcessor.processDocumentUploadFileEvent event: {}", event);
    }
}
