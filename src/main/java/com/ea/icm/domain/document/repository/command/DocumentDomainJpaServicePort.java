package com.ea.icm.domain.document.repository.command;

import com.ea.icm.domain.document.model.DocumentAggregate;

public interface DocumentDomainJpaServicePort {
    long addDocument(DocumentAggregate document);

    DocumentAggregate updateDocument(DocumentAggregate document);
}
