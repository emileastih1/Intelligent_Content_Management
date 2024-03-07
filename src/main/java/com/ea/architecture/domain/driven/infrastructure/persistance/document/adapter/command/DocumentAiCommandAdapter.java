package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainAiServicePort;
import com.ea.architecture.domain.driven.infrastructure.repository.document.DocumentAiRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentAiCommandAdapter implements DocumentDomainAiServicePort {

    DocumentAiRepository documentAiRepository;

    public DocumentAiCommandAdapter(DocumentAiRepository documentAiRepository) {
        this.documentAiRepository = documentAiRepository;
    }

    @Override
    public void addDocumentToVectorStore(DocumentAggregate document) {
        documentAiRepository.addDocumentToVectorStore(document);
    }
}
