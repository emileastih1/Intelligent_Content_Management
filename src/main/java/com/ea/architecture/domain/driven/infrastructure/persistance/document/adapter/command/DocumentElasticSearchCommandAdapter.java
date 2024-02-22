package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.DocumentDomainCommandServicePort;
import com.ea.architecture.domain.driven.domain.exception.FunctionalException;
import com.ea.architecture.domain.driven.domain.exception.MessageCode;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import com.ea.architecture.domain.driven.infrastructure.repository.document.DocumentESConnectorRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentElasticSearchCommandAdapter implements DocumentDomainCommandServicePort {

    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentElasticSearchCommandAdapter.class);

    DocumentESConnectorRepository documentESConnectorRepository;
    DocumentInfrastructureMapper documentInfrastructureMapper;

    public DocumentElasticSearchCommandAdapter(DocumentESConnectorRepository documentESConnectorRepository, DocumentInfrastructureMapper documentInfrastructureMapper) {
        this.documentESConnectorRepository = documentESConnectorRepository;
        this.documentInfrastructureMapper = documentInfrastructureMapper;
    }

    public DocumentAggregate retrieveDocumentById(Long documentId) {
        DocumentEntity documentEntity = null;
        try {
            documentEntity = documentESConnectorRepository
                    .getDocumentById(String.valueOf(documentId));
        } catch (IOException e) {
            LOGGER.error("Error while retrieving document by ID: "+e.getMessage());
            throw new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND, "Document not found: "+e.getMessage());
        }
        return documentInfrastructureMapper.entityToDomain(documentEntity);
    }

    @Override
    public String addDocument(DocumentAggregate document) {
        String documentStatus = StringUtils.EMPTY;
        try {
            documentStatus = documentESConnectorRepository.addDocument(documentInfrastructureMapper.domainToEntity(document));
        } catch (IOException e) {
            LOGGER.error("Error while adding document: "+e.getMessage());
            throw new FunctionalException(MessageCode.DOCUMENT_CANNOT_BE_ADDED , "Document cannot be added: "+e.getMessage());
        }
        return documentStatus;
    }

    @Override
    public DocumentAggregate duplicateDocument(DocumentAggregate document) {
        return null;
    }

    @Override
    public DocumentAggregate deleteDocument(DocumentAggregate document) {
        return null;
    }
}
