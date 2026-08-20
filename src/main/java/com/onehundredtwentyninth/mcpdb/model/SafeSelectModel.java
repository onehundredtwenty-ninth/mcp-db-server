package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record SafeSelectModel(
        String sql,
        int rowCount,
        List<Map<String, Object>> rows
) {

}
