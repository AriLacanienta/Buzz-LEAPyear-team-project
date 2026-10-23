package com.buzzleapyear.trading_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class DashboardConfig {
    @Bean
    public Clock dashboardClock() {
        return Clock.systemDefaultZone();
    }
}
