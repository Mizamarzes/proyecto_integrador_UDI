-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Usuarios y roles de Oracle (seguridad de la base de datos)
--
-- No confundir con las tablas usuario y rol: esas son las cuentas de la
-- aplicación (login, menú). Aquí se crean cuentas de ORACLE con el menor
-- privilegio que necesita cada una:
--
--   Usuario Oracle   Rol Oracle            Para qué
--   --------------   -------------------   -----------------------------------
--   <esquema>        (dueño)               Crea tablas y PL/SQL. Solo scripts.
--   ENPH_APP         ROL_ENPH_APLICACION   La aplicación JavaFX se conecta con él:
--                                          lee y escribe datos, usa secuencias y
--                                          ejecuta funciones y procedimientos.
--                                          No puede crear ni borrar objetos.
--   ENPH_CONSULTA    ROL_ENPH_LECTURA      Solo lectura (reportes, análisis). No ve
--                                          la tabla usuario (tiene las claves).
--   ENPH_AUDITOR     ROL_ENPH_AUDITOR      Solo lee la auditoría y el historial
--                                          de revisiones.
--
-- Se ejecuta como SYSTEM DESPUÉS de crear el esquema y su PL/SQL (los
-- GRANT necesitan que los objetos existan). Se puede volver a ejecutar:
-- borra y vuelve a crear usuarios y roles. Si después se crean tablas o
-- procedimientos nuevos, hay que ejecutarlo otra vez.
--
--   docker cp docs/db oracle-xe-10g:/tmp/db
--   docker exec -it oracle-xe-10g sqlplus system/oracle @/tmp/db/seguridad/usuarios_roles.sql ENPH
--
-- El parámetro es el dueño del esquema (ENPH o el usuario de cada equipo,
-- en MAYÚSCULAS). La aplicación se conecta luego con:
--   db.usuario=enph_app   db.contrasena=enph_app   db.esquema=<esquema>
-- (db.esquema hace ALTER SESSION SET CURRENT_SCHEMA para no tener que
-- escribir <esquema>.tabla en cada consulta).
--
-- Claves de desarrollo iguales al nombre del usuario; en producción se
-- cambian con ALTER USER ... IDENTIFIED BY ...
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON
DEFINE esquema = &1

-- ---------------------------------------------------------------------
-- 1. Borrar lo anterior (si existe)
-- ---------------------------------------------------------------------
BEGIN
FOR r IN (SELECT username FROM dba_users
          WHERE username IN ('ENPH_APP','ENPH_CONSULTA','ENPH_AUDITOR')) LOOP
   EXECUTE IMMEDIATE 'DROP USER '||r.username||' CASCADE';
END LOOP;
FOR r IN (SELECT role FROM dba_roles
          WHERE role IN ('ROL_ENPH_APLICACION','ROL_ENPH_LECTURA','ROL_ENPH_AUDITOR')) LOOP
   EXECUTE IMMEDIATE 'DROP ROLE '||r.role;
END LOOP;
END;
/

-- ---------------------------------------------------------------------
-- 2. Roles
-- ---------------------------------------------------------------------
CREATE ROLE rol_enph_lectura;
CREATE ROLE rol_enph_aplicacion;
CREATE ROLE rol_enph_auditor;

-- ---------------------------------------------------------------------
-- 3. Privilegios de cada rol sobre los objetos del esquema
-- ---------------------------------------------------------------------
DECLARE
v_esquema VARCHAR2(30):=UPPER('&esquema');
v_tablas NUMBER:=0;
BEGIN
FOR t IN (SELECT table_name FROM dba_tables WHERE owner=v_esquema ORDER BY table_name) LOOP
   EXECUTE IMMEDIATE 'GRANT SELECT, INSERT, UPDATE, DELETE ON '||v_esquema||'.'||t.table_name
      ||' TO rol_enph_aplicacion';
   IF t.table_name<>'USUARIO' THEN
      EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.'||t.table_name||' TO rol_enph_lectura';
   END IF;
   v_tablas:=v_tablas+1;
END LOOP;
IF v_tablas=0 THEN
   RAISE_APPLICATION_ERROR(-20000,'El usuario '||v_esquema||' no tiene tablas. Cree primero el esquema.');
END IF;

-- Los DAO toman los id con seq_<tabla>.NEXTVAL
FOR s IN (SELECT sequence_name FROM dba_sequences WHERE sequence_owner=v_esquema) LOOP
   EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.'||s.sequence_name||' TO rol_enph_aplicacion';
END LOOP;

-- Funciones y procedimientos. Las funciones también para lectura (se usan en SELECT).
FOR p IN (SELECT object_name, object_type FROM dba_objects
          WHERE owner=v_esquema AND object_type IN ('FUNCTION','PROCEDURE')) LOOP
   EXECUTE IMMEDIATE 'GRANT EXECUTE ON '||v_esquema||'.'||p.object_name||' TO rol_enph_aplicacion';
   IF p.object_type='FUNCTION' THEN
      EXECUTE IMMEDIATE 'GRANT EXECUTE ON '||v_esquema||'.'||p.object_name||' TO rol_enph_lectura';
   END IF;
END LOOP;

-- Auditor: solo la trazabilidad
EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.auditoria TO rol_enph_auditor';
EXECUTE IMMEDIATE 'GRANT SELECT ON '||v_esquema||'.auditoria_encuesta TO rol_enph_auditor';

DBMS_OUTPUT.PUT_LINE('Privilegios otorgados sobre '||v_tablas||' tablas de '||v_esquema||'.');
END;
/

-- ---------------------------------------------------------------------
-- 4. Usuarios
-- ---------------------------------------------------------------------
CREATE USER enph_app IDENTIFIED BY enph_app
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp;
GRANT CREATE SESSION TO enph_app;
GRANT rol_enph_aplicacion TO enph_app;

CREATE USER enph_consulta IDENTIFIED BY enph_consulta
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp;
GRANT CREATE SESSION TO enph_consulta;
GRANT rol_enph_lectura TO enph_consulta;

CREATE USER enph_auditor IDENTIFIED BY enph_auditor
  DEFAULT TABLESPACE users TEMPORARY TABLESPACE temp;
GRANT CREATE SESSION TO enph_auditor;
GRANT rol_enph_auditor TO enph_auditor;

-- ---------------------------------------------------------------------
-- 5. Resumen
-- ---------------------------------------------------------------------
COLUMN grantee FORMAT A22
COLUMN granted_role FORMAT A22
SELECT grantee, granted_role FROM dba_role_privs
WHERE grantee IN ('ENPH_APP','ENPH_CONSULTA','ENPH_AUDITOR')
ORDER BY grantee;

COLUMN privilege FORMAT A10
SELECT grantee, privilege, COUNT(*) objetos FROM dba_tab_privs
WHERE grantee IN ('ROL_ENPH_APLICACION','ROL_ENPH_LECTURA','ROL_ENPH_AUDITOR')
GROUP BY grantee, privilege
ORDER BY grantee, privilege;

UNDEFINE esquema
UNDEFINE 1
