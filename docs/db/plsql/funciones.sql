-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Funciones PL/SQL (Oracle 10g)
--
-- Se ejecuta como dueño del esquema, después de las tablas. Lo llama
-- esquema_oracle10g.sql; para una base que ya existe:
--   sqlplus <esquema>/<clave> @plsql/funciones.sql
--
-- Las funciones no modifican datos, así que también se pueden usar dentro
-- de un SELECT (la aplicación usa fn_gasto_total_encuesta en Reportes).
--
-- Errores propios (RAISE_APPLICATION_ERROR), comunes a funciones,
-- procedimientos y disparadores. La aplicación muestra el mensaje al usuario:
--   -20001  No existe la encuesta
--   -20002  La encuesta está cerrada (no admite cambios)
--   -20003  El estado de la encuesta no permite la acción
--   -20004  Falta la observación del rechazo
--   -20005  La fecha del día no corresponde al periodo de la encuesta
--   -20006  Gasto personal después del día 7
--   -20007  Cambio de estado no permitido
--
-- Los mensajes no llevan «ñ» ni mayúsculas con tilde: la base usa ZHS16GBK.
-- (fn_usuario_sesion está en migraciones/migracion_auditoria.sql.)
-- =====================================================================

SET DEFINE OFF

-- ---------------------------------------------------------------------
-- Código del caso como lo muestra la aplicación: E-000005
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_codigo_encuesta
(v_encuestaId encuesta.id%TYPE)
RETURN VARCHAR2
IS
BEGIN
RETURN 'E-'||LPAD(v_encuestaId,6,'0');
END;
/

-- ---------------------------------------------------------------------
-- Estado de la encuesta. Falla con -20001 si no existe.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_estado_encuesta
(v_encuestaId encuesta.id%TYPE)
RETURN VARCHAR2
IS
v_estado encuesta.estado%TYPE;
BEGIN
SELECT estado INTO v_estado
FROM encuesta
WHERE id=v_encuestaId;
RETURN v_estado;
EXCEPTION
WHEN NO_DATA_FOUND THEN
   RAISE_APPLICATION_ERROR(-20001,'No existe la encuesta '||v_encuestaId||'.');
END;
/

-- ---------------------------------------------------------------------
-- Nombre del estado para los mensajes (el mismo de EstadoEncuesta en Java).
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_descripcion_estado
(v_estado encuesta.estado%TYPE)
RETURN VARCHAR2
IS
BEGIN
RETURN CASE v_estado
          WHEN 'EN_CURSO' THEN 'En curso'
          WHEN 'PENDIENTE_APROBACION' THEN 'Pendiente de aprobación'
          WHEN 'APROBADA' THEN 'Aprobada'
          WHEN 'RECHAZADA' THEN 'Rechazada'
          ELSE v_estado
       END;
END;
/

-- ---------------------------------------------------------------------
-- 'S' si la encuesta admite cambios (en curso o rechazada) y 'N' si está
-- cerrada (pendiente de aprobación o aprobada), RF-06. Si la encuesta no
-- existe devuelve 'S': de eso responde la llave foránea.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_encuesta_editable
(v_encuestaId encuesta.id%TYPE)
RETURN CHAR
IS
v_estado encuesta.estado%TYPE;
BEGIN
SELECT estado INTO v_estado
FROM encuesta
WHERE id=v_encuestaId;
IF v_estado IN ('EN_CURSO','RECHAZADA') THEN
   RETURN 'S';
END IF;
RETURN 'N';
EXCEPTION
WHEN NO_DATA_FOUND THEN
   RETURN 'S';
END;
/

-- ---------------------------------------------------------------------
-- Encuesta del hogar (un hogar tiene a lo sumo una). Null si no tiene.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_encuesta_hogar
(v_hogarId hogar.id%TYPE)
RETURN NUMBER
IS
v_encuestaId encuesta.id%TYPE;
BEGIN
SELECT id INTO v_encuestaId
FROM encuesta
WHERE hogar_id=v_hogarId;
RETURN v_encuestaId;
EXCEPTION
WHEN NO_DATA_FOUND THEN
   RETURN NULL;
END;
/

-- ---------------------------------------------------------------------
-- Encuesta a la que pertenece un día (registro_diario). Null si no existe.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_encuesta_registro
(v_registroId registro_diario.id%TYPE)
RETURN NUMBER
IS
v_encuestaId registro_diario.encuesta_id%TYPE;
BEGIN
SELECT encuesta_id INTO v_encuestaId
FROM registro_diario
WHERE id=v_registroId;
RETURN v_encuestaId;
EXCEPTION
WHEN NO_DATA_FOUND THEN
   RETURN NULL;
END;
/

-- ---------------------------------------------------------------------
-- Número de día (1 a 14) de un registro_diario. Null si no existe.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_dia_registro
(v_registroId registro_diario.id%TYPE)
RETURN NUMBER
IS
v_numeroDia registro_diario.numero_dia%TYPE;
BEGIN
SELECT numero_dia INTO v_numeroDia
FROM registro_diario
WHERE id=v_registroId;
RETURN v_numeroDia;
EXCEPTION
WHEN NO_DATA_FOUND THEN
   RETURN NULL;
END;
/

-- ---------------------------------------------------------------------
-- Fecha que corresponde al día n de la encuesta: fecha_desde + n - 1.
-- Null si la encuesta no existe.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_fecha_dia
(v_encuestaId encuesta.id%TYPE,
v_numeroDia registro_diario.numero_dia%TYPE)
RETURN DATE
IS
v_fechaDesde encuesta.fecha_desde%TYPE;
BEGIN
SELECT fecha_desde INTO v_fechaDesde
FROM encuesta
WHERE id=v_encuestaId;
RETURN TRUNC(v_fechaDesde)+v_numeroDia-1;
EXCEPTION
WHEN NO_DATA_FOUND THEN
   RETURN NULL;
END;
/

-- ---------------------------------------------------------------------
-- Gasto total de la encuesta: gastos diarios (capítulo D) más comidas
-- fuera del hogar (capítulo D1) de sus 14 días. 0 si no tiene renglones.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_gasto_total_encuesta
(v_encuestaId encuesta.id%TYPE)
RETURN NUMBER
IS
v_gastos NUMBER;
v_comidas NUMBER;
BEGIN
SELECT NVL(SUM(g.valor),0) INTO v_gastos
FROM gasto_diario g JOIN registro_diario r ON r.id=g.registro_diario_id
WHERE r.encuesta_id=v_encuestaId;

SELECT NVL(SUM(cf.valor),0) INTO v_comidas
FROM comida_fuera_hogar cf JOIN registro_diario r ON r.id=cf.registro_diario_id
WHERE r.encuesta_id=v_encuestaId;

RETURN v_gastos+v_comidas;
END;
/
