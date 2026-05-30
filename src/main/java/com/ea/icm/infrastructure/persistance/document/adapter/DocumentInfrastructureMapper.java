package com.ea.icm.infrastructure.persistance.document.adapter;

import com.ea.icm.domain.common.adapter.EntityMapperUtil;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.infrastructure.persistance.document.model.DocumentElasticEntity;
import com.ea.icm.infrastructure.persistance.document.model.DocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentInfrastructureMapper extends EntityMapperUtil {
    // Elasticsearch mapping
    @Mapping(target = "documentType", source = "documentType", qualifiedByName = "mapDocumentTypeToString")
    @Mapping(target = "creationDate", source = "creationDate", qualifiedByName = "mapZonedDateTimeToString")
    @Mapping(target = "documentId", source = "id", qualifiedByName = "mapLongToString")
    DocumentElasticEntity domainToEntity(DocumentAggregate documentAggregate);

    @Mapping(target = "documentType", source = "documentType", qualifiedByName = "mapStringToDocumentType")
    @Mapping(target = "creationDate", source = "creationDate", qualifiedByName = "mapStringToZonedDateTime")
    @Mapping(target = "id", source = "documentId", qualifiedByName = "mapStringToLong")
    DocumentAggregate entityToDomain(DocumentElasticEntity documentElasticEntity);

    // JPA mapping
    @Mapping(target = "name", source = "documentName")
    @Mapping(target = "content", source = "file")
    @Mapping(target = "textContent", source = "content")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "sentiment", source = "sentiment")
    @Mapping(target = "fileSize", source = "fileSize", qualifiedByName = "mapFileSizeToString")
    @Mapping(target = "creationDate", source = "creationDate")
    @Mapping(target = "modificationDate", source = "modificationDate")
    DocumentEntity domainToJpaEntity(DocumentAggregate documentAggregate);

    @Mapping(target = "documentName", source = "name")
    @Mapping(target = "file", source = "content")
    @Mapping(target = "content", source = "textContent")
    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "sentiment", source = "sentiment")
    @Mapping(target = "fileSize", source = "fileSize", qualifiedByName = "mapStringToFileSize")
    @Mapping(target = "creationDate", source = "creationDate")
    @Mapping(target = "modificationDate", source = "modificationDate")
    DocumentAggregate jpaEntityToDomain(DocumentEntity documentEntity);
}
