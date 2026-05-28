package com.ea.icm.infrastructure.repository.document;

import com.ea.icm.infrastructure.persistance.document.model.DocumentEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentJpaRepository extends ListCrudRepository<DocumentEntity, Long> {
}
