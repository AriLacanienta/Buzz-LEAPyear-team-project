package com.buzzleapyear.trading_api.config;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsDataSourceConfigTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AnalyticsDataSourceConfig.class, PropertyBinding.class)
        .withPropertyValues(
            "spring.datasource.url=jdbc:postgresql://localhost:5432/trading_test",
            "app.analytics.datasource.url=jdbc:postgresql://localhost:5432/analytics_test")
        .withInitializer(context -> context.addBeanFactoryPostProcessor(beanFactory ->
            beanFactory.getBeanDefinition("analyticsFlywayMigration").setLazyInit(true)));

            @Configuration(proxyBeanMethods = false)
            @EnableConfigurationProperties
            static class PropertyBinding {}

    @Test
    void namedParameterQueriesUseTradingWhileAnalyticsQueriesStaySeparate() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            DataSource trading = context.getBean("tradingDataSource", DataSource.class);
            DataSource analytics = context.getBean("analyticsDataSource", DataSource.class);
            NamedParameterJdbcTemplate dashboardJdbc = context.getBean(NamedParameterJdbcTemplate.class);
            JdbcTemplate analyticsJdbc = context.getBean("analyticsJdbcTemplate", JdbcTemplate.class);

            assertThat(dashboardJdbc.getJdbcTemplate().getDataSource()).isSameAs(trading);
            assertThat(analyticsJdbc.getDataSource()).isSameAs(analytics).isNotSameAs(trading);
        });
    }
}
