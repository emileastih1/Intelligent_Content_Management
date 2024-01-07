package com.ea.architecture.domain.driven.application.user.port.query;

import com.ea.architecture.domain.driven.domain.user.model.UserAggregate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class represents the first port connected to the presentation layer
 */
@Service
@Transactional(readOnly = true)
public interface UserManagementQueryService {
    UserAggregate getUserByFilter(UserAggregate userAggregate) throws Exception;
}
