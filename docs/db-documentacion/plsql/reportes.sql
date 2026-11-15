-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Reportes en PL/SQL (Oracle 10g)
--
-- Se ejecuta como dueño del esquema, después de plsql/funciones.sql (usa
-- fn_gasto_total_encuesta, fn_codigo_encuesta, fn_descripcion_estado y
-- fn_estado_encuesta). Lo llama esquema_oracle10g.sql; para una base que
-- ya existe:
--   sqlplus <esquema>/<clave> @plsql/reportes.sql
--
-- Cada reporte es una función que recorre un cursor y arma el texto del
-- reporte línea por línea (CHR(10)). Se invocan desde un bloque anónimo:
--   SET SERVEROUTPUT ON SIZE 1000000 FORMAT WRAPPED
--   SET LINESIZE 32767 TRIMOUT ON TAB OFF
--   DECLARE
--   v_mensaje VARCHAR2(32767);
--   BEGIN
--   v_mensaje:=fn_reporte_encuestas(NULL);
--   DBMS_OUTPUT.PUT_LINE(v_mensaje);
--   END;
--   /
-- (con LINESIZE corto SQL*Plus parte el texto cada LINESIZE caracteres).
-- Hay ejemplos de los cuatro en pruebas/bloques_anonimos.sql.
--
--   fn_reporte_encuestas(estado)          Encuestas con encuestador, municipio,
--                                         días registrados y gasto total.
--   fn_reporte_encuestadores(desde,hasta) Encuestas de cada encuestador por
--                                         estado y su gasto registrado.
--   fn_reporte_gasto_ubicacion            Hogares encuestados y gasto por
--                                         región, departamento y municipio.
--   fn_reporte_revisiones(encuesta)       Historial de aprobaciones y rechazos
--                                         de una encuesta.
--
-- El texto se limita a 32.000 caracteres (máximo de un VARCHAR2 en PL/SQL);
-- si un reporte no cabe, termina con "... (reporte recortado)".
-- Los mensajes no llevan «ñ» ni mayúsculas con tilde: la base usa ZHS16GBK.
-- =====================================================================

SET DEFINE OFF

-- ---------------------------------------------------------------------
-- Columna de ancho fijo para los reportes: recorta el texto y lo completa
-- con espacios. No se usa RPAD(texto,n) porque en ZHS16GBK RPAD cuenta las
-- letras con tilde como dos posiciones y la columna queda corrida.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_columna
(v_texto VARCHAR2,
 v_ancho NUMBER)
RETURN VARCHAR2
IS
v_valor VARCHAR2(4000);
BEGIN
v_valor:=SUBSTR(NVL(v_texto,'-'),1,v_ancho-1);
RETURN v_valor||RPAD(' ',v_ancho-LENGTH(v_valor));
END;
/

-- ---------------------------------------------------------------------
-- Reporte de encuestas. v_estado filtra por estado (EN_CURSO,
-- PENDIENTE_APROBACION, APROBADA, RECHAZADA); con NULL muestra todas.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reporte_encuestas
(v_estado encuesta.estado%TYPE)
RETURN VARCHAR2
IS
v_mensaje VARCHAR2(32767);
v_linea VARCHAR2(500);
v_cantidad NUMBER:=0;
v_gastoTotal NUMBER:=0;

CURSOR c_encuesta IS
SELECT e.id, e.estado, e.fecha_desde,
u.nombre||' '||u.apellido AS encuestador,
m.nombre AS municipio,
(SELECT COUNT(*) FROM registro_diario r WHERE r.encuesta_id=e.id) AS dias,
fn_gasto_total_encuesta(e.id) AS gasto
FROM encuesta e
JOIN encuestador en ON en.id=e.encuestador_id
JOIN usuario u ON u.id=en.usuario_id
JOIN hogar h ON h.id=e.hogar_id
JOIN vivienda v ON v.id=h.vivienda_id
JOIN municipio m ON m.id=v.municipio_id
WHERE v_estado IS NULL OR e.estado=v_estado
ORDER BY e.id;

