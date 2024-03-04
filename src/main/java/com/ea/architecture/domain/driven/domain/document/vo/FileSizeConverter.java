package com.ea.architecture.domain.driven.domain.document.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FileSizeConverter implements AttributeConverter<FileSize, String> {
    @Override
    public String convertToDatabaseColumn(FileSize fileSize) {
        return fileSize.getSize() + " " + fileSize.getUnitOfMeasurement().name();
    }

    @Override
    public FileSize convertToEntityAttribute(String s) {
        return new FileSize(s.split(" ")[0], UnitOfMeasurement.valueOf(s.split(" ")[1]));
    }
}
