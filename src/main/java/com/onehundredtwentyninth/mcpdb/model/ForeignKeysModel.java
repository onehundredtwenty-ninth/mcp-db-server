package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record ForeignKeysModel(
        String schema,
        String table,
        List<Map<String, Object>> outgoing,
        List<Map<String, Object>> incoming
) {

}
