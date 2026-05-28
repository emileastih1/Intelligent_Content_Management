package com.ea.icm.domain.document.repository.query;

import com.ea.icm.domain.document.model.DocumentAggregate;

public interface DocumentDomainQueryServicePort {
    DocumentAggregate retrieveDocumentById(String documentId);

    DocumentAggregate retrieveDocumentByName(String documentName);

    DocumentAggregate retrieveDocumentByFilter(DocumentAggregate documentAggregate);

    DocumentAggregate extractDocumentByName(String documentName);

    DocumentAggregate extractDocumentByFilter(DocumentAggregate documentAggregate);

}
