package com.ea.architecture.domain.driven.gateway.user.mapper;

import com.ea.architecture.domain.driven.application.user.dto.UserDto;
import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.mapstruct.Mapper;

@Mapper
public interface UserExpositionMapper {
    UserDto domainToDto(UserAggregate userAggregate);
}
