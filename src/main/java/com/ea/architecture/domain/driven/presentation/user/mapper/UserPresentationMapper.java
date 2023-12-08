package com.ea.architecture.domain.driven.presentation.user.mapper;

import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import com.ea.architecture.domain.driven.infrastructure.persistance.user.adapter.EntityMapperUtil;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface UserPresentationMapper extends EntityMapperUtil {
    UserDto domainToDto(UserAggregate userAggregate);
    UserAggregate dtoToDomain(UserDto userDto);
}
