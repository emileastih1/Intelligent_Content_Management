package com.ea.architecture.domain.driven.domain.common.adapter;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.vo.DocumentTypes;
import com.ea.architecture.domain.driven.domain.document.vo.FileSize;
import com.ea.architecture.domain.driven.domain.document.vo.UnitOfMeasurement;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Named;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Base64;

public interface EntityMapperUtil {
    @Named("mapUniqueIdToString")
    default String mapUniqueIdToString(UniqueId uid) {
        return uid != null ? String.valueOf(uid.getId()) : StringUtils.EMPTY;
    }

    @Named("mapStringToUniqueId")
    default UniqueId mapStringToUniqueId(String uid) {
        return new UniqueId(uid);
    }

    @Named("mapLongToString")
    default String mapLongToString(Long value) {
        return value != null ? String.valueOf(value) : StringUtils.EMPTY;
    }

    @Named("mapStringToLong")
    default Long mapStringToLong(String value) {
        return StringUtils.isNotBlank(value) ? Long.parseLong(value) : 0L;
    }

    @Named("mapStringToFileSize")
    default FileSize map(String fileSize) {
        if (fileSize != null) {
            var fileSizeSplit = fileSize.split(" ");
            return new FileSize(fileSizeSplit[0], UnitOfMeasurement.valueOf(fileSizeSplit[1]));
        } else {
            return null;
        }
    }

    @Named("mapFileSizeToString")
    default String map(FileSize fileSize) {
        if (fileSize != null) {
            return fileSize.getSize() + File.separator + fileSize.getUnitOfMeasurement();
        } else {
            return "0";
        }
    }

    @Named("mapBase64StringToByteArray")
    default byte[] mapBase64StringToByteArray(String base64File) {
        // Check for null to avoid NullPointerException
        return base64File == null ? null : Base64.getDecoder().decode(base64File);
    }

    @Named("mapByteArrayToBase64String")
    default String mapByteArrayToBase64String(byte[] byteArray) {
        return byteArray == null ? null : Base64.getEncoder().encodeToString(byteArray);
    }

    @Named("mapDocumentTypeToString")
    default String mapDocumentType(DocumentTypes documentTypes) {
        return documentTypes != null ? documentTypes.name() : DocumentTypes.UNDEFINED.name();
    }

    @Named("mapStringToDocumentType")
    default DocumentTypes mapDocumentType(String documentType) {
        return documentType != null ? DocumentTypes.valueOf(documentType) : DocumentTypes.UNDEFINED;
    }

    @Named("mapZonedDateTimeToString")
    default String mapZonedDateTimeToString(ZonedDateTime zonedDateTime) {
        return zonedDateTime != null ? zonedDateTime.toString() : null;
    }

    @Named("mapStringToZonedDateTime")
    default ZonedDateTime mapStringToZonedDateTime(String zonedDateTime) {
        return zonedDateTime != null ? ZonedDateTime.parse(zonedDateTime) : null;
    }
}
