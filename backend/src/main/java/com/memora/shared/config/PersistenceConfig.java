package com.memora.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables the {@code @CreatedDate} / {@code @LastModifiedDate} support used by {@code BaseEntity}. */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {}
