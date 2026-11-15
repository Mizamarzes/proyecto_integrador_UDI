-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Sincronización: vistas del esquema ORIGEN
--
-- Lo ejecuta instalar.sql al final, como dueño del ORIGEN (el esquema de
-- la aplicación). Parámetro: el 1er argumento = destino, que ya debe
-- existir (db-destino/01_crear_usuario.sql). Para un origen ya instalado
-- se puede ejecutar solo, desde esta carpeta:
--   sqlplus <origen>/<clave> @06_vistas_sincronizacion.sql <DESTINO>
--
-- Una vista por tabla con los datos que se envían, como en la guía
-- (vis_historiaClinica, vis_detalleHistoria). Solo se preparan las
-- encuestas APROBADAS y lo que cuelga de ellas:
--   vis_sinc_encuesta              encuestas en estado APROBADA
--   vis_sinc_vivienda, _hogar      vivienda y hogar de cada encuesta
--   vis_sinc_visita                capítulo B (las visitas son del hogar)
--   vis_sinc_caracteristica_gasto  capítulo C
--   vis_sinc_registro_diario       días 1 a 14
--   vis_sinc_gasto_diario          capítulo D
--   vis_sinc_comida_fuera_hogar    capítulo D1
--   vis_sinc_auditoria_encuesta    historial de revisiones
--   vis_sinc_rol, _usuario,        todos: el procedimiento del destino solo
--   vis_sinc_encuestador           inserta los que le falten (llaves foráneas
--                                  de created_by, encuestador_id, cerrada_por)
-- Al final se da SELECT sobre cada vista al destino. Va directo al usuario
-- (no por un rol) porque el PL/SQL de otro esquema no ve los privilegios
-- que llegan por roles.
--
-- Se puede volver a ejecutar (CREATE OR REPLACE). Si se cambian las
-- columnas de una tabla, se actualiza aquí y en
-- db-destino/05_procedimiento_sincronizacion.sql.
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
WHENEVER SQLERROR EXIT FAILURE
DEFINE destino = &1

CREATE OR REPLACE VIEW vis_sinc_encuesta AS
SELECT id, hogar_id, encuestador_id, informante_nombre, informante_orden,
       resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas,
       forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre,
       cerrada_por, created_at, updated_at, created_by
FROM encuesta
WHERE estado='APROBADA';

CREATE OR REPLACE VIEW vis_sinc_vivienda AS
SELECT v.id, v.municipio_id, v.clase, v.sector, v.seccion, v.manzana,
       v.segmento, v.edificacion_numero, v.vivienda_numero, v.barrio,
       v.direccion, v.telefono, v.created_at, v.updated_at, v.created_by
FROM vivienda v
WHERE v.id IN (SELECT h.vivienda_id
               FROM hogar h
               JOIN encuesta e ON e.hogar_id=h.id
               WHERE e.estado='APROBADA');

CREATE OR REPLACE VIEW vis_sinc_hogar AS
SELECT h.id, h.vivienda_id, h.hogar_numero, h.total_personas, h.ciclo,
       h.created_at, h.updated_at, h.created_by
FROM hogar h
JOIN encuesta e ON e.hogar_id=h.id
WHERE e.estado='APROBADA';

CREATE OR REPLACE VIEW vis_sinc_visita AS
SELECT vi.id, vi.hogar_id, vi.numero_visita, vi.fecha, vi.hora_inicio,
       vi.hora_fin, vi.resultado_visita, vi.partes_pendientes,
       vi.fecha_proxima, vi.hora_proxima, vi.created_at, vi.updated_at,
       vi.created_by
FROM visita vi
JOIN encuesta e ON e.hogar_id=vi.hogar_id
WHERE e.estado='APROBADA';

CREATE OR REPLACE VIEW vis_sinc_caracteristica_gasto AS
SELECT cg.id, cg.encuesta_id, cg.grupo_alimento_id, cg.encuestador_id,
       cg.frecuencia_compra, cg.valor_ultima_compra, cg.comprara_en_periodo,
       cg.created_at, cg.updated_at, cg.created_by
