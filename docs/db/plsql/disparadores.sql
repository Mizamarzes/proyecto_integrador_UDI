-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Disparadores de validación (Oracle 10g)
--
-- Se ejecuta como dueño del esquema, después de plsql/funciones.sql y
-- plsql/procedimientos.sql. Tiene dos grupos:
--   trg_val_<tabla>  validación (este encabezado)
--   trg_cud_<tabla>  auditoría (al final del archivo; necesitan la tabla
--                    auditoria de migraciones/migracion_auditoria.sql)
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

-- =====================================================================
-- DISPARADORES DE AUDITORÍA
-- trg_cud_<tabla>: AFTER INSERT OR UPDATE OR DELETE ... FOR EACH ROW, uno
-- por tabla. Registran cada movimiento en la tabla auditoria (creada por
-- migraciones/migracion_auditoria.sql) con el procedimiento
-- incluir_auditoria. Solo usan :NEW y :OLD (no consultan tablas, para no
-- caer en ORA-04091 con los ON DELETE CASCADE) y nunca guardan la clave
-- del usuario.
-- =====================================================================

CREATE OR REPLACE TRIGGER trg_cud_modulo
AFTER INSERT OR UPDATE OR DELETE ON modulo
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='MODULO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el módulo '||:NEW.nombre||' ('||:NEW.slug||')';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el módulo '||:OLD.nombre||' ('||:OLD.slug||')';
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el módulo '||:OLD.nombre||' ('||:NEW.slug||')';
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_rol
AFTER INSERT OR UPDATE OR DELETE ON rol
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='ROL';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el rol '||:NEW.nombre;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el rol '||:OLD.nombre;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el rol '||:OLD.nombre;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_rol_modulo
AFTER INSERT OR UPDATE OR DELETE ON rol_modulo
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='ROL_MODULO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se dio permiso al rol '||:NEW.rol_id||' sobre el módulo '||:NEW.modulo_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se quitó el permiso del rol '||:OLD.rol_id||' sobre el módulo '||:OLD.modulo_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el permiso del rol '||:NEW.rol_id||' sobre el módulo '||:NEW.modulo_id;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_usuario
AFTER INSERT OR UPDATE OR DELETE ON usuario
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='USUARIO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el usuario '||:NEW.email||' con el rol '||:NEW.rol_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el usuario '||:OLD.email;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   IF UPDATING('ultimo_acceso') THEN
      r_auditoria.usuario_id:=:NEW.id;
      r_auditoria.descripcion:='Inicio de sesión del usuario '||:NEW.email;
   ELSIF UPDATING('password') THEN
      r_auditoria.descripcion:='Se cambió la clave del usuario '||:NEW.email;
   ELSIF :OLD.activo<>:NEW.activo THEN
      r_auditoria.descripcion:='Se '||CASE :NEW.activo WHEN 'S' THEN 'activó' ELSE 'desactivó' END
                               ||' el usuario '||:NEW.email;
   ELSE
      r_auditoria.descripcion:='Se modificó el usuario '||:OLD.email;
      IF :OLD.rol_id<>:NEW.rol_id THEN
         r_auditoria.descripcion:=r_auditoria.descripcion||', rol: '||:OLD.rol_id||' -> '||:NEW.rol_id;
      END IF;
   END IF;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_encuestador
