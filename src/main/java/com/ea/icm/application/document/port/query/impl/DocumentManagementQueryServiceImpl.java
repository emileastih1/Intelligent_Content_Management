package com.ea.icm.application.document.port.query.impl;

import com.ea.icm.application.document.port.query.DocumentManagementQueryService;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.repository.query.DocumentDomainQueryServicePort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentManagementQueryServiceImpl implements DocumentManagementQueryService {
    DocumentDomainQueryServicePort documentDomainQueryServicePort;

    public DocumentManagementQueryServiceImpl(DocumentDomainQueryServicePort documentDomainQueryServicePort) {
        this.documentDomainQueryServicePort = documentDomainQueryServicePort;
    }

    @Override
    public DocumentAggregate findDocumentById(String id) {
        return documentDomainQueryServicePort.retrieveDocumentById(id);
    }

    @Override
    public List<DocumentAggregate> listDocuments() {
        return documentDomainQueryServicePort.list();
    }

    @Override
    public DocumentAggregate findDocumentByName(String documentName) {
        return null;
    }

    @Override
    public DocumentAggregate findDocumentByFilter(DocumentAggregate documentAggregate) {
        return null;
    }

    @Override
    public DocumentAggregate extractDocumentByName(String documentName) {
        return null;
    }

    @Override
    public DocumentAggregate extractDocumentByFilter(DocumentAggregate documentAggregate) {
        return null;
    }

}
