package com.ea.icm.application.document.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DocumentDto(long id,
                          String elasticId,
                          @NotNull String documentName,
                          String content,
                          List<String> tags,
                          String category,
                          String sentiment,
                          String creationUser,
                          //This field holds the size and the measurement
                          String fileSize,
                          String location) {
}
