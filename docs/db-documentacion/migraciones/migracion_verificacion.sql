-- =====================================================================
-- Migración: módulo de Verificación (RF-10)
--
-- Para esquemas creados antes del módulo:
--   1. encuesta.estado pasa a VARCHAR2(20) y admite EN_CORRECCION y APROBADA.
--   2. Nueva tabla auditoria_encuesta (historial: cierre, reapertura,
--      devolución, aprobación y exportación) con su secuencia.
--   3. Nuevo módulo «verificacion», asignado al rol CRITICO_DIGITADOR.
--   4. Cada encuesta con fecha de cierre recibe su registro de CIERRE.
--
-- Se puede ejecutar más de una vez.
--
-- docker cp docs/db/migraciones/migracion_verificacion.sql oracle-xe-10g:/tmp/
-- docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<clave> @/tmp/migracion_verificacion.sql
-- =====================================================================

SET DEFINE OFF

-- 1. Estados de la encuesta
ALTER TABLE encuesta MODIFY (estado VARCHAR2(20));

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
ALTER TABLE encuesta ADD CONSTRAINT ck_encuesta_estado
  CHECK (estado IN ('ABIERTA', 'CERRADA', 'REABIERTA', 'EN_CORRECCION', 'APROBADA'));
ALTER TABLE encuesta ADD CONSTRAINT ck_encuesta_cierre
  CHECK (estado NOT IN ('CERRADA', 'APROBADA') OR (fecha_cierre IS NOT NULL AND cerrada_por IS NOT NULL));

-- 2. Historial de verificación (ORA-00955: el objeto ya existe)
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE auditoria_encuesta (
    id           NUMBER(10)      NOT NULL,
    encuesta_id  NUMBER(10)      NOT NULL,
    accion       VARCHAR2(20)    NOT NULL,
    observacion  VARCHAR2(1000),
    created_at   TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at   TIMESTAMP,
    created_by   NUMBER(10),
    CONSTRAINT pk_auditoria_encuesta PRIMARY KEY (id),
    CONSTRAINT ck_auditoria_accion CHECK (accion IN (''CIERRE'', ''REAPERTURA'', ''DEVOLUCION'', ''APROBACION'', ''EXPORTACION'')),
    CONSTRAINT ck_auditoria_devolucion CHECK (accion <> ''DEVOLUCION'' OR observacion IS NOT NULL),
    CONSTRAINT fk_auditoria_encuesta FOREIGN KEY (encuesta_id) REFERENCES encuesta (id) ON DELETE CASCADE,
    CONSTRAINT fk_auditoria_created_by FOREIGN KEY (created_by) REFERENCES usuario (id))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX ix_auditoria_encuesta ON auditoria_encuesta (encuesta_id)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_auditoria_encuesta START WITH 1 INCREMENT BY 1 NOCACHE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF;
END;
/

-- 3. Módulo y permiso del crítico-digitador
INSERT INTO modulo (id, nombre, slug)
SELECT seq_modulo.NEXTVAL, 'Verificación', 'verificacion' FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM modulo WHERE slug = 'verificacion');

INSERT INTO rol_modulo (id, rol_id, modulo_id)
SELECT seq_rol_modulo.NEXTVAL, r.id, m.id
  FROM rol r, modulo m
 WHERE r.nombre = 'CRITICO_DIGITADOR' AND m.slug = 'verificacion'
   AND NOT EXISTS (SELECT 1 FROM rol_modulo x WHERE x.rol_id = r.id AND x.modulo_id = m.id);

-- 4. Cierre de las encuestas que ya estaban cerradas
INSERT INTO auditoria_encuesta (id, encuesta_id, accion, observacion, created_at, created_by)
SELECT seq_auditoria_encuesta.NEXTVAL, e.id, 'CIERRE', NULL, e.fecha_cierre, e.cerrada_por
  FROM encuesta e
 WHERE e.fecha_cierre IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM auditoria_encuesta a WHERE a.encuesta_id = e.id AND a.accion = 'CIERRE');

COMMIT;

SELECT r.nombre AS rol, m.slug AS modulo
  FROM rol_modulo rm JOIN rol r ON r.id = rm.rol_id JOIN modulo m ON m.id = rm.modulo_id
 ORDER BY r.id, m.id;
