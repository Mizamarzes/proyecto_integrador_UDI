-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Usuarios y roles de Oracle (seguridad de la base de datos)
--
-- No confundir con las tablas usuario y rol: esas son las cuentas de la
-- aplicación (login, menú). Aquí se crean cuentas de ORACLE con el menor
-- privilegio que necesita cada una. Los nombres llevan delante el dueño del
-- esquema (<esquema>) para no chocar con los de otros equipos cuando todos
-- comparten la misma base de datos:
--
--   Usuario Oracle       Rol Oracle                 Para qué
--   ------------------   ------------------------   ------------------------------
--   <esquema>            (dueño)                    Crea tablas y PL/SQL. Solo scripts.
--   <esquema>_APP        ROL_<esquema>_APLICACION   La aplicación JavaFX se conecta
--                                                   con él: lee y escribe datos, usa
--                                                   secuencias y ejecuta funciones y
--                                                   procedimientos. No puede crear
--                                                   ni borrar objetos.
--   <esquema>_CONSULTA   ROL_<esquema>_LECTURA      Solo lectura (reportes, análisis).
--                                                   No ve la tabla usuario (tiene
--                                                   las claves).
--   <esquema>_AUDITOR    ROL_<esquema>_AUDITOR      Solo lee la auditoría y el
--                                                   historial de revisiones.
--
-- Ejemplo con el esquema DJGCJDCMPI: DJGCJDCMPI_APP, DJGCJDCMPI_CONSULTA y
-- DJGCJDCMPI_AUDITOR. Los nombres de Oracle tienen máximo 30 caracteres, así
-- que el nombre del esquema puede tener hasta 16.
--
-- Se ejecuta como SYSTEM DESPUÉS de crear el esquema y su PL/SQL (los
-- GRANT necesitan que los objetos existan). Se puede volver a ejecutar:
-- borra y vuelve a crear los usuarios y roles de ESE esquema (no toca los
-- de otros equipos). Si después se crean tablas o procedimientos nuevos,
-- hay que ejecutarlo otra vez.
--
--   docker cp docs/db oracle-xe-10g:/tmp/db
--   docker exec -it oracle-xe-10g sqlplus system/oracle @/tmp/db/seguridad/usuarios_roles.sql ENPH
--
-- El parámetro es el dueño del esquema (ENPH o el usuario de cada equipo,
-- en MAYÚSCULAS). La aplicación se conecta luego con:
--   db.usuario=<esquema>_app   db.contrasena=<esquema>_app   db.esquema=<esquema>
-- (db.esquema hace ALTER SESSION SET CURRENT_SCHEMA para no tener que
-- escribir <esquema>.tabla en cada consulta).
--
-- Claves de desarrollo iguales al nombre del usuario; en producción se
-- cambian con ALTER USER ... IDENTIFIED BY ...
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT FAILURE
DEFINE esquema = &1

-- ---------------------------------------------------------------------
-- 1. Borrar lo anterior de este esquema (si existe)
-- ---------------------------------------------------------------------
DECLARE
v_esquema VARCHAR2(30):=UPPER('&esquema');
BEGIN
IF LENGTH(v_esquema)>16 THEN
   RAISE_APPLICATION_ERROR(-20000,'El nombre del esquema tiene mas de 16 caracteres: '
      ||'ROL_'||v_esquema||'_APLICACION pasaria de 30.');
END IF;
FOR r IN (SELECT username FROM dba_users
          WHERE username IN (v_esquema||'_APP', v_esquema||'_CONSULTA', v_esquema||'_AUDITOR')) LOOP
   EXECUTE IMMEDIATE 'DROP USER '||r.username||' CASCADE';
END LOOP;
FOR r IN (SELECT role FROM dba_roles
          WHERE role IN ('ROL_'||v_esquema||'_APLICACION', 'ROL_'||v_esquema||'_LECTURA',
                         'ROL_'||v_esquema||'_AUDITOR')) LOOP
   EXECUTE IMMEDIATE 'DROP ROLE '||r.role;
END LOOP;
END;
/

-- ---------------------------------------------------------------------
-- 2. Roles
-- ---------------------------------------------------------------------
CREATE ROLE rol_&esquema._lectura;
CREATE ROLE rol_&esquema._aplicacion;
CREATE ROLE rol_&esquema._auditor;

