package com.ea.icm.domain.document.mapper;

import com.ea.icm.domain.common.adapter.EntityMapperUtil;
import com.ea.icm.domain.document.model.DocumentAggregate;
import com.ea.icm.domain.document.model.DocumentFileCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentIndexMapper extends EntityMapperUtil {
    DocumentFileCommand domainToCommand(DocumentAggregate documentAggregate);
}
