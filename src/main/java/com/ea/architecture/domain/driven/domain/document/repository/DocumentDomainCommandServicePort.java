package com.ea.architecture.domain.driven.domain.document.repository;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;

@Service
public interface DocumentDomainCommandServicePort {
    DocumentAggregate addDocumentToLocation(DocumentAggregate document, String location);
    DocumentAggregate copyDocumentToLocation(DocumentAggregate document, String location);
    DocumentAggregate moveDocumentToLocation(DocumentAggregate document, String location);
    DocumentAggregate deleteDocument(DocumentAggregate document);
}
