package com.gonguham.backend.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/h2-console/**", "/api/v1/health").permitAll()
                it.anyRequest().permitAll()
            }
            .headers { headers -> headers.frameOptions { it.sameOrigin() } }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }
}
