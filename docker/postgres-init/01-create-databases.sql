-- Creates one database per business service inside the single PostgreSQL 16
-- container defined in docker-compose.yml (database-per-service, shared instance).
-- Runs automatically on first container start only: the official postgres image
-- executes every *.sql/*.sh file found in /docker-entrypoint-initdb.d/, in
-- lexical order, but only when the data directory is empty. On subsequent
-- restarts with an existing volume, this script is not re-run, so plain
-- CREATE DATABASE statements (no IF NOT EXISTS, not supported by PostgreSQL
-- for CREATE DATABASE) are safe here.
--
-- notification-service is deliberately absent: it owns no database (see
-- README, "Data model").

CREATE DATABASE auth_db;
CREATE DATABASE catalog_db;
CREATE DATABASE customer_db;
CREATE DATABASE order_db;
