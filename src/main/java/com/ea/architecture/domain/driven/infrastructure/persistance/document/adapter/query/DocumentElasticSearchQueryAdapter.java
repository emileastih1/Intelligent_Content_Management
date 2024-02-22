package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.query;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.DocumentDomainQueryServicePort;
import com.ea.architecture.domain.driven.domain.exception.FunctionalException;
import com.ea.architecture.domain.driven.domain.exception.MessageCode;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import com.ea.architecture.domain.driven.infrastructure.repository.document.DocumentESConnectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentElasticSearchQueryAdapter implements DocumentDomainQueryServicePort {

    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentElasticSearchQueryAdapter.class);

    DocumentESConnectorRepository documentESConnectorRepository;
    DocumentInfrastructureMapper documentInfrastructureMapper;

    public DocumentElasticSearchQueryAdapter(DocumentESConnectorRepository documentESConnectorRepository, DocumentInfrastructureMapper documentInfrastructureMapper) {
        this.documentESConnectorRepository = documentESConnectorRepository;
        this.documentInfrastructureMapper = documentInfrastructureMapper;
    }

    @Override
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
    public DocumentAggregate retrieveDocumentByName(String documentName) {
        return null;
    }

    @Override
    public DocumentAggregate retrieveDocumentByFilter(DocumentAggregate documentAggregate) {
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
