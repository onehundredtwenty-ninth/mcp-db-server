package com.onehundredtwentyninth.mcpdb.config;

import com.onehundredtwentyninth.mcpdb.tool.DatabaseTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(DatabaseTools databaseTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(databaseTools)
                .build();
    }
}
