package com.ea.architecture.domain.driven.domain.document.repository;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;

@Service
public interface DocumentDomainQueryServicePort {
    DocumentAggregate retrieveDocumentById(Long documentId);
    DocumentAggregate retrieveDocumentByName(String documentName);
    DocumentAggregate retrieveDocumentByFilter(DocumentAggregate documentAggregate);
    DocumentAggregate extractDocumentByName(String documentName);
    DocumentAggregate extractDocumentByFilter(DocumentAggregate documentAggregate);
}
