package com.ea.icm.application.document.dto;

import java.util.List;

public record BatchUpdateRequestDto(List<Long> documentIds, UpdatePayload updatePayload) {
    public record UpdatePayload(List<String> tagsToAdd, String category) {}
}
