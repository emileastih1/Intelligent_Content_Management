package com.ea.architecture.domain.driven.infrastructure.persistance.external.adapter.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.externe.repository.command.AiServiceClientCommandPort;
import com.ea.architecture.domain.driven.infrastructure.repository.external.AiServiceExternalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentAiCommandServiceClientCommandAdapter implements AiServiceClientCommandPort {

    AiServiceExternalRepository aiServiceExternalRepository;

    public DocumentAiCommandServiceClientCommandAdapter(AiServiceExternalRepository aiServiceExternalRepository) {
        this.aiServiceExternalRepository = aiServiceExternalRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToVectorStore(DocumentAggregate aggregate) {
        aiServiceExternalRepository.sendToVectorStore(aggregate);
    }
}