FROM caracteristica_gasto cg
JOIN encuesta e ON e.id=cg.encuesta_id
WHERE e.estado='APROBADA';

CREATE OR REPLACE VIEW vis_sinc_registro_diario AS
SELECT rd.id, rd.encuesta_id, rd.numero_dia, rd.fecha, rd.observaciones,
       rd.created_at, rd.updated_at, rd.created_by
FROM registro_diario rd
JOIN encuesta e ON e.id=rd.encuesta_id
WHERE e.estado='APROBADA';

-- encuesta_id viene del día, para filtrar por encuesta en el destino
CREATE OR REPLACE VIEW vis_sinc_gasto_diario AS
SELECT g.id, g.registro_diario_id, g.encuestador_id, g.lugar_compra_id,
       g.numero_orden, g.nombre_articulo, g.cantidad, g.unidad_medida,
       g.unidad_medida_otra, g.equivalencia_gr, g.equivalencia_cm3,
       g.forma_adquisicion, g.forma_adquisicion_otra, g.valor,
       g.frecuencia_compra, g.tipo_gasto, g.beneficiario_mesada,
       g.created_at, g.updated_at, g.created_by, rd.encuesta_id
FROM gasto_diario g
JOIN registro_diario rd ON rd.id=g.registro_diario_id
JOIN encuesta e ON e.id=rd.encuesta_id
WHERE e.estado='APROBADA';

CREATE OR REPLACE VIEW vis_sinc_comida_fuera_hogar AS
SELECT cf.id, cf.registro_diario_id, cf.encuestador_id, cf.lugar_compra_id,
       cf.numero_orden, cf.descripcion, cf.tipo_comida, cf.cantidad,
       cf.forma_adquisicion, cf.forma_adquisicion_otra, cf.valor,
       cf.frecuencia_compra, cf.tipo_gasto, cf.a_domicilio,
       cf.created_at, cf.updated_at, cf.created_by, rd.encuesta_id
FROM comida_fuera_hogar cf
JOIN registro_diario rd ON rd.id=cf.registro_diario_id
JOIN encuesta e ON e.id=rd.encuesta_id
WHERE e.estado='APROBADA';

CREATE OR REPLACE VIEW vis_sinc_auditoria_encuesta AS
SELECT a.id, a.encuesta_id, a.accion, a.observacion, a.created_at,
       a.updated_at, a.created_by
FROM auditoria_encuesta a
JOIN encuesta e ON e.id=a.encuesta_id
WHERE e.estado='APROBADA';

CREATE OR REPLACE VIEW vis_sinc_rol AS
SELECT id, nombre, created_at, updated_at, created_by
FROM rol;

CREATE OR REPLACE VIEW vis_sinc_usuario AS
SELECT id, email, password, nombre, apellido, telefono, rol_id, activo,
       ultimo_acceso, created_at, updated_at, created_by
FROM usuario;

CREATE OR REPLACE VIEW vis_sinc_encuestador AS
SELECT id, usuario_id, created_at, updated_at, created_by
FROM encuestador;

-- Privilegio de consultar las vistas al esquema destino (paso 3 de la guía)
GRANT SELECT ON vis_sinc_encuesta TO &destino;
GRANT SELECT ON vis_sinc_vivienda TO &destino;
GRANT SELECT ON vis_sinc_hogar TO &destino;
GRANT SELECT ON vis_sinc_visita TO &destino;
GRANT SELECT ON vis_sinc_caracteristica_gasto TO &destino;
GRANT SELECT ON vis_sinc_registro_diario TO &destino;
GRANT SELECT ON vis_sinc_gasto_diario TO &destino;
GRANT SELECT ON vis_sinc_comida_fuera_hogar TO &destino;
GRANT SELECT ON vis_sinc_auditoria_encuesta TO &destino;
GRANT SELECT ON vis_sinc_rol TO &destino;
GRANT SELECT ON vis_sinc_usuario TO &destino;
GRANT SELECT ON vis_sinc_encuestador TO &destino;

PROMPT Vistas de sincronizacion creadas y concedidas a &destino.
