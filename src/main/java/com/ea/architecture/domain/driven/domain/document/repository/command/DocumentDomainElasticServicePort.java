package com.ea.architecture.domain.driven.domain.document.repository.command;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;

public interface DocumentDomainElasticServicePort {
    DocumentResult addOrUpdateDocument(DocumentAggregate document);

    DocumentAggregate duplicateDocument(DocumentAggregate document);

    DocumentAggregate deleteDocument(DocumentAggregate document);

}
