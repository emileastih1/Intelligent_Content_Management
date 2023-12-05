package com.ea.architecture.domain.driven.domain.user.port;

import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import com.ea.architecture.domain.driven.infrastructure.user.UserEntity;

public interface UserDomainService {

    UserAggregate findUser(UserEntity userEntity);
}
