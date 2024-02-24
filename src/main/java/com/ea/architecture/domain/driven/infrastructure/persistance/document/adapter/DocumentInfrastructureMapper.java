package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter;

import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring")
public interface DocumentInfrastructureMapper extends EntityMapperUtil {
    @Mapping(target = "documentId", source = "id", qualifiedByName = "mapUniqueIdToLong", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "documentType", source = "documentType", qualifiedByName = "mapDocumentTypeToString")
    @Mapping(target = "creationDate", source = "creationDate", qualifiedByName = "mapZonedDateTimeToString")
    DocumentEntity domainToEntity(DocumentAggregate documentAggregate);
    @Mapping(target = "id", source = "documentId", qualifiedByName = "mapLongToUniqueId")
    @Mapping(target = "documentType", source = "documentType", qualifiedByName = "mapStringToDocumentType")
    @Mapping(target = "creationDate", source = "creationDate", qualifiedByName = "mapStringToZonedDateTime")
    DocumentAggregate entityToDomain(DocumentEntity documentEntity);

}
