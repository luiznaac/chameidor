-- External systems registry: callers of task registration authenticate with an opaque Bearer
-- token whose SHA-256 hex digest lives here. Rotation/deactivation are plain UPDATEs. Generated
-- by ./gradlew :persistence:generateMigrationScript -Pname=V2__external_systems, not hand-written;
-- MigrationSchemaTest keeps this file and the Exposed *Table objects in step going forward.

CREATE TABLE IF NOT EXISTS external_systems (`name` VARCHAR(50) PRIMARY KEY, token_hash VARCHAR(64) NOT NULL, active BOOLEAN NOT NULL);
ALTER TABLE external_systems ADD CONSTRAINT external_systems_token_hash_unique UNIQUE (token_hash);
