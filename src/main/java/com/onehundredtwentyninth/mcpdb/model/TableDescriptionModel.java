package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;
import java.util.Map;

public record TableDescriptionModel(
        String schema,
        String table,
        ForeignKeysModel foreignKeys,
        ConstraintsInfoModel constraintsInfo,
        List<Map<String, Object>> columns
) {

}
