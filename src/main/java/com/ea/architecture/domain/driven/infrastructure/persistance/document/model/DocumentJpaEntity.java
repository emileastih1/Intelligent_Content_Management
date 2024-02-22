package com.ea.architecture.domain.driven.infrastructure.persistance.document.model;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public record DocumentJpaEntity(
        @Id Long id,
        String name, String content, String type,
        String status, String createdBy, String createdDate,
        String updatedBy, String updatedDate) {
}
