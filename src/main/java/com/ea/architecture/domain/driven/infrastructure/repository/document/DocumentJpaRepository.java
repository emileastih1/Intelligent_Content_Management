package com.ea.architecture.domain.driven.infrastructure.repository.document;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentJpaRepository extends ListCrudRepository<DocumentEntity, Long> {
}