AFTER INSERT OR UPDATE OR DELETE ON encuestador
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='ENCUESTADOR';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el encuestador '||:NEW.id||' del usuario '||:NEW.usuario_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el encuestador '||:OLD.id||' del usuario '||:OLD.usuario_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el encuestador '||:NEW.id||' del usuario '||:NEW.usuario_id;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_region
AFTER INSERT OR UPDATE OR DELETE ON region
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='REGION';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se incluyó la región '||:NEW.nombre;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó la región '||:OLD.nombre;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó la región '||:OLD.nombre||' -> '||:NEW.nombre;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_departamento
AFTER INSERT OR UPDATE OR DELETE ON departamento
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='DEPARTAMENTO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se incluyó el departamento '||:NEW.nombre;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el departamento '||:OLD.nombre;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el departamento '||:OLD.nombre||' -> '||:NEW.nombre;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_municipio
AFTER INSERT OR UPDATE OR DELETE ON municipio
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='MUNICIPIO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se incluyó el municipio '||:NEW.nombre||' del departamento '||:NEW.departamento_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el municipio '||:OLD.nombre||' del departamento '||:OLD.departamento_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el municipio '||:OLD.nombre||' -> '||:NEW.nombre;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_vivienda
AFTER INSERT OR UPDATE OR DELETE ON vivienda
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='VIVIENDA';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó la vivienda '||:NEW.id||' ('||:NEW.direccion||')';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó la vivienda '||:OLD.id||' ('||:OLD.direccion||')';
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó la vivienda '||:NEW.id||' ('||:NEW.direccion||')';
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_hogar
AFTER INSERT OR UPDATE OR DELETE ON hogar
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='HOGAR';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el hogar '||:NEW.hogar_numero||' de la vivienda '||:NEW.vivienda_id
                            ||' con '||:NEW.total_personas||' personas';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el hogar '||:OLD.hogar_numero||' de la vivienda '||:OLD.vivienda_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el hogar '||:NEW.hogar_numero||' de la vivienda '||:NEW.vivienda_id;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_encuesta
AFTER INSERT OR UPDATE OR DELETE ON encuesta
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='ENCUESTA';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó la encuesta '||:NEW.id||' del hogar '||:NEW.hogar_id
                            ||' (del '||TO_CHAR(:NEW.fecha_desde,'DD-MM-YYYY')
                            ||' al '||TO_CHAR(:NEW.fecha_hasta,'DD-MM-YYYY')||')';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó la encuesta '||:OLD.id||' del hogar '||:OLD.hogar_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   IF :OLD.estado<>:NEW.estado THEN
      r_auditoria.descripcion:='Se cambió el estado de la encuesta '||:NEW.id||': '
                               ||:OLD.estado||' -> '||:NEW.estado;
   ELSE
      r_auditoria.descripcion:='Se modificó la encuesta '||:NEW.id||' del hogar '||:NEW.hogar_id;
   END IF;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_visita
AFTER INSERT OR UPDATE OR DELETE ON visita
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='VISITA';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó la visita '||:NEW.numero_visita||' del hogar '||:NEW.hogar_id
                            ||' ('||TO_CHAR(:NEW.fecha,'DD-MM-YYYY')||', resultado '||:NEW.resultado_visita||')';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó la visita '||:OLD.numero_visita||' del hogar '||:OLD.hogar_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó la visita '||:NEW.numero_visita||' del hogar '||:NEW.hogar_id;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_grupo_alimento
AFTER INSERT OR UPDATE OR DELETE ON grupo_alimento
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='GRUPO_ALIMENTO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el grupo de alimentos '||:NEW.id||' ('||:NEW.nombre||')';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el grupo de alimentos '||:OLD.id||' ('||:OLD.nombre||')';
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el grupo de alimentos '||:NEW.id||' ('||:NEW.nombre||')';
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_caracteristica_gasto
AFTER INSERT OR UPDATE OR DELETE ON caracteristica_gasto
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='CARACTERISTICA_GASTO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el grupo '||:NEW.grupo_alimento_id||' en el capítulo C de la encuesta '
                            ||:NEW.encuesta_id||' (frecuencia '||:NEW.frecuencia_compra||')';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el grupo '||:OLD.grupo_alimento_id||' del capítulo C de la encuesta '
                            ||:OLD.encuesta_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el grupo '||:NEW.grupo_alimento_id||' del capítulo C de la encuesta '
                            ||:NEW.encuesta_id||' (frecuencia '||:OLD.frecuencia_compra
                            ||' -> '||:NEW.frecuencia_compra||')';
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_lugar_compra
AFTER INSERT OR UPDATE OR DELETE ON lugar_compra
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='LUGAR_COMPRA';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el lugar de compra '||:NEW.id||' ('||:NEW.nombre||')';
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el lugar de compra '||:OLD.id||' ('||:OLD.nombre||')';
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el lugar de compra '||:NEW.id||' ('||:NEW.nombre||')';
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_registro_diario
AFTER INSERT OR UPDATE OR DELETE ON registro_diario
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='REGISTRO_DIARIO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el día '||:NEW.numero_dia||' ('||TO_CHAR(:NEW.fecha,'DD-MM-YYYY')
                            ||') de la encuesta '||:NEW.encuesta_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el día '||:OLD.numero_dia||' de la encuesta '||:OLD.encuesta_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el día '||:NEW.numero_dia||' de la encuesta '||:NEW.encuesta_id;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_gasto_diario
