package com.ea.architecture.domain.driven.infrastructure.persistance.user.adapter;

import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import com.ea.architecture.domain.driven.infrastructure.persistance.user.model.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring", uses = {AddressInfrastructureMapper.class})
@Component
public interface UserInfrastructureMapper extends EntityMapperUtil{
    UserEntity domainToEntity(UserAggregate userAggregate);
    UserAggregate entityToDomain(UserEntity userEntity);
}
