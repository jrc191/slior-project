-- ============================================================
-- setup_database.sql
-- Script de inicialización de la base de datos PostgreSQL para SLIOR
-- Ejecutar como superusuario de PostgreSQL:
--   psql -U postgres -f scripts/setup_database.sql
-- ============================================================

-- 1. Crear usuario de la aplicación
CREATE USER slior_user WITH PASSWORD 'slior_pass';

-- 2. Crear base de datos
CREATE DATABASE slior_db
    WITH OWNER = slior_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'es_ES.UTF-8'
    LC_CTYPE = 'es_ES.UTF-8'
    TEMPLATE = template0;

-- 3. Conceder privilegios
GRANT ALL PRIVILEGES ON DATABASE slior_db TO slior_user;

-- ============================================================
-- Las tablas son creadas automáticamente por Hibernate (ddl-auto=update)
-- al arrancar el backend por primera vez.
-- ============================================================

-- Verificación (ejecutar conectado a slior_db):
-- \c slior_db slior_user
-- \dt  (debería mostrar tablas tras arrancar el backend)
