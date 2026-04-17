package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record ConstraintsInfoModel(
        String schema,
        String table,
        List<Map<String, Object>> keys,
        List<Map<String, Object>> checks,
        List<Map<String, Object>> defaults
) {

}
