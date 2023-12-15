package com.ea.architecture.domain.driven.presentation;

import com.ea.architecture.domain.driven.application.config.security.RestSecurityConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(RestSecurityConfiguration.URL_BASE_PATH)
public abstract class BaseQueryRestController {
}
