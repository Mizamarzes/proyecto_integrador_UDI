-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- db-origen: instala el esquema ORIGEN completo (Oracle 10g)
--
-- El origen es el esquema de la aplicación. Esta carpeta se instala sola,
-- sin archivos de otras carpetas:
--   01_crear_usuario.sql         (SYSTEM) crea el usuario origen
--   02_tablas.sql                tablas, llaves, índices y secuencias
--   03_catalogos.sql             grupos de alimentos, lugares de compra y
--                                ubicaciones (ubicaciones/*.sql)
--   04_datos_iniciales.sql       roles, módulos, permisos, usuarios y casos
--                                de prueba (clave de todos: 123456)
--   05_auditoria.sql             tabla auditoria y seq_auditoria
--   plsql/*.sql                  funciones, procedimientos, disparadores
--                                (validación y auditoría) y reportes
--   06_vistas_sincronizacion.sql vistas vis_sinc_* y GRANT al destino
--   usuarios_roles.sql           (SYSTEM, opcional) usuarios <origen>_APP,
--                                _CONSULTA y _AUDITOR
--   prueba_sincronizacion.sql    prueba de la sincronización (ROLLBACK)
--
-- ORDEN COMPLETO (origen y destino), cada comando desde su carpeta y con
-- NLS_LANG=AMERICAN_AMERICA.AL32UTF8 (sin él se dañan las tildes):
--   1. db-origen:  sqlplus system/<clave> @01_crear_usuario.sql DJGCJDCMPI
--   2. db-destino: sqlplus system/<clave> @01_crear_usuario.sql DJGCJDCMPI_CENTRAL
--   3. db-origen:  sqlplus DJGCJDCMPI/DJGCJDCMPI @instalar.sql DJGCJDCMPI_CENTRAL
--   4. db-origen:  sqlplus system/<clave> @usuarios_roles.sql DJGCJDCMPI      (opcional)
--   5. db-destino: sqlplus DJGCJDCMPI_CENTRAL/DJGCJDCMPI_CENTRAL @instalar.sql DJGCJDCMPI
--   6. db-origen:  sqlplus DJGCJDCMPI/DJGCJDCMPI @prueba_sincronizacion.sql DJGCJDCMPI_CENTRAL
-- El destino va después del origen porque su procedimiento lee las vistas
-- del origen, y el usuario destino debe existir antes porque las vistas se
-- le conceden.
--
-- En el contenedor de desarrollo:
--   docker cp docs/sql-creaciones-servidor-udi/db-origen oracle-xe-10g:/tmp/db-origen
--   docker exec -it -w /tmp/db-origen -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g \
--     sqlplus DJGCJDCMPI/DJGCJDCMPI @instalar.sql DJGCJDCMPI_CENTRAL
-- Se ejecuta DESDE la carpeta (-w o cd): así @@ encuentra los archivos.
--
-- Este script como dueño del origen. Parámetro: el 1er argumento = nombre
-- del esquema destino. Solo funciona en un esquema vacío (los datos
-- iniciales usan ids fijos) y se detiene en el primer error.
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT FAILURE ROLLBACK
DEFINE destino = &1

PROMPT
PROMPT == Revisando que el esquema este vacio y que exista el destino &destino
DECLARE
v_cantidad NUMBER;
BEGIN
SELECT COUNT(*) INTO v_cantidad FROM user_tables;
IF v_cantidad>0 THEN
   RAISE_APPLICATION_ERROR(-20000,'El esquema '||USER||' ya tiene '||v_cantidad
      ||' tablas: instalar.sql solo funciona en un esquema vacio.');
END IF;
SELECT COUNT(*) INTO v_cantidad FROM all_users WHERE username=UPPER('&destino');
IF v_cantidad=0 THEN
   RAISE_APPLICATION_ERROR(-20000,'No existe el usuario destino '||UPPER('&destino')
      ||'. Creelo primero con db-destino/01_crear_usuario.sql.');
END IF;
END;
/

PROMPT == 02 Tablas, llaves, indices y secuencias
@@02_tablas.sql
PROMPT == 03 Catalogos y ubicaciones
@@03_catalogos.sql
PROMPT == 04 Datos iniciales y de prueba
@@04_datos_iniciales.sql
PROMPT == 05 Auditoria
@@05_auditoria.sql
PROMPT == PL/SQL: funciones, procedimientos, disparadores y reportes
@@plsql/funciones.sql
@@plsql/procedimientos.sql
@@plsql/disparadores.sql
@@plsql/reportes.sql

SET DEFINE ON
PROMPT == 06 Vistas de sincronizacion (GRANT a &destino)
@@06_vistas_sincronizacion.sql &destino

PROMPT == Revisando objetos invalidos
DECLARE
v_lista VARCHAR2(4000);
BEGIN
FOR r IN (SELECT object_type, object_name FROM user_objects
          WHERE status='INVALID' ORDER BY object_type, object_name) LOOP
   v_lista:=SUBSTR(v_lista||' '||r.object_type||' '||r.object_name||';',1,3900);
END LOOP;
IF v_lista IS NOT NULL THEN
   RAISE_APPLICATION_ERROR(-20000,'Quedaron objetos invalidos (vea SHOW ERRORS):'||v_lista);
END IF;
END;
/

PROMPT
PROMPT Origen instalado. Siga con db-destino/instalar.sql (parametro: este esquema).
EXIT
