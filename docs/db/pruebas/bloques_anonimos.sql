-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Bloques anónimos que invocan las funciones, procedimientos, disparadores
-- y reportes
-- (para la sustentación y el video).
--
--   docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g \
--     sqlplus enph/enph @/tmp/db/pruebas/bloques_anonimos.sql
--
-- Las pruebas que modifican datos terminan en ROLLBACK, así que el script
-- se puede ejecutar varias veces sin dañar la base. Toma las encuestas de
-- los datos que haya (la primera en curso, la primera pendiente...).
-- =====================================================================

SET SERVEROUTPUT ON SIZE 100000
SET DEFINE OFF
SET FEEDBACK OFF
SET LINESIZE 150
SET PAGESIZE 100

PROMPT
PROMPT =============================================================
PROMPT  1. FUNCIONES
PROMPT =============================================================

PROMPT
PROMPT -- 1.1 Estado, si admite cambios y gasto total (D + D1) de cada encuesta
COLUMN caso FORMAT A10
COLUMN estado FORMAT A25
COLUMN editable FORMAT A8
COLUMN gasto_total FORMAT 999,999,990
SELECT fn_codigo_encuesta(e.id) caso,
       fn_descripcion_estado(e.estado) estado,
       fn_encuesta_editable(e.id) editable,
       fn_gasto_total_encuesta(e.id) gasto_total
FROM encuesta e
ORDER BY e.id;

PROMPT
PROMPT -- 1.2 Fecha de cada día del periodo de la primera encuesta
DECLARE
v_encuestaId encuesta.id%TYPE;
BEGIN
SELECT MIN(id) INTO v_encuestaId FROM encuesta;
DBMS_OUTPUT.PUT_LINE('Encuesta '||fn_codigo_encuesta(v_encuestaId));
FOR v_dia IN 1..14 LOOP
   DBMS_OUTPUT.PUT_LINE('  Dia '||LPAD(v_dia,2)||': '||TO_CHAR(fn_fecha_dia(v_encuestaId,v_dia),'YYYY-MM-DD'));
END LOOP;
END;
/

PROMPT
PROMPT -- 1.3 Encuesta de un hogar y de un dia registrado
DECLARE
v_hogarId hogar.id%TYPE;
v_registroId registro_diario.id%TYPE;
BEGIN
SELECT MIN(hogar_id) INTO v_hogarId FROM encuesta;
SELECT MIN(id) INTO v_registroId FROM registro_diario;
DBMS_OUTPUT.PUT_LINE('Hogar '||v_hogarId||' -> encuesta '||fn_encuesta_hogar(v_hogarId));
DBMS_OUTPUT.PUT_LINE('Registro diario '||v_registroId||' -> encuesta '||fn_encuesta_registro(v_registroId)
   ||', dia '||fn_dia_registro(v_registroId));
END;
/

