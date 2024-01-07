package com.ea.architecture.domain.driven.application.document.dto;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;

import java.time.ZonedDateTime;

public record DocumentDto(UniqueId id,
                          String name,
                          String creationUser,
                          //This field holds the size and the measurement
                          String fileSize,
                          String location) {
}