AFTER INSERT OR UPDATE OR DELETE ON gasto_diario
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='GASTO_DIARIO';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó el gasto '||:NEW.numero_orden||' ('||:NEW.nombre_articulo||', $'
                            ||:NEW.valor||') del registro diario '||:NEW.registro_diario_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó el gasto '||:OLD.numero_orden||' ('||:OLD.nombre_articulo||', $'
                            ||:OLD.valor||') del registro diario '||:OLD.registro_diario_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó el gasto '||:NEW.numero_orden||' ('||:NEW.nombre_articulo
                            ||') del registro diario '||:NEW.registro_diario_id;
   IF :OLD.valor<>:NEW.valor THEN
      r_auditoria.descripcion:=r_auditoria.descripcion||', valor: $'||:OLD.valor||' -> $'||:NEW.valor;
   END IF;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_comida_fuera_hogar
AFTER INSERT OR UPDATE OR DELETE ON comida_fuera_hogar
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='COMIDA_FUERA_HOGAR';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se incluyó la comida fuera del hogar '||:NEW.numero_orden||' ('||:NEW.tipo_comida
                            ||': '||:NEW.descripcion||', $'||:NEW.valor||') del registro diario '
                            ||:NEW.registro_diario_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó la comida fuera del hogar '||:OLD.numero_orden||' ('||:OLD.descripcion
                            ||') del registro diario '||:OLD.registro_diario_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó la comida fuera del hogar '||:NEW.numero_orden||' ('||:NEW.descripcion
                            ||') del registro diario '||:NEW.registro_diario_id;
   IF :OLD.valor<>:NEW.valor THEN
      r_auditoria.descripcion:=r_auditoria.descripcion||', valor: $'||:OLD.valor||' -> $'||:NEW.valor;
   END IF;
END IF;
incluir_auditoria(r_auditoria);
END;
/

CREATE OR REPLACE TRIGGER trg_cud_auditoria_encuesta
AFTER INSERT OR UPDATE OR DELETE ON auditoria_encuesta
FOR EACH ROW
DECLARE
r_auditoria auditoria%ROWTYPE;
BEGIN
r_auditoria.tabla:='AUDITORIA_ENCUESTA';
IF INSERTING THEN
   r_auditoria.operacion:='INSERCION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.usuario_id:=:NEW.created_by;
   r_auditoria.descripcion:='Se registró la acción '||:NEW.accion||' sobre la encuesta '||:NEW.encuesta_id;
ELSIF DELETING THEN
   r_auditoria.operacion:='ELIMINACION';
   r_auditoria.registro_id:=:OLD.id;
   r_auditoria.descripcion:='Se eliminó la acción '||:OLD.accion||' de la encuesta '||:OLD.encuesta_id;
ELSE
   r_auditoria.operacion:='MODIFICACION';
   r_auditoria.registro_id:=:NEW.id;
   r_auditoria.descripcion:='Se modificó la acción '||:OLD.accion||' de la encuesta '||:NEW.encuesta_id;
END IF;
incluir_auditoria(r_auditoria);
END;
/
