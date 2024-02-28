package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.command;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainElasticServicePort;
import com.ea.architecture.domain.driven.domain.exception.FunctionalException;
import com.ea.architecture.domain.driven.domain.exception.MessageCode;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.architecture.domain.driven.infrastructure.repository.document.DocumentESConnectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentElasticSearchElasticAdapter implements DocumentDomainElasticServicePort {
    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentElasticSearchElasticAdapter.class);
    DocumentESConnectorRepository documentESConnectorRepository;
    DocumentInfrastructureMapper documentInfrastructureMapper;

    public DocumentElasticSearchElasticAdapter(DocumentESConnectorRepository documentESConnectorRepository, DocumentInfrastructureMapper documentInfrastructureMapper) {
        this.documentESConnectorRepository = documentESConnectorRepository;
        this.documentInfrastructureMapper = documentInfrastructureMapper;
    }

    @Override
    public DocumentResult addOrUpdateDocument(DocumentAggregate document) {
        DocumentResult documentResult;
        try {
            documentResult = documentESConnectorRepository.addOrUpdateDocument(documentInfrastructureMapper.domainToEntity(document));
        } catch (IOException e) {
            LOGGER.error("Error while adding document: "+e.getMessage());
            throw new FunctionalException(MessageCode.DOCUMENT_CANNOT_BE_ADDED , "Document cannot be added: "+e.getMessage());
        }
        return documentResult;
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
