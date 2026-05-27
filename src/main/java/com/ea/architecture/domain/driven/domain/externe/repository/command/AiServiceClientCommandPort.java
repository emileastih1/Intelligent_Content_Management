package com.ea.architecture.domain.driven.domain.externe.repository.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;

public interface AiServiceClientCommandPort {
    void sendToVectorStore(DocumentAggregate aggregate);
}
