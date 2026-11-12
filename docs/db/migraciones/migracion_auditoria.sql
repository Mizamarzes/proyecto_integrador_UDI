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

CREATE OR REPLACE FUNCTION fn_usuario_sesion
RETURN NUMBER
IS
v_usuarioId NUMBER(10);
BEGIN
v_usuarioId:=TO_NUMBER(SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER'));
RETURN v_usuarioId;
EXCEPTION
WHEN VALUE_ERROR OR INVALID_NUMBER THEN
   RETURN NULL;
END;
/

CREATE OR REPLACE PROCEDURE incluir_auditoria
(r_auditoria auditoria%ROWTYPE)
IS
BEGIN
INSERT INTO auditoria (id, tabla, operacion, registro_id, descripcion, usuario_id, usuario_bd, fecha)
VALUES (seq_auditoria.NEXTVAL, r_auditoria.tabla, r_auditoria.operacion, r_auditoria.registro_id,
        SUBSTR(r_auditoria.descripcion,1,500), NVL(fn_usuario_sesion,r_auditoria.usuario_id),
        USER, SYSTIMESTAMP);
END;
/

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

COLUMN object_type FORMAT A12
COLUMN object_name FORMAT A30
COLUMN status FORMAT A8
SELECT object_type, object_name, status
FROM user_objects
WHERE object_name IN ('AUDITORIA', 'SEQ_AUDITORIA', 'FN_USUARIO_SESION', 'INCLUIR_AUDITORIA')
   OR object_name LIKE 'TRG_CUD_%'
ORDER BY object_type, object_name;
