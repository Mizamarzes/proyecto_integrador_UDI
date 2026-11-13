-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Procedimientos almacenados (Oracle 10g)
--
-- Se ejecuta como dueño del esquema, después de plsql/funciones.sql.
--
-- La aplicación los llama desde los DAO con CallableStatement
-- ({call pr_...(?, ?)}) dentro de ConexionBD.enTransaccion. Por eso NO
-- hacen COMMIT: la transacción la confirma o la deshace la aplicación (a
-- diferencia de los bloques de ejemplo, que sí terminan en COMMIT; ver
-- pruebas/bloques_anonimos.sql).
--
-- Los permisos por rol de la aplicación (quién puede aprobar o reabrir) se
-- siguen revisando en la capa service; aquí se validan el estado del caso
-- y los datos. Los códigos de error están al inicio de funciones.sql.
-- =====================================================================

SET DEFINE OFF

-- ---------------------------------------------------------------------
-- Sesión: deja el id del usuario de la aplicación en CLIENT_IDENTIFIER.
-- fn_usuario_sesion lo lee para llenar auditoria.usuario_id en las
-- modificaciones y eliminaciones (las inserciones ya traen created_by).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pr_iniciar_sesion
(v_usuarioId usuario.id%TYPE)
IS
BEGIN
DBMS_SESSION.SET_IDENTIFIER(TO_CHAR(v_usuarioId));
END;
/

CREATE OR REPLACE PROCEDURE pr_cerrar_sesion
IS
BEGIN
DBMS_SESSION.CLEAR_IDENTIFIER;
END;
/

-- ---------------------------------------------------------------------
-- Registra una revisión del crítico-digitador en auditoria_encuesta.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pr_incluir_revision
(r_revision auditoria_encuesta%ROWTYPE)
IS
BEGIN
INSERT INTO auditoria_encuesta (id, encuesta_id, accion, observacion, created_by)
VALUES (seq_auditoria_encuesta.NEXTVAL, r_revision.encuesta_id, r_revision.accion,
        r_revision.observacion, r_revision.created_by);
END;
/

-- ---------------------------------------------------------------------
-- Cierra el caso (RF-06): una encuesta en curso o rechazada pasa a
-- «Pendiente de aprobación» y deja de admitir cambios. La fecha de cierre
-- la manda la aplicación (hora local del equipo).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pr_cerrar_encuesta
(v_encuestaId encuesta.id%TYPE,
v_usuarioId usuario.id%TYPE,
v_fechaCierre encuesta.fecha_cierre%TYPE)
IS
v_estado encuesta.estado%TYPE;
BEGIN
v_estado:=fn_estado_encuesta(v_encuestaId);
IF fn_encuesta_editable(v_encuestaId)='N' THEN
   RAISE_APPLICATION_ERROR(-20002,'La encuesta '||v_encuestaId
      ||' está cerrada. Un administrador debe reabrirla para hacer cambios.');
END IF;

UPDATE encuesta
SET estado='PENDIENTE_APROBACION', fecha_cierre=NVL(v_fechaCierre,SYSTIMESTAMP),
    cerrada_por=v_usuarioId, updated_at=SYSTIMESTAMP
WHERE id=v_encuestaId;
END;
/

-- ---------------------------------------------------------------------
-- Reabre un caso pendiente de aprobación o aprobado: vuelve a «En curso»
-- (en la aplicación solo lo puede hacer un administrador). Conserva la
-- fecha y el usuario del último cierre.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pr_reabrir_encuesta
(v_encuestaId encuesta.id%TYPE)
IS
v_estado encuesta.estado%TYPE;
BEGIN
v_estado:=fn_estado_encuesta(v_encuestaId);
IF v_estado NOT IN ('PENDIENTE_APROBACION','APROBADA') THEN
   RAISE_APPLICATION_ERROR(-20003,'La encuesta '||v_encuestaId||' no está cerrada.');
END IF;

UPDATE encuesta
SET estado='EN_CURSO', updated_at=SYSTIMESTAMP
WHERE id=v_encuestaId;
END;
/

-- ---------------------------------------------------------------------
-- Rechaza un caso pendiente de aprobación (RF-10) con la observación para
-- el encuestador. La encuesta vuelve a admitir cambios.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pr_rechazar_encuesta
(v_encuestaId encuesta.id%TYPE,
v_observacion auditoria_encuesta.observacion%TYPE,
v_usuarioId usuario.id%TYPE)
IS
v_estado encuesta.estado%TYPE;
r_revision auditoria_encuesta%ROWTYPE;
BEGIN
v_estado:=fn_estado_encuesta(v_encuestaId);
IF v_estado<>'PENDIENTE_APROBACION' THEN
   RAISE_APPLICATION_ERROR(-20003,'Solo se puede rechazar un caso pendiente de aprobación; '
      ||fn_codigo_encuesta(v_encuestaId)||' está "'||fn_descripcion_estado(v_estado)||'".');
END IF;
IF TRIM(v_observacion) IS NULL THEN
   RAISE_APPLICATION_ERROR(-20004,'Escriba la observación para el encuestador.');
END IF;

UPDATE encuesta
SET estado='RECHAZADA', updated_at=SYSTIMESTAMP
WHERE id=v_encuestaId;

r_revision.encuesta_id:=v_encuestaId;
r_revision.accion:='RECHAZADA';
r_revision.observacion:=TRIM(v_observacion);
r_revision.created_by:=v_usuarioId;
pr_incluir_revision(r_revision);
END;
/

