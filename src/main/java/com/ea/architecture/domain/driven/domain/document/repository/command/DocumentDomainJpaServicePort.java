package com.ea.architecture.domain.driven.domain.document.repository.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;

@Service
public interface DocumentDomainJpaServicePort {
    long addDocument(DocumentAggregate document);
}
