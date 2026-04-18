package com.onehundredtwentyninth.mcpdb.tool;

import com.onehundredtwentyninth.mcpdb.model.ConstraintsInfoModel;
import com.onehundredtwentyninth.mcpdb.model.ForeignKeysModel;
import com.onehundredtwentyninth.mcpdb.model.InsertDryRunModel;
import com.onehundredtwentyninth.mcpdb.model.SafeSelectModel;
import com.onehundredtwentyninth.mcpdb.model.SchemaOverviewModel;
import com.onehundredtwentyninth.mcpdb.model.SearchSchemaModel;
import com.onehundredtwentyninth.mcpdb.model.TableDependenciesModel;
import com.onehundredtwentyninth.mcpdb.model.TableDescriptionModel;
import com.onehundredtwentyninth.mcpdb.service.DbMetadataService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class DatabaseTools {

    private final DbMetadataService service;

    public DatabaseTools(DbMetadataService service) {
        this.service = service;
    }

    @Tool(description = "schema_overview: показывает список доступных таблиц и базовую информацию по ним")
    public SchemaOverviewModel schemaOverview() {
        return service.schemaOverview();
    }

    @Tool(description = "describe_table: показывает колонки таблицы, их типы данных и признак nullable")
    public TableDescriptionModel describeTable(
            @ToolParam(description = "Схема") String schema,
            @ToolParam(description = "Имя таблицы") String table) {
        return service.describeTable(schema, table);
    }

    @Tool(description = "foreign_keys: показывает внешние ключи таблицы — на что она ссылается и кто ссылается на неё")
    public ForeignKeysModel foreignKeys(
            @ToolParam(description = "Схема") String schema,
            @ToolParam(description = "Имя таблицы") String table) {
        return service.foreignKeys(schema, table);
    }

    @Tool(description = "constraints_info: показывает ограничения таблицы — PRIMARY KEY, UNIQUE, CHECK, DEFAULT, identity и computed колонки")
    public ConstraintsInfoModel constraintsInfo(
            @ToolParam(description = "Схема") String schema,
            @ToolParam(description = "Имя таблицы") String table) {
        return service.constraintsInfo(schema, table);
    }

    @Tool(description = "table_dependencies: показывает зависимости таблицы и помогает понять, какие сущности нужно создать раньше")
    public TableDependenciesModel tableDependencies(
            @ToolParam(description = "Схема") String schema,
            @ToolParam(description = "Имя таблицы") String table) {
        return service.tableDependencies(schema, table);
    }

    @Tool(description = "insert_dry_run: выполняет INSERT внутри транзакции с обязательным rollback для проверки корректности запроса")
    public InsertDryRunModel insertDryRun(
            @ToolParam(description = "Один INSERT statement без ;") String sql) {
        return service.insertDryRun(sql);
    }

    @Tool(description = "safe_select: выполняет произвольный SELECT-запрос")
    public SafeSelectModel safeSelect(
            @ToolParam(description = "Один SELECT statement без ;") String sql,
            @ToolParam(description = "Лимит строк, будет дополнительно ограничен сервером") Integer limit) {
        return service.safeSelect(sql, limit);
    }

    @Tool(description = "search_schema: поиск таблиц и колонок по подстроке")
    public SearchSchemaModel searchSchema(
            @ToolParam(description = "Фрагмент имени таблицы или колонки") String term) {
        return service.searchSchema(term);
    }
}
