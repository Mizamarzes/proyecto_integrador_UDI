-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- db-destino, paso 1: crear el usuario (esquema) DESTINO
--
-- El destino es el esquema central que recibe las encuestas APROBADAS del
-- origen (carpeta db-origen), como el esquema principal de la guía
-- docs/referencias/SincronizaciónOracle(EsquemaAesquema).pdf. La
-- aplicación no inicia sesión en él: solo ejecuta su procedimiento
-- pr_sincronizar_aprobadas.
--
-- Se ejecuta como SYSTEM (o un usuario con rol DBA), desde esta carpeta.
-- Parámetro: nombre del usuario destino (la clave queda igual al nombre).
--   sqlplus system/<clave> @01_crear_usuario.sql DJGCJDCMPI_CENTRAL
--
-- Debe existir ANTES de instalar el origen: las vistas del origen se le
-- conceden al destino. Si en el servidor ya le dieron un usuario, este paso
-- no hace falta: basta con que tenga los privilegios del GRANT de abajo.
--
-- Por seguridad NO borra un usuario que ya exista (perdería sus datos).
-- Para empezar de cero, bórrelo a mano: DROP USER <destino> CASCADE;
-- (si lo hace, vuelva a ejecutar db-origen/06_vistas_sincronizacion.sql:
-- el origen pierde los GRANT que le había dado).
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
WHENEVER SQLERROR EXIT FAILURE
DEFINE destino = &1

DECLARE
v_existe NUMBER;
BEGIN
SELECT COUNT(*) INTO v_existe FROM dba_users WHERE username=UPPER('&destino');
IF v_existe>0 THEN
   RAISE_APPLICATION_ERROR(-20000,'El usuario '||UPPER('&destino')
      ||' ya existe. Si quiere crearlo de nuevo: DROP USER '||UPPER('&destino')||' CASCADE;');
END IF;
END;
/

CREATE USER &destino IDENTIFIED BY &destino
DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TRIGGER TO &destino;

PROMPT Usuario destino &destino creado (clave = nombre del usuario).
EXIT