-- ---------------------------------------------------------------------
-- Aprueba un caso pendiente de aprobación (RF-10). La validación cruzada
-- (9 reglas) la hace antes VerificacionService; aquí se revisa el estado,
-- se cambia y se registra la revisión.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pr_aprobar_encuesta
(v_encuestaId encuesta.id%TYPE,
v_usuarioId usuario.id%TYPE)
IS
v_estado encuesta.estado%TYPE;
r_revision auditoria_encuesta%ROWTYPE;
BEGIN
v_estado:=fn_estado_encuesta(v_encuestaId);
IF v_estado<>'PENDIENTE_APROBACION' THEN
   RAISE_APPLICATION_ERROR(-20003,'Solo se puede aprobar un caso pendiente de aprobación; '
      ||fn_codigo_encuesta(v_encuestaId)||' está "'||fn_descripcion_estado(v_estado)||'".');
END IF;

UPDATE encuesta
SET estado='APROBADA', updated_at=SYSTIMESTAMP
WHERE id=v_encuestaId;

r_revision.encuesta_id:=v_encuestaId;
r_revision.accion:='APROBADA';
r_revision.created_by:=v_usuarioId;
pr_incluir_revision(r_revision);
END;
/

-- ---------------------------------------------------------------------
-- Exportación de las encuestas aprobadas (RF-10). Abre un cursor con una
-- fila por encuesta APROBADA: ubicación de la vivienda, encuestador, quién
-- la aprobó y cuándo (última aprobación en auditoria_encuesta), visitas,
-- días registrados y gasto total (fn_gasto_total_encuesta). Los filtros de
-- ubicación son opcionales: NULL = todas. La aplicación escribe con este
-- cursor la hoja «Encuestas aprobadas» del Excel y le agrega el detalle de
-- los capítulos B, C, D y D1 de cada encuesta. Solo lee: no cambia estados
-- ni registra revisiones. Falla con -20008 si un filtro no existe.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE pr_exportar_aprobadas
(v_regionId region.id%TYPE,
v_departamentoId departamento.id%TYPE,
v_municipioId municipio.id%TYPE,
c_encuestas OUT SYS_REFCURSOR)
IS
v_existe NUMBER;
BEGIN
IF v_regionId IS NOT NULL THEN
   SELECT COUNT(*) INTO v_existe FROM region WHERE id=v_regionId;
   IF v_existe=0 THEN
      RAISE_APPLICATION_ERROR(-20008,'No existe la region '||v_regionId||'.');
   END IF;
END IF;
IF v_departamentoId IS NOT NULL THEN
   SELECT COUNT(*) INTO v_existe FROM departamento WHERE id=v_departamentoId;
   IF v_existe=0 THEN
      RAISE_APPLICATION_ERROR(-20008,'No existe el departamento '||v_departamentoId||'.');
   END IF;
END IF;
IF v_municipioId IS NOT NULL THEN
   SELECT COUNT(*) INTO v_existe FROM municipio WHERE id=v_municipioId;
   IF v_existe=0 THEN
      RAISE_APPLICATION_ERROR(-20008,'No existe el municipio '||v_municipioId||'.');
   END IF;
END IF;

OPEN c_encuestas FOR
SELECT e.id encuesta_id, fn_codigo_encuesta(e.id) codigo, e.hogar_id,
       h.hogar_numero, h.total_personas,
       r.nombre region, d.nombre departamento, m.nombre municipio,
       v.barrio, v.direccion,
       ue.nombre||' '||ue.apellido encuestador,
       e.informante_nombre, e.fecha_desde, e.fecha_hasta, e.fecha_cierre,
       CASE WHEN ua.id IS NULL THEN NULL ELSE ua.nombre||' '||ua.apellido END aprobada_por,
       ap.created_at fecha_aprobacion,
       (SELECT COUNT(*) FROM visita vi WHERE vi.hogar_id=e.hogar_id) visitas,
       (SELECT COUNT(*) FROM registro_diario rd WHERE rd.encuesta_id=e.id) dias,
       fn_gasto_total_encuesta(e.id) gasto_total
FROM encuesta e
JOIN hogar h ON h.id=e.hogar_id
JOIN vivienda v ON v.id=h.vivienda_id
JOIN municipio m ON m.id=v.municipio_id
JOIN departamento d ON d.id=m.departamento_id
JOIN region r ON r.id=d.region_id
JOIN encuestador en ON en.id=e.encuestador_id
JOIN usuario ue ON ue.id=en.usuario_id
LEFT JOIN (SELECT a.encuesta_id, a.created_by, a.created_at,
                  ROW_NUMBER() OVER (PARTITION BY a.encuesta_id
                                     ORDER BY a.created_at DESC, a.id DESC) n
           FROM auditoria_encuesta a
           WHERE a.accion='APROBADA') ap ON ap.encuesta_id=e.id AND ap.n=1
LEFT JOIN usuario ua ON ua.id=ap.created_by
WHERE e.estado='APROBADA'
AND (v_regionId IS NULL OR d.region_id=v_regionId)
AND (v_departamentoId IS NULL OR m.departamento_id=v_departamentoId)
AND (v_municipioId IS NULL OR v.municipio_id=v_municipioId)
ORDER BY r.nombre, d.nombre, m.nombre, e.id;
END;
/

-- ---------------------------------------------------------------------
-- Auditoría: inserta un movimiento en la tabla auditoria. Lo llaman los
-- disparadores trg_cud_<tabla> (plsql/disparadores.sql). El usuario es el
-- de la sesión (fn_usuario_sesion) o, si no hay, el que trae el registro
-- (created_by en las inserciones).
-- ---------------------------------------------------------------------
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
