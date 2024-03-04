package com.ea.architecture.domain.driven.infrastructure.repository.document;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentJpaRepository extends ListCrudRepository<DocumentAggregate, Long> {
}
