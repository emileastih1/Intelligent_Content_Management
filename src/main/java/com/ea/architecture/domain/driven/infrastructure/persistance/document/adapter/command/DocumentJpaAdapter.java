package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.command;

import com.ea.architecture.domain.driven.domain.document.mapper.DocumentIndexMapper;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.model.DocumentFileCommand;
import com.ea.architecture.domain.driven.domain.document.repository.command.DocumentDomainJpaServicePort;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.DocumentInfrastructureMapper;
import com.ea.architecture.domain.driven.infrastructure.repository.document.DocumentJpaRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class DocumentJpaAdapter implements DocumentDomainJpaServicePort {

    DocumentJpaRepository documentJpaRepository;
    DocumentInfrastructureMapper documentInfrastructureMapper;
    DocumentIndexMapper documentUploadMapper;

    public DocumentJpaAdapter(DocumentInfrastructureMapper documentInfrastructureMapper,
                              DocumentIndexMapper documentUploadMapper,
                              DocumentJpaRepository documentJpaRepository) {
        this.documentInfrastructureMapper = documentInfrastructureMapper;
        this.documentUploadMapper = documentUploadMapper;
        this.documentJpaRepository = documentJpaRepository;
    }

    @Override
    public long addDocument(DocumentAggregate documentAggregate) {
        //Create the command to index the document
        DocumentFileCommand documentFileCommand = new DocumentFileCommand(
                0L,
                StringUtils.EMPTY,
                documentAggregate.getDocumentName(),
                documentAggregate.getDocumentType().toString(),
                documentAggregate.getFile(),
                null
        );
        //Create the indexation event by applying the command to the aggregate
        documentAggregate.indexDocument(documentFileCommand);

        //Save the document to the vector store
        documentAggregate.sendDocumentToEventStore(documentFileCommand);

        return documentJpaRepository.save(documentAggregate).getId();
    }
}
