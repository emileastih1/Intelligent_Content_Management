package com.ea.architecture.domain.driven.application.config.security;

public class RestSecurityConfiguration {

    public static final String URL_BASE_PATH = "/api";

    public static final String BASIC_AUTH = "BasicAuth";

    //TODO Add permission list names here
    public static final String PERMISSION_PREFIX = "demo-api:";
    public static final String PERM_READ = PERMISSION_PREFIX + "READ";
    public static final String PERM_WRITE = PERMISSION_PREFIX + "WRITE";
}
