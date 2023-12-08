package com.ea.architecture.domain.driven.presentation;

import com.ea.architecture.domain.driven.application.security.ApiSecurityConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(ApiSecurityConfiguration.URL_BASE_PATH)
public abstract class BaseQueryRestController {
}
