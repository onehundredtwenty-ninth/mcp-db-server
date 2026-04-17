package com.onehundredtwentyninth.mcpdb.model;

import java.util.List;

public record TableDependenciesModel(
        String table,
        List<String> mustExistBeforeInsert,
        int dependencyCount
) {

}
