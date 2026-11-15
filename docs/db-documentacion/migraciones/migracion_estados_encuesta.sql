-- =====================================================================
-- Migración: cuatro estados de la encuesta
--
-- Para esquemas creados con los estados ABIERTA, CERRADA, REABIERTA,
-- EN_CORRECCION y APROBADA (requiere migracion_verificacion.sql aplicada):
--   1. encuesta.estado pasa a EN_CURSO, PENDIENTE_APROBACION, APROBADA y
--      RECHAZADA:
--        ABIERTA, REABIERTA -> EN_CURSO
--        CERRADA            -> PENDIENTE_APROBACION
--        EN_CORRECCION      -> RECHAZADA
--        APROBADA           -> APROBADA
--   2. auditoria_encuesta.accion solo admite APROBADA y RECHAZADA:
--        APROBACION -> APROBADA, DEVOLUCION -> RECHAZADA, y se borran
--        los registros de CIERRE, REAPERTURA y EXPORTACION.
--
-- Se puede ejecutar más de una vez.
--
-- docker cp docs/db/migraciones/migracion_estados_encuesta.sql oracle-xe-10g:/tmp/
-- docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<clave> @/tmp/migracion_estados_encuesta.sql
-- =====================================================================

SET DEFINE OFF

-- 1. Estados de la encuesta
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE encuesta DROP CONSTRAINT ck_encuesta_estado';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE encuesta DROP CONSTRAINT ck_encuesta_cierre';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

UPDATE encuesta SET estado = 'EN_CURSO' WHERE estado IN ('ABIERTA', 'REABIERTA');
UPDATE encuesta SET estado = 'PENDIENTE_APROBACION' WHERE estado = 'CERRADA';
UPDATE encuesta SET estado = 'RECHAZADA' WHERE estado = 'EN_CORRECCION';

ALTER TABLE encuesta MODIFY (estado DEFAULT 'EN_CURSO');
ALTER TABLE encuesta ADD CONSTRAINT ck_encuesta_estado
  CHECK (estado IN ('EN_CURSO', 'PENDIENTE_APROBACION', 'APROBADA', 'RECHAZADA'));
ALTER TABLE encuesta ADD CONSTRAINT ck_encuesta_cierre
  CHECK (estado NOT IN ('PENDIENTE_APROBACION', 'APROBADA') OR (fecha_cierre IS NOT NULL AND cerrada_por IS NOT NULL));

-- 2. Acciones del historial
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE auditoria_encuesta DROP CONSTRAINT ck_auditoria_accion';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE auditoria_encuesta DROP CONSTRAINT ck_auditoria_devolucion';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE auditoria_encuesta DROP CONSTRAINT ck_auditoria_rechazo';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

DELETE FROM auditoria_encuesta WHERE accion IN ('CIERRE', 'REAPERTURA', 'EXPORTACION');
UPDATE auditoria_encuesta SET accion = 'APROBADA' WHERE accion = 'APROBACION';
UPDATE auditoria_encuesta SET accion = 'RECHAZADA' WHERE accion = 'DEVOLUCION';

ALTER TABLE auditoria_encuesta ADD CONSTRAINT ck_auditoria_accion
  CHECK (accion IN ('APROBADA', 'RECHAZADA'));
ALTER TABLE auditoria_encuesta ADD CONSTRAINT ck_auditoria_rechazo
  CHECK (accion <> 'RECHAZADA' OR observacion IS NOT NULL);

COMMIT;
