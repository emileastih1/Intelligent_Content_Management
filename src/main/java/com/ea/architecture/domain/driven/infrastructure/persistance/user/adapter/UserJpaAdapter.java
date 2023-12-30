package com.ea.architecture.domain.driven.infrastructure.persistance.user.adapter;

import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import com.ea.architecture.domain.driven.domain.user.repository.UserDomainServicePort;
import com.ea.architecture.domain.driven.infrastructure.persistance.user.model.UserEntity;
import com.ea.architecture.domain.driven.infrastructure.repository.user.UserJpaRepository;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class UserJpaAdapter implements UserDomainServicePort {

    UserJpaRepository userJpaRepository;

    UserInfrastructureMapper userInfrastructureMapper;

    public UserJpaAdapter(UserInfrastructureMapper userInfrastructureMapper, UserJpaRepository userJpaRepository) {
        this.userInfrastructureMapper = userInfrastructureMapper;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserAggregate findUserByFilter(UserAggregate userAggregate) throws Exception {
        UserEntity userEntity = userInfrastructureMapper.domainToEntity(userAggregate);
        return userInfrastructureMapper.entityToDomain(userJpaRepository.findByFirstNameAndLastName(userEntity.getFirstName(), userEntity.getLastName())
                .orElseThrow(() -> new Exception("User not found")));
    }
}
