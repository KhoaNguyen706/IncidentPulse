package com.example.IncidentPulse.ApplicationCofig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunables for the per-IP login rate limiter (bound from {@code app.rate-limit.login}).
 * Defaults: a burst of 5 attempts, then 1 attempt back every 3 minutes
 * (≈ full refill after 15 minutes).
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.rate-limit.login")
public class LoginRateLimitProperties {
    private long capacity = 5;
    private long refillTokens = 1;
    private long refillPeriodMinutes = 3;
}
