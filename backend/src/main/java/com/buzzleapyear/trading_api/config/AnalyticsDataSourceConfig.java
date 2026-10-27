package com.buzzleapyear.trading_api.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.flywaydb.core.Flyway;

@Configuration
public class AnalyticsDataSourceConfig {
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties tradingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    DataSource tradingDataSource() {
        return tradingDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    NamedParameterJdbcTemplate tradingNamedParameterJdbcTemplate(
            @Qualifier("tradingDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @ConfigurationProperties("app.analytics.datasource")
    DataSourceProperties analyticsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource analyticsDataSource() {
        return analyticsDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    JdbcTemplate analyticsJdbcTemplate(
            @Qualifier("analyticsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    InitializingBean analyticsFlywayMigration(
            @Qualifier("analyticsDataSource") DataSource dataSource) {
        return () -> Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/analytics")
                .load()
                .migrate();
    }
}
