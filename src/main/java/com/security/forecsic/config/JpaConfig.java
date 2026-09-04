package com.security.forecsic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.security.forecsic.repositery.jpa")
public class JpaConfig {
}
