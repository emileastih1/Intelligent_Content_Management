package com.ea.architecture.domain.driven.domain.document.mapper;

import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.entity.DocumentAttachment;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.model.DocumentUploadCommand;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.model.DocumentEntity;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import javax.print.Doc;

@Mapper(componentModel = "spring")
public interface DocumentUploadMapper extends EntityMapperUtil {
    public abstract DocumentUploadCommand domainToCommand(DocumentAggregate documentAggregate);
}
