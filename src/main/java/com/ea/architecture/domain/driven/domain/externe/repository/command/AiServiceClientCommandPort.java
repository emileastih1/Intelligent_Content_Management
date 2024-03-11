package com.ea.architecture.domain.driven.domain.externe.repository.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;

@Service
public interface AiServiceClientCommandPort {
    void sendToVectorStore(DocumentAggregate aggregate);
}
