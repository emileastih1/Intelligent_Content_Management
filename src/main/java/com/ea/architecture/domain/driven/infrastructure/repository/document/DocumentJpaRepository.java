package com.ea.architecture.domain.driven.infrastructure.repository.document;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentJpaEntity;
import org.springframework.data.repository.ListCrudRepository;

public interface DocumentJpaRepository extends ListCrudRepository<DocumentJpaEntity, Long> {
}