-- ---------------------------------------------------------------------
-- 3. Privilegios de cada rol sobre los objetos del esquema
-- ---------------------------------------------------------------------
DECLARE
v_esquema VARCHAR2(30):=UPPER('&esquema');
v_aplicacion VARCHAR2(30):='ROL_'||v_esquema||'_APLICACION';
v_lectura VARCHAR2(30):='ROL_'||v_esquema||'_LECTURA';
v_auditor VARCHAR2(30):='ROL_'||v_esquema||'_AUDITOR';
v_tablas NUMBER:=0;
BEGIN
FOR t IN (SELECT table_name FROM dba_tables WHERE owner=v_esquema ORDER BY table_name) LOOP
   EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON '||v_esquema||'.'||t.table_name
      ||' TO '||v_aplicacion;
   IF t.table_name<>'USUARIO' THEN
      EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.'||t.table_name||' TO '||v_lectura;
   END IF;
   v_tablas:=v_tablas+1;
END LOOP;
IF v_tablas=0 THEN
   RAISE_APPLICATION_ERROR(-20000,'El usuario '||v_esquema||' no tiene tablas. Cree primero el esquema.');
END IF;

-- Los DAO toman los id con seq_<tabla>.NEXTVAL
FOR s IN (SELECT sequence_name FROM dba_sequences WHERE sequence_owner=v_esquema) LOOP
   EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.'||s.sequence_name||' TO '||v_aplicacion;
END LOOP;

-- Funciones y procedimientos. Las funciones también para lectura (se usan en SELECT).
FOR p IN (SELECT object_name, object_type FROM dba_objects
          WHERE owner=v_esquema AND object_type IN ('FUNCTION','PROCEDURE')) LOOP
   EXECUTE IMMEDIATE 'GRANT EXECUTE ON '||v_esquema||'.'||p.object_name||' TO '||v_aplicacion;
   IF p.object_type='FUNCTION' THEN
      EXECUTE IMMEDIATE 'GRANT EXECUTE ON '||v_esquema||'.'||p.object_name||' TO '||v_lectura;
   END IF;
END LOOP;

-- Auditor: solo la trazabilidad
EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.auditoria TO '||v_auditor;
EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.auditoria_encuesta TO '||v_auditor;

DBMS_OUTPUT.PUT_LINE('Privilegios otorgados sobre '||v_tablas||' tablas de '||v_esquema||'.');
END;
/

-- ---------------------------------------------------------------------
-- 4. Usuarios
-- ---------------------------------------------------------------------
CREATE USER &esquema._app IDENTIFIED BY &esquema._app
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp;
GRANT CREATE SESSION TO &esquema._app;
GRANT rol_&esquema._aplicacion TO &esquema._app;

CREATE USER &esquema._consulta IDENTIFIED BY &esquema._consulta
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp;
GRANT CREATE SESSION TO &esquema._consulta;
GRANT rol_&esquema._lectura TO &esquema._consulta;

CREATE USER &esquema._auditor IDENTIFIED BY &esquema._auditor
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp;
GRANT CREATE SESSION TO &esquema._auditor;
GRANT rol_&esquema._auditor TO &esquema._auditor;

WHENEVER SQLERROR CONTINUE

-- ---------------------------------------------------------------------
-- 5. Resumen
-- ---------------------------------------------------------------------
COLUMN grantee FORMAT A30
COLUMN granted_role FORMAT A30
SELECT grantee, granted_role FROM dba_role_privs
WHERE grantee IN (UPPER('&esquema._APP'), UPPER('&esquema._CONSULTA'), UPPER('&esquema._AUDITOR'))
ORDER BY grantee;

COLUMN privilege FORMAT A10
SELECT grantee, privilege, COUNT(*) objetos FROM dba_tab_privs
WHERE grantee IN (UPPER('ROL_&esquema._APLICACION'), UPPER('ROL_&esquema._LECTURA'),
                  UPPER('ROL_&esquema._AUDITOR'))
GROUP BY grantee, privilege
ORDER BY grantee, privilege;

UNDEFINE esquema
UNDEFINE 1
