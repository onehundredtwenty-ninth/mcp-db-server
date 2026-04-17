package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record TableDescriptionModel(
        String schema,
        String table,
        List<Map<String, Object>> columns
) {

}
