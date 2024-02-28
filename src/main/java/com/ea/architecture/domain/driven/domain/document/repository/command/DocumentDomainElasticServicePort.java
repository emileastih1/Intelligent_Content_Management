package com.ea.architecture.domain.driven.domain.document.repository.command;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;

@Service
public interface DocumentDomainElasticServicePort {
    DocumentResult addOrUpdateDocument(DocumentAggregate document);
    DocumentAggregate duplicateDocument(DocumentAggregate document);
    DocumentAggregate deleteDocument(DocumentAggregate document);

}
