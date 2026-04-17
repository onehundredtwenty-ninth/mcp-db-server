package com.onehundredtwentyninth.mcpdb.validation;

import com.onehundredtwentyninth.mcpdb.config.DatabaseProperties;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class SqlSafetyValidator {

    private static final Set<String> FORBIDDEN_TOKENS = Set.of(
            "merge", "update", "delete ", "truncate", "alter", "drop", "create",
            "exec ", "execute", "grant", "revoke", "deny", "dbcc", "backup",
            "restore", "openrowset", "opendatasource", "xp_", "sp_"
    );

    private final DatabaseProperties properties;

    public SqlSafetyValidator(DatabaseProperties properties) {
        this.properties = properties;
    }

    public Select validateSelect(String sql) {
        var statement = parseSingleStatement(sql);
        if (!(statement instanceof Select select)) {
            throw new SqlValidationException("Разрешён только SELECT");
        }
        if (select.getPlainSelect() == null) {
            throw new SqlValidationException("Разрешён только простой SELECT без UNION/INTERSECT/EXCEPT");
        }
        validateTables(sql);
        validateTextTokens(sql);
        var lower = normalize(sql);
        if (lower.contains(" select into ") || lower.contains(" into #") || lower.contains(" into [#")) {
            throw new SqlValidationException("SELECT INTO запрещён");
        }
        if (lower.contains(" for xml ") || lower.contains(" for json ")) {
            throw new SqlValidationException("FOR XML / FOR JSON запрещены");
        }
        return select;
    }

    public void validateInsert(String sql) {
        var statement = parseSingleStatement(sql);
        if (!(statement instanceof Insert)) {
            throw new SqlValidationException("Разрешён только INSERT");
        }
        validateTables(sql);
        validateTextTokens(sql);
    }

    public String enforceTopLimit(String sql, int limit) {
        var select = validateSelect(sql);
        var plainSelect = select.getPlainSelect();
        if (plainSelect != null && plainSelect.getTop() == null) {
            var trimmed = sql.trim();
            var lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("select distinct")) {
                return trimmed.replaceFirst("(?i)^select\\s+distinct\\s+", "SELECT DISTINCT TOP " + limit + " ");
            }
            return trimmed.replaceFirst("(?i)^select\\s+", "SELECT TOP " + limit + " ");
        }
        return sql;
    }

    private Statement parseSingleStatement(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new SqlValidationException("SQL пустой");
        }
        var trimmed = sql.trim();
        if (trimmed.contains(";")) {
            throw new SqlValidationException("Разрешён ровно один SQL statement без ';'");
        }
        try {
            return CCJSqlParserUtil.parse(trimmed);
        } catch (Exception e) {
            throw new SqlValidationException("Не удалось разобрать SQL: " + e.getMessage(), e);
        }
    }

    private void validateTextTokens(String sql) {
        var lower = normalize(sql);
        for (var token : FORBIDDEN_TOKENS) {
            if (lower.contains(token)) {
                throw new SqlValidationException("Обнаружен запрещённый SQL token: " + token.trim());
            }
        }
        if (lower.contains("--") || lower.contains("/*") || lower.contains("*/")) {
            throw new SqlValidationException("SQL comments запрещены");
        }
    }

    private void validateTables(String sql) {
        var lower = sql.toLowerCase(Locale.ROOT);
        var allowedSchemas = properties.schemaWhitelist().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
        for (var fragment : lower.split("\\s+")) {
            if (fragment.contains(".")) {
                var parts = fragment
                        .replace("[", "")
                        .replace("]", "")
                        .replace(",", "")
                        .replace("(", "")
                        .replace(")", "")
                        .split("\\.");
                if (parts.length >= 2) {
                    var schema = parts[parts.length - 2];
                    if (schema.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_')
                            && !allowedSchemas.contains(schema)) {
                        throw new SqlValidationException("Схема '%s' не входит в whitelist".formatted(schema));
                    }
                }
            }
        }
    }

    private String normalize(String sql) {
        return (" " + sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ") + " ");
    }
}
