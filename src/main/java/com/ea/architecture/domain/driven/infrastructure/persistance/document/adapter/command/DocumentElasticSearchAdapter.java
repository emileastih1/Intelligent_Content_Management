package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.command;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainElasticServicePort;
import com.ea.architecture.domain.driven.domain.exception.FunctionalException;
import com.ea.architecture.domain.driven.domain.exception.MessageCode;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.DocumentExtractor;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.utils.ExtractionUtils;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentElasticEntity;
import com.ea.architecture.domain.driven.infrastructure.repository.document.DocumentESConnectorRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;

@Service
public class DocumentElasticSearchAdapter implements DocumentDomainElasticServicePort {
    public static final Logger LOGGER = LoggerFactory.getLogger(DocumentElasticSearchAdapter.class);
    DocumentESConnectorRepository documentESConnectorRepository;
    DocumentInfrastructureMapper documentInfrastructureMapper;
    DocumentExtractor documentExtractor;

    public DocumentElasticSearchAdapter(DocumentESConnectorRepository documentESConnectorRepository,
                                        DocumentInfrastructureMapper documentInfrastructureMapper,
                                        DocumentExtractor documentExtractor) {
        this.documentESConnectorRepository = documentESConnectorRepository;
        this.documentInfrastructureMapper = documentInfrastructureMapper;
        this.documentExtractor = documentExtractor;
    }

    @Override
    public DocumentResult addOrUpdateDocument(DocumentAggregate document) {
        DocumentResult documentResult;
        try {
            DocumentElasticEntity elasticDocument = documentInfrastructureMapper.domainToEntity(document);
            ImmutablePair<String, Metadata> extractedResult = documentExtractor.extract(document.getFile());
            elasticDocument.setDocumentContent(extractedResult.getLeft());
            elasticDocument.setDocumentMetadata(ExtractionUtils.metadataToString(extractedResult.getRight()));
            documentResult = documentESConnectorRepository.addOrUpdateDocument(elasticDocument);
        } catch (IOException | TikaException | SAXException e) {
            LOGGER.error("Error while adding document: " + e.getMessage());
            throw new FunctionalException(MessageCode.DOCUMENT_CANNOT_BE_ADDED, "Document cannot be added: " + e.getMessage());
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
