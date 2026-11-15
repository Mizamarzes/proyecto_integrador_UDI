-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- db-origen, paso 1: crear el usuario (esquema) ORIGEN
--
-- El origen es el esquema con el que trabaja la aplicación: usuarios,
-- viviendas, encuestas... Sus encuestas APROBADAS se envían al esquema
-- destino (carpeta db-destino) con el botón «Sincronizar aprobadas».
--
-- Se ejecuta como SYSTEM (o un usuario con rol DBA), desde esta carpeta.
-- Parámetro: nombre del usuario origen (la clave queda igual al nombre).
--   sqlplus system/<clave> @01_crear_usuario.sql DJGCJDCMPI
--
-- Si en el servidor ya le dieron un usuario, este paso no hace falta: basta
-- con que tenga los privilegios del GRANT de abajo (CREATE VIEW incluido,
-- para las vistas de la sincronización).
--
-- Por seguridad NO borra un usuario que ya exista (perdería sus datos).
-- Para empezar de cero, bórrelo a mano: DROP USER <origen> CASCADE;
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
WHENEVER SQLERROR EXIT FAILURE
DEFINE origen = &1

DECLARE
v_existe NUMBER;
BEGIN
SELECT COUNT(*) INTO v_existe FROM dba_users WHERE username=UPPER('&origen');
IF v_existe>0 THEN
   RAISE_APPLICATION_ERROR(-20000,'El usuario '||UPPER('&origen')
      ||' ya existe. Si quiere crearlo de nuevo: DROP USER '||UPPER('&origen')||' CASCADE;');
END IF;
END;
/

CREATE USER &origen IDENTIFIED BY &origen
DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TRIGGER, CREATE VIEW TO &origen;

PROMPT Usuario origen &origen creado (clave = nombre del usuario).
EXIT
