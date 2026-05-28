package com.ea.icm.infrastructure.persistance.document.adapter.query;

import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.repository.query.DocumentDomainQueryServicePort;
import com.ea.icm.domain.exception.FunctionalException;
import com.ea.icm.domain.exception.MessageCode;
import com.ea.icm.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.icm.infrastructure.persistance.document.model.DocumentElasticEntity;
import com.ea.icm.infrastructure.repository.document.DocumentESConnectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentElasticSearchQueryAdapter implements DocumentDomainQueryServicePort {

    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentElasticSearchQueryAdapter.class);

    DocumentESConnectorRepository documentESConnectorRepository;
    DocumentInfrastructureMapper documentInfrastructureMapper;


    public DocumentElasticSearchQueryAdapter(DocumentESConnectorRepository documentESConnectorRepository,
                                             DocumentInfrastructureMapper documentInfrastructureMapper) {
        this.documentESConnectorRepository = documentESConnectorRepository;
        this.documentInfrastructureMapper = documentInfrastructureMapper;
    }

    @Override
    public DocumentAggregate retrieveDocumentById(String documentId) {
        DocumentElasticEntity documentElasticEntity = null;
        try {
            documentElasticEntity = documentESConnectorRepository
                    .getDocumentById(documentId);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving document by ID: {}", e.getMessage());
            throw new FunctionalException(MessageCode.DOCUMENT_NOT_FOUND, "Document not found: " + e.getMessage());
        }
        return documentInfrastructureMapper.entityToDomain(documentElasticEntity);
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
