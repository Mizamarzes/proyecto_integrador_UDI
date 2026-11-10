-- =====================================================================
-- Reemplaza el seed de prueba de ubicación (10 regiones, 10 departamentos,
-- 10 municipios) por la división político-administrativa completa:
-- regiones.sql, departamentos.sql y municipios.sql.
--
-- Solo para una base creada con el seed anterior de esquema_oracle10g.sql:
-- reasigna las viviendas de los 10 municipios viejos a sus nuevos ids y se
-- detiene si encuentra otros municipios.
--
-- Uso (copiar la carpeta completa, el script llama a los otros con @@):
--   docker cp docs/db/inserts oracle-xe-10g:/tmp/inserts
--   docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g \
--     sqlplus enph/enph @/tmp/inserts/recargar_ubicaciones.sql
-- =====================================================================

SET DEFINE OFF
WHENEVER SQLERROR EXIT ROLLBACK

-- 1. Verificar que la base tiene exactamente el seed anterior.
DECLARE
  v_esperados NUMBER;
  v_total     NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_total FROM municipio;
  -- Se compara por id y por nombres sin tilde: en bases cargadas sin
  -- NLS_LANG las tildes quedan dañadas (p. ej. 'Gir??n').
  SELECT COUNT(*) INTO v_esperados FROM municipio
   WHERE (id, nombre) IN ((1, 'Bucaramanga'), (2, 'Floridablanca'), (4, 'Piedecuesta'),
                          (6, 'Tunja'), (8, 'Barranquilla'), (9, 'Cartagena'), (10, 'Cali'))
      OR (id = 3 AND nombre LIKE 'Gir%n')
      OR (id = 5 AND nombre LIKE 'C%cuta')
      OR (id = 7 AND nombre LIKE 'Medell%n');
  IF v_total > 10 THEN
    RAISE_APPLICATION_ERROR(-20001, 'La tabla municipio ya tiene ' || v_total || ' filas: el script ya se ejecuto o la base no tiene el seed anterior.');
  ELSIF v_esperados <> v_total THEN
    RAISE_APPLICATION_ERROR(-20002, 'Hay municipios distintos del seed anterior; reasigne sus viviendas a mano.');
  END IF;
END;
/

-- 2. Las viviendas apuntan a los municipios viejos: se suelta la FK mientras
--    se reemplazan (DDL, hace commit implícito).
ALTER TABLE vivienda DISABLE CONSTRAINT fk_vivienda_municipio;

-- 3. Reasignar viviendas, vaciar y cargar (una sola transacción).
UPDATE vivienda SET municipio_id = CASE municipio_id
  WHEN 1  THEN 118   -- Bucaramanga
  WHEN 2  THEN 342   -- Floridablanca
  WHEN 3  THEN 374   -- Girón
  WHEN 4  THEN 661   -- Piedecuesta
  WHEN 5  THEN 275   -- Cúcuta
  WHEN 6  THEN 1013  -- Tunja
  WHEN 7  THEN 547   -- Medellín
  WHEN 8  THEN 88    -- Barranquilla
  WHEN 9  THEN 171   -- Cartagena
  WHEN 10 THEN 150   -- Cali
END;

DELETE FROM municipio;
DELETE FROM departamento;
DELETE FROM region;

@@regiones.sql
@@departamentos.sql
@@municipios.sql

COMMIT;

-- 4. Volver a activar la FK (valida que toda vivienda tenga municipio).
ALTER TABLE vivienda ENABLE CONSTRAINT fk_vivienda_municipio;

-- 5. Las secuencias siguen después de los ids fijos.
DROP SEQUENCE seq_region;
DROP SEQUENCE seq_departamento;
DROP SEQUENCE seq_municipio;
CREATE SEQUENCE seq_region       START WITH 7    INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_departamento START WITH 100  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_municipio    START WITH 1103 INCREMENT BY 1 NOCACHE;

SELECT (SELECT COUNT(*) FROM region) regiones,
       (SELECT COUNT(*) FROM departamento) departamentos,
       (SELECT COUNT(*) FROM municipio) municipios
  FROM dual;

EXIT
