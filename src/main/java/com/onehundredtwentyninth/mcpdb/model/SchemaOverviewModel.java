package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record SchemaOverviewModel(
        String database,
        List<String> schemas,
        Map<String, Long> tableCountBySchema,
        List<Map<String, Object>> tables
) {

}