BEGIN
IF v_estado IS NOT NULL
   AND v_estado NOT IN ('EN_CURSO','PENDIENTE_APROBACION','APROBADA','RECHAZADA') THEN
   RAISE_APPLICATION_ERROR(-20008,'El estado '||v_estado||' no existe.');
END IF;

v_mensaje:='REPORTE DE ENCUESTAS'
   ||CASE WHEN v_estado IS NOT NULL THEN ' - '||fn_descripcion_estado(v_estado) END||CHR(10)
   ||RPAD('Caso',10)||RPAD('Estado',25)||RPAD('Encuestador',24)||RPAD('Municipio',20)
   ||RPAD('Inicio',12)||LPAD('Dias',5)||LPAD('Gasto total',14)||CHR(10)
   ||RPAD('-',110,'-')||CHR(10);

FOR r_encuesta IN c_encuesta LOOP
   v_linea:=RPAD(fn_codigo_encuesta(r_encuesta.id),10)
      ||fn_columna(fn_descripcion_estado(r_encuesta.estado),25)
      ||fn_columna(r_encuesta.encuestador,24)
      ||fn_columna(r_encuesta.municipio,20)
      ||RPAD(TO_CHAR(r_encuesta.fecha_desde,'YYYY-MM-DD'),12)
      ||LPAD(r_encuesta.dias,5)
      ||LPAD(TO_CHAR(r_encuesta.gasto,'FM999,999,999,990'),14)||CHR(10);
   IF LENGTH(v_mensaje)+LENGTH(v_linea)>32000 THEN
      v_mensaje:=v_mensaje||'... (reporte recortado)'||CHR(10);
      EXIT;
   END IF;
   v_mensaje:=v_mensaje||v_linea;
   v_cantidad:=v_cantidad+1;
   v_gastoTotal:=v_gastoTotal+r_encuesta.gasto;
END LOOP;

IF v_cantidad=0 THEN
   RETURN v_mensaje||'No hay encuestas.';
END IF;
RETURN v_mensaje||RPAD('-',110,'-')||CHR(10)
   ||'Total: '||v_cantidad||' encuestas, gasto registrado '
   ||TO_CHAR(v_gastoTotal,'FM999,999,999,990')||'.';
END;
/

-- ---------------------------------------------------------------------
-- Productividad de los encuestadores: cuántas encuestas tiene cada uno en
-- cada estado y el gasto que registró. Cuenta las encuestas que empiezan
-- (fecha_desde) entre v_desde y v_hasta; con NULL no se limita ese lado.
-- Los encuestadores sin encuestas en el rango aparecen con 0.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reporte_encuestadores
(v_desde DATE,
 v_hasta DATE)
RETURN VARCHAR2
IS
v_mensaje VARCHAR2(32767);
v_linea VARCHAR2(500);
v_encuestas NUMBER:=0;
v_gastoTotal NUMBER:=0;

CURSOR c_encuestador IS
SELECT u.nombre||' '||u.apellido AS encuestador,
COUNT(e.id) AS total,
SUM(CASE WHEN e.estado='EN_CURSO' THEN 1 ELSE 0 END) AS en_curso,
SUM(CASE WHEN e.estado='PENDIENTE_APROBACION' THEN 1 ELSE 0 END) AS pendientes,
SUM(CASE WHEN e.estado='APROBADA' THEN 1 ELSE 0 END) AS aprobadas,
SUM(CASE WHEN e.estado='RECHAZADA' THEN 1 ELSE 0 END) AS rechazadas,
NVL(SUM(CASE WHEN e.id IS NOT NULL THEN fn_gasto_total_encuesta(e.id) END),0) AS gasto
FROM encuestador en
JOIN usuario u ON u.id=en.usuario_id
LEFT JOIN encuesta e ON e.encuestador_id=en.id
   AND (v_desde IS NULL OR e.fecha_desde>=TRUNC(v_desde))
   AND (v_hasta IS NULL OR e.fecha_desde<TRUNC(v_hasta)+1)
