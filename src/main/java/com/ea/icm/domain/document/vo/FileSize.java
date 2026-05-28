package com.ea.icm.domain.document.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class FileSize {
    private String size;
    private UnitOfMeasurement unitOfMeasurement;
}

