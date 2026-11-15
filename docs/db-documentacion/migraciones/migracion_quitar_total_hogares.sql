-- =====================================================================
-- Migración: quitar vivienda.total_hogares
--
-- El total de hogares de la vivienda (A.12) ya no se escribe a mano: es la
-- cantidad de hogares registrados, y ViviendaDAO lo calcula con COUNT.
-- Para esquemas creados antes de este cambio. Se puede ejecutar más de una
-- vez (si la columna ya no existe, no hace nada).
--
-- docker cp docs/db/migracion_quitar_total_hogares.sql oracle-xe-10g:/tmp/
-- docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<clave> @/tmp/migracion_quitar_total_hogares.sql
-- =====================================================================

DECLARE
  existe NUMBER;
BEGIN
  SELECT COUNT(*) INTO existe FROM user_tab_columns
   WHERE table_name = 'VIVIENDA' AND column_name = 'TOTAL_HOGARES';
  IF existe > 0 THEN
    -- El CHECK ck_vivienda_total_hogares se elimina junto con la columna
    EXECUTE IMMEDIATE 'ALTER TABLE vivienda DROP COLUMN total_hogares';
  END IF;
END;
/

EXIT;
