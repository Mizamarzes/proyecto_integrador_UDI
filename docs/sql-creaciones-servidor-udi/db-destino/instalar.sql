-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- db-destino: instala el esquema DESTINO (central) completo (Oracle 10g)
--
-- El destino recibe las encuestas APROBADAS del origen (carpeta
-- db-origen) con el procedimiento pr_sincronizar_aprobadas, que la
-- aplicación ejecuta desde el botón «Sincronizar aprobadas» de
-- Verificación (propiedad db.esquemaDestino). Nace con la misma estructura
-- que el origen pero SOLO con los catálogos: sin roles, usuarios ni
-- encuestas de prueba; los roles, usuarios, encuestadores, viviendas,
-- hogares y encuestas le llegan con la sincronización. Esta carpeta se
-- instala sola, sin archivos de otras carpetas:
--   01_crear_usuario.sql              (SYSTEM) crea el usuario destino
--   02_tablas.sql                     tablas, llaves, índices y secuencias
--                                     (iguales a las del origen)
--   03_catalogos.sql                  grupos de alimentos, lugares de compra
--                                     y ubicaciones (mismos ids del origen)
--   04_auditoria.sql                  tabla auditoria y seq_auditoria
--   plsql/*.sql                       funciones, procedimientos, disparadores
--                                     y reportes (los reportes sirven para
--                                     consultar lo consolidado)
--   05_procedimiento_sincronizacion.sql  pr_sincronizar_aprobadas y su GRANT
--
-- Orden completo con el origen: ver el encabezado de db-origen/instalar.sql.
-- Este script va DESPUÉS de instalar el origen (el procedimiento lee sus
-- vistas). En el contenedor de desarrollo:
--   docker cp docs/sql-creaciones-servidor-udi/db-destino oracle-xe-10g:/tmp/db-destino
--   docker exec -it -w /tmp/db-destino -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g \
--     sqlplus DJGCJDCMPI_CENTRAL/DJGCJDCMPI_CENTRAL @instalar.sql DJGCJDCMPI
-- Se ejecuta DESDE la carpeta (-w o cd): así @@ encuentra los archivos.
--
-- Este script como dueño del destino. Parámetro: el 1er argumento = nombre
-- del esquema origen. Solo funciona en un esquema vacío y se detiene en el
-- primer error.
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT FAILURE ROLLBACK
DEFINE origen = &1

PROMPT
PROMPT == Revisando que el esquema este vacio y que el origen &origen ya este instalado
DECLARE
v_cantidad NUMBER;
BEGIN
SELECT COUNT(*) INTO v_cantidad FROM user_tables;
IF v_cantidad>0 THEN
   RAISE_APPLICATION_ERROR(-20000,'El esquema '||USER||' ya tiene '||v_cantidad
      ||' tablas: instalar.sql solo funciona en un esquema vacio.');
END IF;
SELECT COUNT(*) INTO v_cantidad FROM all_views
WHERE owner=UPPER('&origen') AND view_name LIKE 'VIS\_SINC\_%' ESCAPE '\';
IF v_cantidad<12 THEN
   RAISE_APPLICATION_ERROR(-20000,'Este usuario no ve las 12 vistas VIS_SINC_* de '||UPPER('&origen')
      ||' (ve '||v_cantidad||'). Instale primero db-origen con este destino como parametro.');
END IF;
END;
/

PROMPT == 02 Tablas, llaves, indices y secuencias
@@02_tablas.sql
PROMPT == 03 Catalogos y ubicaciones
@@03_catalogos.sql
PROMPT == 04 Auditoria
@@04_auditoria.sql
PROMPT == PL/SQL: funciones, procedimientos, disparadores y reportes
@@plsql/funciones.sql
@@plsql/procedimientos.sql
@@plsql/disparadores.sql
@@plsql/reportes.sql

SET DEFINE ON
PROMPT == 05 Procedimiento de sincronizacion (lee las vistas de &origen)
@@05_procedimiento_sincronizacion.sql &origen

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
PROMPT Destino instalado. Configure db.esquemaDestino con este esquema en la aplicacion.
EXIT
