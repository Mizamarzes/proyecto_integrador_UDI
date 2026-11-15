-- =====================================================================
-- Migración: unificar módulos (para esquemas creados con el seed anterior)
--
-- Antes había 10 módulos; ahora son 5:
--   viviendas  ← viviendas + hogares            (nombre «Viviendas y hogares»)
--   encuestas  ← encuestas + visitas + caracteristicas-gasto
--                + gastos-diarios + comidas-fuera-hogar
--   usuarios, roles y reportes no cambian.
--
-- Cada rol que tenía alguno de los módulos viejos recibe el módulo unificado,
-- y luego se borran los módulos viejos (sus permisos se borran en cascada).
-- Se puede ejecutar más de una vez.
--
-- docker cp docs/db/migracion_unificar_modulos.sql oracle-xe-10g:/tmp/
-- docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<clave> @/tmp/migracion_unificar_modulos.sql
-- =====================================================================

-- Encuestas: roles con cualquiera de los módulos de captura
INSERT INTO rol_modulo (id, rol_id, modulo_id)
SELECT seq_rol_modulo.NEXTVAL, r.rol_id, m.id
  FROM (SELECT DISTINCT rm.rol_id
          FROM rol_modulo rm JOIN modulo v ON v.id = rm.modulo_id
         WHERE v.slug IN ('visitas', 'caracteristicas-gasto', 'gastos-diarios', 'comidas-fuera-hogar')) r,
       modulo m
 WHERE m.slug = 'encuestas'
   AND NOT EXISTS (SELECT 1 FROM rol_modulo x WHERE x.rol_id = r.rol_id AND x.modulo_id = m.id);

-- Viviendas y hogares: roles con el módulo de hogares
INSERT INTO rol_modulo (id, rol_id, modulo_id)
SELECT seq_rol_modulo.NEXTVAL, r.rol_id, m.id
  FROM (SELECT DISTINCT rm.rol_id
          FROM rol_modulo rm JOIN modulo v ON v.id = rm.modulo_id
         WHERE v.slug = 'hogares') r,
       modulo m
 WHERE m.slug = 'viviendas'
   AND NOT EXISTS (SELECT 1 FROM rol_modulo x WHERE x.rol_id = r.rol_id AND x.modulo_id = m.id);

DELETE FROM modulo WHERE slug IN ('hogares', 'visitas', 'caracteristicas-gasto', 'gastos-diarios', 'comidas-fuera-hogar');

UPDATE modulo SET nombre = 'Viviendas y hogares', updated_at = SYSTIMESTAMP WHERE slug = 'viviendas';

COMMIT;

SELECT r.nombre AS rol, m.slug AS modulo
  FROM rol_modulo rm JOIN rol r ON r.id = rm.rol_id JOIN modulo m ON m.id = rm.modulo_id
 ORDER BY r.id, m.id;