PROMPT
PROMPT -- 1.4 Error controlado: estado de una encuesta que no existe (-20001)
DECLARE
v_estado encuesta.estado%TYPE;
BEGIN
v_estado:=fn_estado_encuesta(-1);
EXCEPTION
WHEN OTHERS THEN
   DBMS_OUTPUT.PUT_LINE('Error esperado: '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
/

PROMPT
PROMPT =============================================================
PROMPT  2. PROCEDIMIENTOS (flujo completo de un caso)
PROMPT =============================================================

DECLARE
v_encuestaId encuesta.id%TYPE;
v_usuarioId usuario.id%TYPE;
v_criticoId usuario.id%TYPE;

PROCEDURE mostrar(v_paso VARCHAR2) IS
   v_estado encuesta.estado%TYPE;
   v_revisiones NUMBER;
BEGIN
   v_estado:=fn_estado_encuesta(v_encuestaId);
   SELECT COUNT(*) INTO v_revisiones FROM auditoria_encuesta WHERE encuesta_id=v_encuestaId;
   DBMS_OUTPUT.PUT_LINE(RPAD(v_paso,32)||'-> '||RPAD(fn_descripcion_estado(v_estado),25)
      ||' revisiones: '||v_revisiones);
END;

BEGIN
SELECT MIN(id) INTO v_encuestaId FROM encuesta WHERE estado='EN_CURSO';
SELECT created_by INTO v_usuarioId FROM encuesta WHERE id=v_encuestaId;
SELECT MIN(u.id) INTO v_criticoId FROM usuario u JOIN rol r ON r.id=u.rol_id WHERE r.nombre='CRITICO_DIGITADOR';

pr_iniciar_sesion(v_usuarioId);
DBMS_OUTPUT.PUT_LINE('CLIENT_IDENTIFIER = '||SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER')
   ||', fn_usuario_sesion = '||fn_usuario_sesion);
DBMS_OUTPUT.PUT_LINE('Caso '||fn_codigo_encuesta(v_encuestaId));
mostrar('Estado inicial');

pr_cerrar_encuesta(v_encuestaId,v_usuarioId,SYSTIMESTAMP);
mostrar('pr_cerrar_encuesta');

pr_iniciar_sesion(v_criticoId);
pr_rechazar_encuesta(v_encuestaId,'Falta el valor de la ultima compra del grupo 3.',v_criticoId);
mostrar('pr_rechazar_encuesta');

pr_iniciar_sesion(v_usuarioId);
pr_cerrar_encuesta(v_encuestaId,v_usuarioId,SYSTIMESTAMP);
mostrar('pr_cerrar_encuesta (de nuevo)');

pr_iniciar_sesion(v_criticoId);
pr_aprobar_encuesta(v_encuestaId,v_criticoId);
mostrar('pr_aprobar_encuesta');

pr_reabrir_encuesta(v_encuestaId);
mostrar('pr_reabrir_encuesta');

pr_cerrar_sesion;
DBMS_OUTPUT.PUT_LINE('CLIENT_IDENTIFIER tras pr_cerrar_sesion = '
   ||NVL(SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER'),'(vacio)'));

ROLLBACK;
DBMS_OUTPUT.PUT_LINE('ROLLBACK: la encuesta queda como estaba.');
END;
/

PROMPT
PROMPT -- 2.1 Errores controlados de los procedimientos
DECLARE
v_enCurso encuesta.id%TYPE;
BEGIN
SELECT MIN(id) INTO v_enCurso FROM encuesta WHERE estado='EN_CURSO';
BEGIN
   pr_aprobar_encuesta(v_enCurso,1);
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Aprobar un caso en curso:   '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
BEGIN
   pr_reabrir_encuesta(v_enCurso);
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Reabrir un caso en curso:   '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
BEGIN
   pr_cerrar_encuesta(v_enCurso,1,SYSTIMESTAMP);
   pr_rechazar_encuesta(v_enCurso,'   ',1);
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Rechazar sin observacion:   '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
ROLLBACK;
END;
/

PROMPT
PROMPT -- 2.2 Exportacion de las encuestas aprobadas (pr_exportar_aprobadas, cursor de salida)
DECLARE
c_encuestas SYS_REFCURSOR;
v_encuestaId encuesta.id%TYPE;
v_codigo VARCHAR2(20);
v_hogarId hogar.id%TYPE;
v_hogarNumero hogar.hogar_numero%TYPE;
v_personas hogar.total_personas%TYPE;
v_region region.nombre%TYPE;
v_departamento departamento.nombre%TYPE;
v_municipio municipio.nombre%TYPE;
v_barrio vivienda.barrio%TYPE;
v_direccion vivienda.direccion%TYPE;
v_encuestador VARCHAR2(200);
v_informante encuesta.informante_nombre%TYPE;
v_desde encuesta.fecha_desde%TYPE;
v_hasta encuesta.fecha_hasta%TYPE;
v_cierre encuesta.fecha_cierre%TYPE;
v_aprobadaPor VARCHAR2(200);
v_fechaAprobacion auditoria_encuesta.created_at%TYPE;
v_visitas NUMBER;
v_dias NUMBER;
v_gasto NUMBER;
v_total NUMBER:=0;
BEGIN
pr_exportar_aprobadas(NULL,NULL,NULL,c_encuestas);
LOOP
   FETCH c_encuestas INTO v_encuestaId, v_codigo, v_hogarId, v_hogarNumero, v_personas, v_region,
      v_departamento, v_municipio, v_barrio, v_direccion, v_encuestador, v_informante, v_desde,
      v_hasta, v_cierre, v_aprobadaPor, v_fechaAprobacion, v_visitas, v_dias, v_gasto;
   EXIT WHEN c_encuestas%NOTFOUND;
   v_total:=v_total+1;
   DBMS_OUTPUT.PUT_LINE(v_codigo||'  '||v_municipio||' ('||v_departamento||')  '||v_encuestador
      ||'  aprobada por '||NVL(v_aprobadaPor,'-')||'  dias: '||v_dias||'  gasto: '||v_gasto);
END LOOP;
CLOSE c_encuestas;
DBMS_OUTPUT.PUT_LINE(v_total||' encuesta(s) aprobada(s).');

BEGIN
   pr_exportar_aprobadas(NULL,NULL,-1,c_encuestas);
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Municipio que no existe:    '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
END;
/

PROMPT
PROMPT =============================================================
PROMPT  3. DISPARADORES DE VALIDACION
PROMPT =============================================================

DECLARE
v_encuestaId encuesta.id%TYPE;
v_registroId registro_diario.id%TYPE;
v_hogarId hogar.id%TYPE;
v_diaOcho registro_diario.id%TYPE;
BEGIN
-- Una encuesta en curso que tenga días registrados
SELECT MIN(e.id) INTO v_encuestaId FROM encuesta e
WHERE e.estado='EN_CURSO' AND EXISTS (SELECT 1 FROM registro_diario r WHERE r.encuesta_id=e.id);
SELECT MIN(id) INTO v_registroId FROM registro_diario WHERE encuesta_id=v_encuestaId;
SELECT hogar_id INTO v_hogarId FROM encuesta WHERE id=v_encuestaId;
DBMS_OUTPUT.PUT_LINE('Caso '||fn_codigo_encuesta(v_encuestaId)||', registro diario '||v_registroId);

-- 3.1 trg_val_registro_diario: fecha que no corresponde al día
BEGIN
   UPDATE registro_diario SET fecha=fecha+1 WHERE id=v_registroId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_registro_diario:      '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;

-- 3.2 trg_val_gasto_diario: gasto personal después del día 7
BEGIN
   SELECT MIN(id) INTO v_diaOcho FROM registro_diario WHERE encuesta_id=v_encuestaId AND numero_dia>7;
   IF v_diaOcho IS NULL THEN
      INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha)
      SELECT seq_registro_diario.NEXTVAL, v_encuestaId, d.dia, fn_fecha_dia(v_encuestaId,d.dia)
      FROM (SELECT MIN(n) dia FROM (SELECT LEVEL+7 n FROM dual CONNECT BY LEVEL<=7)
            WHERE n NOT IN (SELECT numero_dia FROM registro_diario WHERE encuesta_id=v_encuestaId)) d;
      SELECT MIN(id) INTO v_diaOcho FROM registro_diario WHERE encuesta_id=v_encuestaId AND numero_dia>7;
   END IF;
   INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, numero_orden, nombre_articulo,
      cantidad, unidad_medida, forma_adquisicion, valor, tipo_gasto)
   SELECT seq_gasto_diario.NEXTVAL, v_diaOcho, encuestador_id, 99, 'Prueba', 1, 'GRAMOS', 'REGALO', 1000, 'PERSONAL'
   FROM encuesta WHERE id=v_encuestaId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_gasto_diario:         '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;

-- 3.3 Con la encuesta cerrada no se puede escribir en ninguno de sus capítulos
pr_cerrar_encuesta(v_encuestaId,1,SYSTIMESTAMP);
BEGIN
   UPDATE registro_diario SET observaciones='cambio' WHERE id=v_registroId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_registro_diario:      '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
BEGIN
   UPDATE gasto_diario SET valor=valor+1 WHERE registro_diario_id=v_registroId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_gasto_diario:         '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
BEGIN
   UPDATE comida_fuera_hogar SET valor=valor+1 WHERE registro_diario_id=v_registroId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_comida_fuera_hogar:   '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
BEGIN
   UPDATE caracteristica_gasto SET updated_at=SYSTIMESTAMP WHERE encuesta_id=v_encuestaId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_caracteristica_gasto: '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
BEGIN
   UPDATE visita SET partes_pendientes='prueba' WHERE hogar_id=v_hogarId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_visita:               '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;

-- 3.4 trg_val_encuesta: una encuesta cerrada no se modifica ni salta de estado
BEGIN
   UPDATE encuesta SET observaciones='cambio' WHERE id=v_encuestaId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_encuesta (datos):     '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
pr_reabrir_encuesta(v_encuestaId);
BEGIN
   UPDATE encuesta SET estado='APROBADA' WHERE id=v_encuestaId;
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('trg_val_encuesta (estado):    '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;

ROLLBACK;
DBMS_OUTPUT.PUT_LINE('ROLLBACK: no quedan cambios de prueba.');
END;
/

PROMPT
PROMPT =============================================================
PROMPT  4. AUDITORIA (trg_cud_<tabla> + incluir_auditoria)
PROMPT =============================================================
PROMPT -- Ultimos 10 movimientos registrados por los disparadores de auditoria
COLUMN tabla FORMAT A20
COLUMN operacion FORMAT A12
COLUMN usuario_bd FORMAT A14
COLUMN descripcion FORMAT A60
SELECT * FROM (
   SELECT tabla, operacion, usuario_id, usuario_bd, descripcion
   FROM auditoria ORDER BY id DESC)
WHERE ROWNUM<=10;

PROMPT
PROMPT =============================================================
PROMPT  5. REPORTES (plsql/reportes.sql)
PROMPT =============================================================
-- Cada reporte es un texto de varias líneas: con LINESIZE corto SQL*Plus
-- lo partiría cada LINESIZE caracteres.
SET SERVEROUTPUT ON SIZE 1000000 FORMAT WRAPPED
SET LINESIZE 32767
SET TRIMOUT ON
SET TAB OFF

PROMPT
PROMPT -- 5.1 Todas las encuestas
DECLARE
v_mensaje VARCHAR2(32767);
BEGIN
v_mensaje:=fn_reporte_encuestas(NULL);
DBMS_OUTPUT.PUT_LINE(v_mensaje);
END;
/

PROMPT
PROMPT -- 5.2 Solo las encuestas pendientes de aprobacion
DECLARE
v_mensaje VARCHAR2(32767);
BEGIN
v_mensaje:=fn_reporte_encuestas('PENDIENTE_APROBACION');
DBMS_OUTPUT.PUT_LINE(v_mensaje);
END;
/

PROMPT
PROMPT -- 5.3 Encuestadores: encuestas que empezaron en los ultimos 90 dias
DECLARE
v_desde DATE:=TRUNC(SYSDATE)-90;
v_hasta DATE:=TRUNC(SYSDATE);
v_mensaje VARCHAR2(32767);
BEGIN
v_mensaje:=fn_reporte_encuestadores(v_desde,v_hasta);
DBMS_OUTPUT.PUT_LINE(v_mensaje);
END;
/

PROMPT
PROMPT -- 5.4 Gasto por region, departamento y municipio
DECLARE
v_mensaje VARCHAR2(32767);
BEGIN
v_mensaje:=fn_reporte_gasto_ubicacion;
DBMS_OUTPUT.PUT_LINE(v_mensaje);
END;
/

PROMPT
PROMPT -- 5.5 Historial de revisiones de la ultima encuesta revisada
DECLARE
v_encuestaId encuesta.id%TYPE;
v_mensaje VARCHAR2(32767);
BEGIN
SELECT MAX(encuesta_id) INTO v_encuestaId
FROM auditoria_encuesta
WHERE id=(SELECT MAX(id) FROM auditoria_encuesta);
IF v_encuestaId IS NULL THEN
   SELECT MIN(id) INTO v_encuestaId FROM encuesta;
END IF;
v_mensaje:=fn_reporte_revisiones(v_encuestaId);
DBMS_OUTPUT.PUT_LINE(v_mensaje);
END;
/

PROMPT
PROMPT -- 5.6 Errores controlados de los reportes (-20008 y -20001)
BEGIN
DBMS_OUTPUT.PUT_LINE(fn_reporte_encuestas('CERRADA'));
EXCEPTION
WHEN OTHERS THEN
   DBMS_OUTPUT.PUT_LINE('Error esperado: '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
/
BEGIN
DBMS_OUTPUT.PUT_LINE(fn_reporte_encuestadores(SYSDATE,SYSDATE-30));
EXCEPTION
WHEN OTHERS THEN
   DBMS_OUTPUT.PUT_LINE('Error esperado: '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
/
BEGIN
DBMS_OUTPUT.PUT_LINE(fn_reporte_revisiones(-1));
EXCEPTION
WHEN OTHERS THEN
   DBMS_OUTPUT.PUT_LINE('Error esperado: '||REGEXP_SUBSTR(SQLERRM,'[^'||CHR(10)||']+'));
END;
/

SET LINESIZE 150
SET FEEDBACK ON
