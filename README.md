# MSSQL MCP Server

MCP сервер, который помогает агенту исследовать структуру MS SQL БД и проверять `INSERT` без сохранения изменений.
Необходим для повышения качества генерации кода подготовки данных перед выполнением автотеста

## Технологии

- Java 17
- Gradle 
- Spring Boot
- Spring AI MCP Starter
- JDBC driver `jTDS`

## Быстрый старт

### 1. Сборка JAR

```powershell
./gradlew clean bootJar
```

Готовый JAR: `build/libs/mcp-db-server.jar`

### 2. Переменные окружения

- `MCP_DB_HOST` (по умолчанию `localhost`)
- `MCP_DB_PORT` (по умолчанию `1433`)
- `MCP_DB_NAME` (по умолчанию `master`)
- `MCP_DB_USERNAME` (по умолчанию `sa`)
- `MCP_DB_PASSWORD` (по умолчанию `sa`) - должен быть задан через переменную окружения. В mcp.json пароль не хранить
- `MCP_DB_HOST_WHITELIST` (по умолчанию `localhost,127.0.0.1`)
- `MCP_DB_SCHEMA_WHITELIST` (по умолчанию `dbo`)
- `MCP_DB_CONNECT_TIMEOUT_SECONDS` (по умолчанию `5`)
- `MCP_DB_QUERY_TIMEOUT_SECONDS` (по умолчанию `15`)
- `MCP_DB_MAX_SELECT_ROWS` (по умолчанию `200`)
- `MCP_DB_MAX_TEXT_LENGTH` (по умолчанию `1000`)

### 3. Запуск

```powershell
java -jar build/libs/mcp-db-server.jar
```

Поддерживаются режимы работы:

- `stdio` для локальной работы MCP клиента

## Настройка через mcp.json

Ниже примеры конфигурации MCP-клиента через файл `mcp.json`.

### Вариант 1: локальный запуск по stdio

```json
{
  "mcpServers": {
    "mssql-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "C:/Users/UserName/IdeaProjects/mcp/mssql-mcp/build/libs/mcp-db-server.jar"
      ],
      "env": {
        "MCP_DB_HOST": "localhost",
        "MCP_DB_PORT": "1433",
        "MCP_DB_NAME": "my_db",
        "MCP_DB_USERNAME": "sa",
        "MCP_DB_HOST_WHITELIST": "localhost",
        "MCP_DB_SCHEMA_WHITELIST": "dbo",
        "MCP_DB_MAX_SELECT_ROWS": "100"
      }
    }
  }
}
```

## Доступные MCP tools

- `schema_overview` — показывает список доступных таблиц и базовую информацию по ним
- `describe_table` — показывает колонки таблицы, их типы данных и признак nullable; показывает внешние ключи таблицы —
  на что она ссылается и кто ссылается на неё; показывает ограничения таблицы — PRIMARY KEY, UNIQUE, CHECK, DEFAULT,
  identity и computed колонки
- `table_dependencies` — показывает зависимости таблицы и помогает понять, какие сущности нужно создать раньше
- `insert_dry_run` — выполняет INSERT внутри транзакции с обязательным rollback для проверки корректности запроса
- `safe_select` — выполняет произвольный SELECT-запрос
- `search_schema` — поиск таблиц и колонок по подстроке

## Политика безопасности SQL

- Разрешены только:
    - `SELECT` (read-only операции)
    - `INSERT` только внутри `insert_dry_run`
- Запрещены:
    - multi-statement (`;`)
    - комментарии (`--`, `/* */`)
    - `UPDATE`, `DELETE`, `MERGE`, `DROP`, `ALTER`, `TRUNCATE`, `EXEC`, временные таблицы и другие опасные конструкции
- Для `insert_dry_run` всегда выполняется `ROLLBACK`, даже при ошибках выполнения.
