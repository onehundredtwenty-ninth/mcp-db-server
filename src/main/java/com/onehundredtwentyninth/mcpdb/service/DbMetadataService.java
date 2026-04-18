package com.onehundredtwentyninth.mcpdb.service;

import com.onehundredtwentyninth.mcpdb.config.DatabaseProperties;
import com.onehundredtwentyninth.mcpdb.model.ConstraintsInfoModel;
import com.onehundredtwentyninth.mcpdb.model.ForeignKeysModel;
import com.onehundredtwentyninth.mcpdb.model.InsertDryRunModel;
import com.onehundredtwentyninth.mcpdb.model.SafeSelectModel;
import com.onehundredtwentyninth.mcpdb.model.SchemaOverviewModel;
import com.onehundredtwentyninth.mcpdb.model.SearchSchemaModel;
import com.onehundredtwentyninth.mcpdb.model.TableDependenciesModel;
import com.onehundredtwentyninth.mcpdb.model.TableDescriptionModel;
import com.onehundredtwentyninth.mcpdb.validation.SqlSafetyValidator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DbMetadataService {

    private final DataSource dataSource;
    private final DatabaseProperties properties;
    private final SqlSafetyValidator sqlSafetyValidator;

    public DbMetadataService(DataSource dataSource, DatabaseProperties properties, SqlSafetyValidator sqlSafetyValidator) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.sqlSafetyValidator = sqlSafetyValidator;
    }

    public SchemaOverviewModel schemaOverview() {
        var sql = """
                SELECT t.TABLE_SCHEMA, t.TABLE_NAME, t.TABLE_TYPE
                FROM INFORMATION_SCHEMA.TABLES t
                WHERE t.TABLE_SCHEMA IN (%s)
                ORDER BY t.TABLE_SCHEMA, t.TABLE_NAME
                """.formatted(inClause(properties.schemaWhitelist().size()));
        var rows = query(sql, properties.schemaWhitelist().toArray());
        var counts = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.get("table_schema").toString(),
                        LinkedHashMap::new,
                        Collectors.counting())
                );
        return new SchemaOverviewModel(properties.database(), properties.schemaWhitelist(), counts, rows);
    }

    public TableDescriptionModel describeTable(String schema, String table) {
        validateSchema(schema);
        var sql = """
                SELECT c.ORDINAL_POSITION,
                       c.COLUMN_NAME,
                       c.DATA_TYPE,
                       c.CHARACTER_MAXIMUM_LENGTH,
                       c.NUMERIC_PRECISION,
                       c.NUMERIC_SCALE,
                       c.IS_NULLABLE,
                       COLUMNPROPERTY(OBJECT_ID(c.TABLE_SCHEMA + '.' + c.TABLE_NAME), c.COLUMN_NAME, 'IsIdentity') AS IS_IDENTITY,
                       COLUMNPROPERTY(OBJECT_ID(c.TABLE_SCHEMA + '.' + c.TABLE_NAME), c.COLUMN_NAME, 'IsComputed') AS IS_COMPUTED
                FROM INFORMATION_SCHEMA.COLUMNS c
                WHERE c.TABLE_SCHEMA = ? AND c.TABLE_NAME = ?
                ORDER BY c.ORDINAL_POSITION
                """;
        var columns = query(sql, schema, table);
        return new TableDescriptionModel(schema, table, columns);
    }

    public ForeignKeysModel foreignKeys(String schema, String table) {
        validateSchema(schema);
        var sql = """
                SELECT fk.name AS FK_NAME,
                       sch_parent.name AS PARENT_SCHEMA,
                       tab_parent.name AS PARENT_TABLE,
                       col_parent.name AS PARENT_COLUMN,
                       sch_ref.name AS REFERENCED_SCHEMA,
                       tab_ref.name AS REFERENCED_TABLE,
                       col_ref.name AS REFERENCED_COLUMN
                FROM sys.foreign_keys fk
                JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
                JOIN sys.tables tab_parent ON fk.parent_object_id = tab_parent.object_id
                JOIN sys.schemas sch_parent ON tab_parent.schema_id = sch_parent.schema_id
                JOIN sys.columns col_parent ON fkc.parent_object_id = col_parent.object_id AND fkc.parent_column_id = col_parent.column_id
                JOIN sys.tables tab_ref ON fk.referenced_object_id = tab_ref.object_id
                JOIN sys.schemas sch_ref ON tab_ref.schema_id = sch_ref.schema_id
                JOIN sys.columns col_ref ON fkc.referenced_object_id = col_ref.object_id AND fkc.referenced_column_id = col_ref.column_id
                WHERE sch_parent.name = ? AND tab_parent.name = ?
                ORDER BY fk.name, fkc.constraint_column_id
                """;
        var outgoing = query(sql, schema, table);
        var incomingSql = sql.replace("sch_parent.name = ? AND tab_parent.name = ?", "sch_ref.name = ? AND tab_ref.name = ?");
        var incoming = query(incomingSql, schema, table);
        return new ForeignKeysModel(schema, table, outgoing, incoming);
    }

    public ConstraintsInfoModel constraintsInfo(String schema, String table) {
        validateSchema(schema);
        var keySql = """
                SELECT tc.CONSTRAINT_NAME,
                       tc.CONSTRAINT_TYPE,
                       kcu.COLUMN_NAME,
                       kcu.ORDINAL_POSITION
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                  ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                 AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
                 AND tc.TABLE_NAME = kcu.TABLE_NAME
                WHERE tc.TABLE_SCHEMA = ? AND tc.TABLE_NAME = ?
                ORDER BY tc.CONSTRAINT_TYPE, tc.CONSTRAINT_NAME, kcu.ORDINAL_POSITION
                """;
        var checkSql = """
                SELECT cc.name AS CHECK_NAME,
                       cc.definition AS CHECK_DEFINITION
                FROM sys.check_constraints cc
                JOIN sys.tables t ON cc.parent_object_id = t.object_id
                JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = ? AND t.name = ?
                ORDER BY cc.name
                """;
        var defaultsSql = """
                SELECT c.name AS COLUMN_NAME,
                       dc.name AS DEFAULT_NAME,
                       dc.definition AS DEFAULT_DEFINITION
                FROM sys.default_constraints dc
                JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
                JOIN sys.tables t ON c.object_id = t.object_id
                JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = ? AND t.name = ?
                ORDER BY c.column_id
                """;
        return new ConstraintsInfoModel(schema,
                table,
                query(keySql, schema, table),
                query(checkSql, schema, table),
                query(defaultsSql, schema, table)
        );
    }

    public TableDependenciesModel tableDependencies(String schema, String table) {
        validateSchema(schema);
        var parentGraph = loadParentGraph();
        var key = schema + "." + table;
        Set<String> visited = new LinkedHashSet<>();
        var stack = new ArrayDeque<>(parentGraph.getOrDefault(key, List.of()));
        while (!stack.isEmpty()) {
            var current = stack.pop();
            if (visited.add(current)) {
                parentGraph.getOrDefault(current, List.of()).forEach(stack::push);
            }
        }
        var order = topologicalOrder(visited, parentGraph);
        return new TableDependenciesModel(key, order, visited.size());
    }

    public SafeSelectModel safeSelect(String sql, Integer limit) {
        var safeLimit = Math.min(limit == null ? properties.maxSelectRows() : limit, properties.maxSelectRows());
        var limitedSql = sqlSafetyValidator.enforceTopLimit(sql, safeLimit);
        var rows = query(limitedSql);
        return new SafeSelectModel(limitedSql, rows.size(), rows);
    }

    public InsertDryRunModel insertDryRun(String sql) {
        sqlSafetyValidator.validateInsert(sql);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setReadOnly(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            int affected;
            try (var statement = connection.prepareStatement(sql)) {
                statement.setQueryTimeout(properties.queryTimeoutSeconds());
                affected = statement.executeUpdate();
            }
            connection.rollback();
            return new InsertDryRunModel(
                    sql,
                    "ROLLED_BACK",
                    affected,
                    Instant.now().toString()
            );
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка insert_dry_run: " + e.getMessage(), e);
        }
    }

    public SearchSchemaModel searchSchema(String term) {
        var sql = """
                SELECT c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS c
                WHERE c.TABLE_SCHEMA IN (%s)
                  AND (
                    LOWER(c.TABLE_NAME) LIKE LOWER(?) OR
                    LOWER(c.COLUMN_NAME) LIKE LOWER(?)
                  )
                ORDER BY c.TABLE_SCHEMA, c.TABLE_NAME, c.ORDINAL_POSITION
                """.formatted(inClause(properties.schemaWhitelist().size()));
        var params = new ArrayList<>(properties.schemaWhitelist());
        params.add("%" + term + "%");
        params.add("%" + term + "%");
        return new SearchSchemaModel(term,  query(sql, params.toArray()));
    }

    private List<Map<String, Object>> query(String sql, Object... params) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            connection.setReadOnly(true);
            statement.setQueryTimeout(properties.queryTimeoutSeconds());
            for (var i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (var rs = statement.executeQuery()) {
                return resultSetToList(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка SQL: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        var metaData = rs.getMetaData();
        var columns = metaData.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (var i = 1; i <= columns; i++) {
                var value = rs.getObject(i);
                if (value instanceof String s && s.length() > properties.maxTextLength()) {
                    value = s.substring(0, properties.maxTextLength()) + "...";
                }
                row.put(metaData.getColumnLabel(i).toLowerCase(), value);
            }
            rows.add(row);
        }
        return rows;
    }

    private void validateSchema(String schema) {
        if (schema == null || properties.schemaWhitelist().stream().noneMatch(schema::equalsIgnoreCase)) {
            throw new IllegalArgumentException("Схема '%s' не входит в whitelist".formatted(schema));
        }
    }

    private String quoted(String schema, String table) {
        return "[" + schema + "].[" + table + "]";
    }

    private String inClause(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private Map<String, List<String>> loadParentGraph() {
        var sql = """
                SELECT sch_parent.name AS PARENT_SCHEMA,
                       tab_parent.name AS PARENT_TABLE,
                       sch_ref.name AS REFERENCED_SCHEMA,
                       tab_ref.name AS REFERENCED_TABLE
                FROM sys.foreign_keys fk
                JOIN sys.tables tab_parent ON fk.parent_object_id = tab_parent.object_id
                JOIN sys.schemas sch_parent ON tab_parent.schema_id = sch_parent.schema_id
                JOIN sys.tables tab_ref ON fk.referenced_object_id = tab_ref.object_id
                JOIN sys.schemas sch_ref ON tab_ref.schema_id = sch_ref.schema_id
                WHERE sch_parent.name IN (%1$s) AND sch_ref.name IN (%1$s)
                ORDER BY sch_parent.name, tab_parent.name
                """.formatted(inClause(properties.schemaWhitelist().size()));
        var params = new ArrayList<>(properties.schemaWhitelist());
        params.addAll(properties.schemaWhitelist());
        var rows = query(sql, params.toArray());
        Map<String, List<String>> graph = new HashMap<>();
        for (var row : rows) {
            var child = row.get("parent_schema") + "." + row.get("parent_table");
            var parent = row.get("referenced_schema") + "." + row.get("referenced_table");
            graph.computeIfAbsent(child, k -> new ArrayList<>()).add(parent);
        }
        return graph;
    }

    private List<String> topologicalOrder(Set<String> nodes, Map<String, List<String>> graph) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> reverse = new HashMap<>();
        for (var node : nodes) {
            indegree.putIfAbsent(node, 0);
        }
        for (var node : nodes) {
            for (var parent : graph.getOrDefault(node, List.of())) {
                if (nodes.contains(parent)) {
                    reverse.computeIfAbsent(parent, k -> new ArrayList<>()).add(node);
                    indegree.put(node, indegree.getOrDefault(node, 0) + 1);
                    indegree.putIfAbsent(parent, 0);
                }
            }
        }
        var queue = indegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toCollection(ArrayDeque::new));
        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            order.add(current);
            for (var dependent : reverse.getOrDefault(current, List.of())) {
                var updated = indegree.merge(dependent, -1, Integer::sum);
                if (updated == 0) {
                    queue.addLast(dependent);
                }
            }
        }
        if (order.size() != indegree.size()) {
            return nodes.stream().sorted().toList();
        }
        return order;
    }
}
