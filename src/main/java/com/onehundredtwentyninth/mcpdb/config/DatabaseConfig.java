package com.onehundredtwentyninth.mcpdb.config;

import com.onehundredtwentyninth.mcpdb.validation.HostWhitelistValidator;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(DatabaseProperties properties, HostWhitelistValidator hostWhitelistValidator) {
        hostWhitelistValidator.validate(properties.host(), properties.hostWhitelist());
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("net.sourceforge.jtds.jdbc.Driver");
        dataSource.setUrl(properties.buildJdbcUrl());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        return dataSource;
    }
}