GROUP BY en.id, u.nombre, u.apellido
ORDER BY total DESC, encuestador;

BEGIN
IF v_desde IS NOT NULL AND v_hasta IS NOT NULL AND TRUNC(v_desde)>TRUNC(v_hasta) THEN
   RAISE_APPLICATION_ERROR(-20008,'La fecha inicial ('||TO_CHAR(v_desde,'YYYY-MM-DD')
      ||') es posterior a la final ('||TO_CHAR(v_hasta,'YYYY-MM-DD')||').');
END IF;

v_mensaje:='REPORTE DE ENCUESTADORES ('
   ||NVL(TO_CHAR(v_desde,'YYYY-MM-DD'),'inicio')||' a '
   ||NVL(TO_CHAR(v_hasta,'YYYY-MM-DD'),'hoy')||')'||CHR(10)
   ||RPAD('Encuestador',26)||LPAD('Total',6)||LPAD('En curso',10)||LPAD('Pendientes',12)
   ||LPAD('Aprobadas',11)||LPAD('Rechazadas',12)||LPAD('Gasto registrado',18)||CHR(10)
   ||RPAD('-',95,'-')||CHR(10);

FOR r_encuestador IN c_encuestador LOOP
   v_linea:=fn_columna(r_encuestador.encuestador,26)
      ||LPAD(r_encuestador.total,6)
      ||LPAD(r_encuestador.en_curso,10)
      ||LPAD(r_encuestador.pendientes,12)
      ||LPAD(r_encuestador.aprobadas,11)
      ||LPAD(r_encuestador.rechazadas,12)
      ||LPAD(TO_CHAR(r_encuestador.gasto,'FM999,999,999,990'),18)||CHR(10);
   IF LENGTH(v_mensaje)+LENGTH(v_linea)>32000 THEN
      v_mensaje:=v_mensaje||'... (reporte recortado)'||CHR(10);
      EXIT;
   END IF;
   v_mensaje:=v_mensaje||v_linea;
   v_encuestas:=v_encuestas+r_encuestador.total;
   v_gastoTotal:=v_gastoTotal+r_encuestador.gasto;
END LOOP;

RETURN v_mensaje||RPAD('-',95,'-')||CHR(10)
   ||'Total: '||v_encuestas||' encuestas, gasto registrado '
   ||TO_CHAR(v_gastoTotal,'FM999,999,999,990')||'.';
END;
/

-- ---------------------------------------------------------------------
-- Gasto por ubicación: hogares encuestados, gasto total y gasto promedio
-- por hogar en cada municipio, ordenado por región y departamento.
-- Solo aparecen los municipios que tienen encuestas.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reporte_gasto_ubicacion
RETURN VARCHAR2
IS
v_mensaje VARCHAR2(32767);
v_linea VARCHAR2(500);
v_hogares NUMBER:=0;
v_gastoTotal NUMBER:=0;

CURSOR c_ubicacion IS
SELECT r.nombre AS region, d.nombre AS departamento, m.nombre AS municipio,
COUNT(e.id) AS hogares,
SUM(fn_gasto_total_encuesta(e.id)) AS gasto
FROM encuesta e
JOIN hogar h ON h.id=e.hogar_id
JOIN vivienda v ON v.id=h.vivienda_id
JOIN municipio m ON m.id=v.municipio_id
JOIN departamento d ON d.id=m.departamento_id
JOIN region r ON r.id=d.region_id
GROUP BY r.nombre, d.nombre, m.nombre
ORDER BY r.nombre, d.nombre, m.nombre;

BEGIN
v_mensaje:='REPORTE DE GASTO POR UBICACION'||CHR(10)
   ||RPAD('Region',20)||RPAD('Departamento',22)||RPAD('Municipio',22)
   ||LPAD('Hogares',8)||LPAD('Gasto total',14)||LPAD('Promedio hogar',16)||CHR(10)
   ||RPAD('-',102,'-')||CHR(10);

