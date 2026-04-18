package com.onehundredtwentyninth.mcpdb.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.db")
public record DatabaseProperties(
        @NotBlank
        String host,

        @Min(1)
        @Max(65535)
        int port,

        @NotBlank
        String database,

        @NotBlank
        String username,

        @NotBlank
        String password,

        @NotEmpty
        List<String> hostWhitelist,

        @NotNull
        List<String> schemaWhitelist,

        @Min(1)
        @Max(60)
        int connectTimeoutSeconds,

        @Min(1)
        @Max(120)
        int queryTimeoutSeconds,

        @Min(1)
        @Max(5000)
        int maxSelectRows,

        @Min(20)
        @Max(10000)
        int maxTextLength
) {

    public String buildJdbcUrl() {
        return "jdbc:jtds:sqlserver://%s:%s/%s".formatted(host, port, database);
    }
}