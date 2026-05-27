package com.ea.architecture.domain.driven.domain.document.repository.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;

public interface DocumentDomainJpaServicePort {
    long addDocument(DocumentAggregate document);
}