FOR r_ubicacion IN c_ubicacion LOOP
   v_linea:=fn_columna(r_ubicacion.region,20)
      ||fn_columna(r_ubicacion.departamento,22)
      ||fn_columna(r_ubicacion.municipio,22)
      ||LPAD(r_ubicacion.hogares,8)
      ||LPAD(TO_CHAR(r_ubicacion.gasto,'FM999,999,999,990'),14)
      ||LPAD(TO_CHAR(ROUND(r_ubicacion.gasto/r_ubicacion.hogares),'FM999,999,999,990'),16)||CHR(10);
   IF LENGTH(v_mensaje)+LENGTH(v_linea)>32000 THEN
      v_mensaje:=v_mensaje||'... (reporte recortado)'||CHR(10);
      EXIT;
   END IF;
   v_mensaje:=v_mensaje||v_linea;
   v_hogares:=v_hogares+r_ubicacion.hogares;
   v_gastoTotal:=v_gastoTotal+r_ubicacion.gasto;
END LOOP;

IF v_hogares=0 THEN
   RETURN v_mensaje||'No hay encuestas.';
END IF;
RETURN v_mensaje||RPAD('-',102,'-')||CHR(10)
   ||'Total: '||v_hogares||' hogares, gasto registrado '
   ||TO_CHAR(v_gastoTotal,'FM999,999,999,990')||', promedio por hogar '
   ||TO_CHAR(ROUND(v_gastoTotal/v_hogares),'FM999,999,999,990')||'.';
END;
/

-- ---------------------------------------------------------------------
-- Historial de revisiones (aprobaciones y rechazos) de una encuesta, con
-- el crítico que la revisó y la observación. Falla con -20001 si la
-- encuesta no existe.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reporte_revisiones
(v_encuestaId encuesta.id%TYPE)
RETURN VARCHAR2
IS
v_mensaje VARCHAR2(32767);
v_linea VARCHAR2(1500);
v_estado encuesta.estado%TYPE;
v_cantidad NUMBER:=0;

CURSOR c_revision IS
SELECT a.created_at, a.accion, a.observacion,
NVL(u.nombre||' '||u.apellido,'(sin usuario)') AS critico
FROM auditoria_encuesta a
LEFT JOIN usuario u ON u.id=a.created_by
WHERE a.encuesta_id=v_encuestaId
ORDER BY a.created_at, a.id;

BEGIN
v_estado:=fn_estado_encuesta(v_encuestaId);

v_mensaje:='HISTORIAL DE REVISIONES DE LA ENCUESTA '||fn_codigo_encuesta(v_encuestaId)
   ||' (estado actual: '||fn_descripcion_estado(v_estado)||')'||CHR(10)
   ||RPAD('Fecha',18)||RPAD('Accion',11)||RPAD('Critico',26)||'Observacion'||CHR(10)
   ||RPAD('-',100,'-')||CHR(10);

FOR r_revision IN c_revision LOOP
   v_linea:=RPAD(TO_CHAR(r_revision.created_at,'YYYY-MM-DD HH24:MI'),18)
      ||RPAD(r_revision.accion,11)
      ||fn_columna(r_revision.critico,26)
      ||NVL(r_revision.observacion,'-')||CHR(10);
   IF LENGTH(v_mensaje)+LENGTH(v_linea)>32000 THEN
      v_mensaje:=v_mensaje||'... (reporte recortado)'||CHR(10);
      EXIT;
   END IF;
   v_mensaje:=v_mensaje||v_linea;
   v_cantidad:=v_cantidad+1;
END LOOP;

IF v_cantidad=0 THEN
   RETURN v_mensaje||'La encuesta no tiene revisiones.';
END IF;
RETURN v_mensaje||RPAD('-',100,'-')||CHR(10)||'Total: '||v_cantidad||' revisiones.';
END;
/
