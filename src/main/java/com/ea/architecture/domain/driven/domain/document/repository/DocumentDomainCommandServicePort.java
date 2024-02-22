package com.ea.architecture.domain.driven.domain.document.repository;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;

@Service
public interface DocumentDomainCommandServicePort {
    String addDocument(DocumentAggregate document);
    DocumentAggregate duplicateDocument(DocumentAggregate document);
    DocumentAggregate deleteDocument(DocumentAggregate document);
}
