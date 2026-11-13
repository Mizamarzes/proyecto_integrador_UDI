-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Migración: tabla auditoria, su índice y la secuencia seq_auditoria.
--
-- Solo crea los objetos de datos (si ya existen no hace nada). El PL/SQL
-- de la auditoría está en plsql/:
--   fn_usuario_sesion   plsql/funciones.sql
--   incluir_auditoria   plsql/procedimientos.sql
--   trg_cud_<tabla>     plsql/disparadores.sql
-- esquema_oracle10g.sql ejecuta esta migración y después esos archivos.
-- En una base que ya existe, ejecute esta migración y luego:
--   @plsql/funciones.sql
--   @plsql/procedimientos.sql
--   @plsql/disparadores.sql
-- =====================================================================

SET DEFINE OFF

BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE auditoria (
    id           NUMBER(10)     CONSTRAINT aud_id_pk PRIMARY KEY,
    tabla        VARCHAR2(30)   CONSTRAINT aud_tabla_nn NOT NULL,
    operacion    VARCHAR2(12)   CONSTRAINT aud_operacion_nn NOT NULL,
    registro_id  NUMBER(10),
    descripcion  VARCHAR2(500)  CONSTRAINT aud_desc_nn NOT NULL,
    usuario_id   NUMBER(10),
    usuario_bd   VARCHAR2(30)   DEFAULT USER CONSTRAINT aud_usuario_bd_nn NOT NULL,
    fecha        TIMESTAMP      DEFAULT SYSTIMESTAMP CONSTRAINT aud_fecha_nn NOT NULL,
    CONSTRAINT aud_operacion_ck CHECK (operacion IN (''INSERCION'', ''MODIFICACION'', ''ELIMINACION'')))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX ix_auditoria_tabla_registro ON auditoria (tabla, registro_id)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE seq_auditoria START WITH 1 INCREMENT BY 1 NOCACHE';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF;
END;
/

COLUMN object_type FORMAT A12
COLUMN object_name FORMAT A30
COLUMN status FORMAT A8
SELECT object_type, object_name, status
FROM user_objects
WHERE object_name IN ('AUDITORIA', 'IX_AUDITORIA_TABLA_REGISTRO', 'SEQ_AUDITORIA')
ORDER BY object_type, object_name;
