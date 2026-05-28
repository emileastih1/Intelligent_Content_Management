package com.ea.icm.infrastructure.persistance.user.adapter;

import com.ea.icm.application.user.dto.AddressDto;
import com.ea.icm.infrastructure.persistance.user.model.AddressEntity;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface AddressInfrastructureMapper {
    AddressEntity domainToEntity(AddressDto addressDto);

    AddressDto entityToDomain(AddressEntity addressEntity);
}
