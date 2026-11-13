-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Disparadores de validación (Oracle 10g)
--
-- Se ejecuta como dueño del esquema, después de plsql/funciones.sql.
-- Los disparadores de auditoría (trg_cud_<tabla>) están en
-- migraciones/migracion_auditoria.sql.
--
-- Son una segunda barrera: la capa service ya valida lo mismo y le muestra
-- al usuario un mensaje amable antes de llegar a la base. Estos disparadores
-- protegen los datos aunque se modifiquen por fuera de la aplicación (por
-- ejemplo, desde SQL*Plus).
--
-- trg_val_<tabla>: BEFORE INSERT OR UPDATE ... FOR EACH ROW. No se disparan
-- al eliminar: si lo hicieran, al borrar una encuesta en cascada consultarían
-- la tabla encuesta mientras cambia (ORA-04091, tabla mutante).
-- =====================================================================

SET DEFINE OFF

-- ---------------------------------------------------------------------
-- Encuesta: solo se permiten los cambios de estado del flujo del caso y
-- una encuesta cerrada no se modifica (RF-06, RF-10).
--   EN_CURSO             -> PENDIENTE_APROBACION                (cerrar)
--   RECHAZADA            -> PENDIENTE_APROBACION                (cerrar de nuevo)
--   PENDIENTE_APROBACION -> APROBADA | RECHAZADA | EN_CURSO     (aprobar, rechazar, reabrir)
--   APROBADA             -> EN_CURSO                            (reabrir)
-- Solo usa :OLD y :NEW, no consulta tablas.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_val_encuesta
BEFORE UPDATE ON encuesta
FOR EACH ROW
DECLARE
v_permitido BOOLEAN;
BEGIN
IF :NEW.estado=:OLD.estado THEN
   IF :OLD.estado IN ('PENDIENTE_APROBACION','APROBADA') THEN
      RAISE_APPLICATION_ERROR(-20002,'La encuesta '||:OLD.id
         ||' está cerrada. Un administrador debe reabrirla para hacer cambios.');
   END IF;
ELSE
   v_permitido:=(:OLD.estado IN ('EN_CURSO','RECHAZADA') AND :NEW.estado='PENDIENTE_APROBACION')
      OR (:OLD.estado='PENDIENTE_APROBACION' AND :NEW.estado IN ('APROBADA','RECHAZADA','EN_CURSO'))
      OR (:OLD.estado='APROBADA' AND :NEW.estado='EN_CURSO');
   IF NOT v_permitido THEN
      RAISE_APPLICATION_ERROR(-20007,'La encuesta '||:OLD.id||' no puede pasar de "'
         ||fn_descripcion_estado(:OLD.estado)||'" a "'||fn_descripcion_estado(:NEW.estado)||'".');
   END IF;
END IF;
END;
/

-- ---------------------------------------------------------------------
-- Visitas (capítulo B): no se registran ni se modifican si la encuesta del
-- hogar está cerrada. Un hogar sin encuesta no tiene restricción.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_val_visita
BEFORE INSERT OR UPDATE ON visita
FOR EACH ROW
DECLARE
v_encuestaId encuesta.id%TYPE;
BEGIN
v_encuestaId:=fn_encuesta_hogar(:NEW.hogar_id);
IF v_encuestaId IS NOT NULL AND fn_encuesta_editable(v_encuestaId)='N' THEN
   RAISE_APPLICATION_ERROR(-20002,'La encuesta '||v_encuestaId
      ||' está cerrada: no se pueden registrar ni modificar visitas.');
END IF;
END;
/

-- ---------------------------------------------------------------------
-- Capítulo C: no se modifica si la encuesta está cerrada.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_val_caracteristica_gasto
BEFORE INSERT OR UPDATE ON caracteristica_gasto
FOR EACH ROW
BEGIN
IF fn_encuesta_editable(:NEW.encuesta_id)='N' THEN
   RAISE_APPLICATION_ERROR(-20002,'La encuesta '||:NEW.encuesta_id
      ||' está cerrada: no se puede modificar el capítulo C.');
END IF;
END;
/

-- ---------------------------------------------------------------------
-- Días de la encuesta: la encuesta debe estar abierta y la fecha del día n
-- debe ser fecha_desde + n - 1.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_val_registro_diario
BEFORE INSERT OR UPDATE ON registro_diario
FOR EACH ROW
DECLARE
v_fechaEsperada DATE;
BEGIN
IF fn_encuesta_editable(:NEW.encuesta_id)='N' THEN
   RAISE_APPLICATION_ERROR(-20002,'La encuesta '||:NEW.encuesta_id
      ||' está cerrada: no se pueden registrar ni modificar sus días.');
END IF;

v_fechaEsperada:=fn_fecha_dia(:NEW.encuesta_id,:NEW.numero_dia);
IF v_fechaEsperada IS NOT NULL AND TRUNC(:NEW.fecha)<>v_fechaEsperada THEN
   RAISE_APPLICATION_ERROR(-20005,'El día '||:NEW.numero_dia||' de la encuesta corresponde a la fecha '
      ||TO_CHAR(v_fechaEsperada,'YYYY-MM-DD')||'.');
END IF;
END;
/

-- ---------------------------------------------------------------------
-- Capítulo D (gastos diarios): la encuesta debe estar abierta y los gastos
-- personales solo se registran los primeros 7 días (instrucción 4).
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_val_gasto_diario
BEFORE INSERT OR UPDATE ON gasto_diario
FOR EACH ROW
DECLARE
v_encuestaId encuesta.id%TYPE;
v_numeroDia registro_diario.numero_dia%TYPE;
BEGIN
v_encuestaId:=fn_encuesta_registro(:NEW.registro_diario_id);
IF v_encuestaId IS NOT NULL AND fn_encuesta_editable(v_encuestaId)='N' THEN
   RAISE_APPLICATION_ERROR(-20002,'La encuesta '||v_encuestaId
      ||' está cerrada: no se pueden registrar ni modificar gastos.');
END IF;

v_numeroDia:=fn_dia_registro(:NEW.registro_diario_id);
IF :NEW.tipo_gasto='PERSONAL' AND v_numeroDia>7 THEN
   RAISE_APPLICATION_ERROR(-20006,'Los gastos personales solo se registran los primeros 7 días (este es el día '
      ||v_numeroDia||').');
END IF;
END;
/

-- ---------------------------------------------------------------------
-- Capítulo D1 (comidas fuera del hogar): mismas reglas que el capítulo D.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_val_comida_fuera_hogar
BEFORE INSERT OR UPDATE ON comida_fuera_hogar
FOR EACH ROW
DECLARE
v_encuestaId encuesta.id%TYPE;
v_numeroDia registro_diario.numero_dia%TYPE;
BEGIN
v_encuestaId:=fn_encuesta_registro(:NEW.registro_diario_id);
IF v_encuestaId IS NOT NULL AND fn_encuesta_editable(v_encuestaId)='N' THEN
   RAISE_APPLICATION_ERROR(-20002,'La encuesta '||v_encuestaId
      ||' está cerrada: no se pueden registrar ni modificar comidas fuera del hogar.');
END IF;

v_numeroDia:=fn_dia_registro(:NEW.registro_diario_id);
IF :NEW.tipo_gasto='PERSONAL' AND v_numeroDia>7 THEN
   RAISE_APPLICATION_ERROR(-20006,'Los gastos personales solo se registran los primeros 7 días (este es el día '
      ||v_numeroDia||').');
END IF;
END;
/
