package com.ea.icm.domain.user.repository;

import com.ea.icm.domain.user.model.UserAggregate;

/**
 * This class represents the second port connected to the infrastructure layer
 */
public interface UserDomainQueryServicePort {

    UserAggregate findUserByFilter(UserAggregate userAggregate) throws Exception;
}
