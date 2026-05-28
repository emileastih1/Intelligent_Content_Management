package com.ea.icm.domain.externe.repository.command;

import com.ea.icm.domain.document.model.DocumentAggregate;

public interface AiServiceClientCommandPort {
    void sendToVectorStore(DocumentAggregate aggregate);
}
