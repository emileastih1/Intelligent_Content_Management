package com.ea.architecture.domain.driven.domain.user.repository;

import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;

/**
 * This class represents the second port connected to the infrastructure layer
 */
public interface UserDomainQueryServicePort {

    UserAggregate findUserByFilter(UserAggregate userAggregate) throws Exception;
}
