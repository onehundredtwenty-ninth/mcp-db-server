package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record SearchSchemaModel(
        String term,
        List<Map<String, Object>> matches
) {

}
