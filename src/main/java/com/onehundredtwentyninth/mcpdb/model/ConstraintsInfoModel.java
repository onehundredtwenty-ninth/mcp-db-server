package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record ConstraintsInfoModel(
        List<Map<String, Object>> primaryKeys,
        List<Map<String, Object>> uniqueKeys,
        List<Map<String, Object>> checks,
        List<Map<String, Object>> defaults
) {

}
