package com.ea.icm.domain.document.repository.command;

import com.ea.icm.domain.document.entity.DocumentResult;
import com.ea.icm.domain.document.model.DocumentAggregate;

public interface DocumentDomainElasticServicePort {
    DocumentResult addOrUpdateDocument(DocumentAggregate document);

    DocumentAggregate duplicateDocument(DocumentAggregate document);

    DocumentAggregate deleteDocument(DocumentAggregate document);

}
