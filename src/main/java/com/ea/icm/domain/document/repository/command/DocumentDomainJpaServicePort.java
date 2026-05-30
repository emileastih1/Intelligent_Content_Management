package com.ea.icm.domain.document.repository.command;

import com.ea.icm.domain.document.model.DocumentAggregate;

public interface DocumentDomainJpaServicePort {
    long addDocument(DocumentAggregate document);

    DocumentAggregate updateDocument(DocumentAggregate document);

    void deleteDocument(long id);

    void batchUpdate(java.util.List<Long> documentIds, java.util.List<String> tagsToAdd, String category);
}
