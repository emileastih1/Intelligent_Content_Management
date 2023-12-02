package com.ea.architecture.domain.driven.gateway;

import com.ea.architecture.domain.driven.application.security.ApiSecurityConfiguration;
import org.springframework.web.bind.annotation.RestController;

@RestController(ApiSecurityConfiguration.URL_BASE_PATH)
public abstract class
BaseRestController {
}
