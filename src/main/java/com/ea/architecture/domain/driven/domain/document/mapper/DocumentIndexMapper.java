package com.ea.architecture.domain.driven.domain.document.mapper;

import com.ea.architecture.domain.driven.domain.common.adapter.EntityMapperUtil;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import com.ea.architecture.domain.driven.domain.document.model.DocumentFileCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentIndexMapper extends EntityMapperUtil {
    DocumentFileCommand domainToCommand(DocumentAggregate documentAggregate);
}
