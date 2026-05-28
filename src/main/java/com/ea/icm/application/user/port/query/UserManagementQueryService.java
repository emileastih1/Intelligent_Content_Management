package com.ea.icm.application.user.port.query;

import com.ea.icm.domain.user.model.UserAggregate;
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
