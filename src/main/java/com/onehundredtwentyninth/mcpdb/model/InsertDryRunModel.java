package com.onehundredtwentyninth.mcpdb.model;

public record InsertDryRunModel(
        String sql,
        String status,
        int affectedRows,
        String rolledBackAt
) {

}
