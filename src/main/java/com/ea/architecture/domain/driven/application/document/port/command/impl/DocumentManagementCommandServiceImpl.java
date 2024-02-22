package com.ea.architecture.domain.driven.application.document.port.command.impl;

import com.ea.architecture.domain.driven.application.document.port.command.DocumentManagementCommandService;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.DocumentDomainCommandServicePort;
import org.springframework.stereotype.Service;

@Service
public class DocumentManagementCommandServiceImpl implements DocumentManagementCommandService {

    DocumentDomainCommandServicePort documentDomainCommandServicePort;

    public DocumentManagementCommandServiceImpl(DocumentDomainCommandServicePort documentDomainCommandServicePort) {
        this.documentDomainCommandServicePort = documentDomainCommandServicePort;
    }

    @Override
    public String addDocument(DocumentAggregate documentAggregate) {
        return documentDomainCommandServicePort.addDocument(documentAggregate);
    }
}
