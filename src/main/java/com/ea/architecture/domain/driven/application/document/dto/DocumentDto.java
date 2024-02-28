package com.ea.architecture.domain.driven.application.document.dto;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

public record DocumentDto(UniqueId id,
                          String elasticId,
                          @NotNull String documentName,
                          String creationUser,
                          //This field holds the size and the measurement
                          String fileSize,
                          String location) {
}
