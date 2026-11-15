-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Prueba de los usuarios y roles de Oracle (seguridad/usuarios_roles.sql):
-- <esquema>_APP, <esquema>_CONSULTA y <esquema>_AUDITOR.
--
--   docker exec -it oracle-xe-10g sqlplus /nolog @/tmp/db/pruebas/pruebas_seguridad.sql ENPH
--
-- El parámetro es el dueño del esquema, en MAYÚSCULAS. Se conecta con cada
-- usuario e intenta operaciones permitidas y prohibidas. Los errores
-- ORA-00942 (la tabla no existe para ese usuario) y ORA-01031 (privilegios
-- insuficientes) son lo esperado en las operaciones prohibidas.
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
SET FEEDBACK ON
SET LINESIZE 120
SET TAB OFF
COLUMN caso FORMAT A10
COLUMN identificador FORMAT A15
DEFINE esquema = &1

PROMPT
PROMPT ==== &esquema._APP (la aplicación): lee, escribe y ejecuta, pero no crea objetos
CONNECT &esquema._app/&esquema._app
ALTER SESSION SET CURRENT_SCHEMA = &esquema;
SELECT COUNT(*) encuestas FROM encuesta;
SELECT fn_codigo_encuesta(MIN(id)) caso, fn_gasto_total_encuesta(MIN(id)) gasto FROM encuesta;
EXEC pr_iniciar_sesion(1)
SELECT SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER') identificador FROM dual;
EXEC pr_cerrar_sesion
PROMPT -- Prohibido: crear una tabla (ORA-01031)
CREATE TABLE prueba_app (id NUMBER);
PROMPT -- Prohibido: borrar una tabla del esquema (ORA-01031)
DROP TABLE &esquema..region;

PROMPT
PROMPT ==== &esquema._CONSULTA (solo lectura)
CONNECT &esquema._consulta/&esquema._consulta
ALTER SESSION SET CURRENT_SCHEMA = &esquema;
SELECT COUNT(*) viviendas FROM vivienda;
SELECT fn_codigo_encuesta(id) caso, fn_gasto_total_encuesta(id) gasto FROM encuesta WHERE ROWNUM<=3;
PROMPT -- Permitido: los reportes son funciones
SET SERVEROUTPUT ON SIZE 1000000 FORMAT WRAPPED
SET LINESIZE 32767
SET TRIMOUT ON
EXEC DBMS_OUTPUT.PUT_LINE(fn_reporte_gasto_ubicacion)
SET LINESIZE 120
PROMPT -- Prohibido: modificar datos (ORA-01031)
UPDATE region SET nombre = nombre WHERE ROWNUM = 1;
PROMPT -- Prohibido: ver la tabla usuario, que guarda las claves (ORA-00942)
SELECT COUNT(*) FROM usuario;
PROMPT -- Prohibido: ejecutar procedimientos (PLS-00201 / ORA-06550)
EXEC pr_reabrir_encuesta(1)

PROMPT
PROMPT ==== &esquema._AUDITOR (solo la trazabilidad)
CONNECT &esquema._auditor/&esquema._auditor
ALTER SESSION SET CURRENT_SCHEMA = &esquema;
SELECT COUNT(*) movimientos FROM auditoria;
SELECT COUNT(*) revisiones FROM auditoria_encuesta;
PROMPT -- Prohibido: leer las encuestas (ORA-00942)
SELECT COUNT(*) FROM encuesta;
PROMPT -- Prohibido: borrar la auditoría (ORA-01031)
DELETE FROM auditoria;

DISCONNECT
UNDEFINE esquema
UNDEFINE 1
EXIT
