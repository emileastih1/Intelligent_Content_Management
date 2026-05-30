package com.ea.icm.domain.document.repository.query;

import com.ea.icm.domain.document.model.DocumentAggregate;

import java.util.List;

public interface DocumentDomainQueryServicePort {
    DocumentAggregate retrieveDocumentById(String documentId);

    List<DocumentAggregate> list();

    DocumentAggregate retrieveDocumentByName(String documentName);

    DocumentAggregate retrieveDocumentByFilter(DocumentAggregate documentAggregate);

    DocumentAggregate extractDocumentByName(String documentName);

    DocumentAggregate extractDocumentByFilter(DocumentAggregate documentAggregate);

    List<DocumentAggregate> search(String query);

}
