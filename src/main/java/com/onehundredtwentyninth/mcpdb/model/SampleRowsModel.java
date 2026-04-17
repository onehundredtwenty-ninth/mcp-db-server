package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record SampleRowsModel(
        String schema,
        String table,
        int limit,
        List<Map<String, Object>> rows
) {

}
