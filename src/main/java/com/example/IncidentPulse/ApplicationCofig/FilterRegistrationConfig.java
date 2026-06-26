package com.example.IncidentPulse.ApplicationCofig;

import com.example.IncidentPulse.Security.ApiKeyFilter;
import com.example.IncidentPulse.Security.JwtFilter;
import com.example.IncidentPulse.Security.LoginRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-registers any {@code Filter} @Component as a servlet filter
 * for every request. Our security filters are wired explicitly into the Spring
 * Security chain (see SecurityConfig), so we disable the duplicate servlet-level
 * registration to keep each filter running exactly once, in the intended order.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtFilter> disableJwtAutoRegistration(JwtFilter filter) {
        return disabled(filter);
    }

    @Bean
    public FilterRegistrationBean<LoginRateLimitFilter> disableLoginRateLimitAutoRegistration(LoginRateLimitFilter filter) {
        return disabled(filter);
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> disableApiKeyAutoRegistration(ApiKeyFilter filter) {
        return disabled(filter);
    }

    private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disabled(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
