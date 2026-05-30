package com.ea.icm.application.document.dto;

import jakarta.validation.constraints.NotNull;

public record DocumentDto(long id,
                          String elasticId,
                          @NotNull String documentName,
                          String content,
                          String creationUser,
                          //This field holds the size and the measurement
                          String fileSize,
                          String location) {
}
