-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Datos iniciales y de prueba del ORIGEN
--
-- Lo ejecuta instalar.sql. Roles, módulos y permisos (los necesita la
-- aplicación para el menú), usuarios (clave 123456), encuestadores y casos
-- de prueba. Usa ids fijos (1, 2, 3...): solo funciona en un esquema
-- recién creado. El destino no los tiene: le llegan con la sincronización.
-- =====================================================================

SET DEFINE OFF
-- ---------------------------------------------------------------------
-- Roles (3)
-- ---------------------------------------------------------------------
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'ADMIN');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'CRITICO_DIGITADOR');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'ENCUESTADOR');

-- ---------------------------------------------------------------------
-- Usuarios (12): 1 administrador, 1 crítico-digitador y 10 encuestadores.
-- ---------------------------------------------------------------------
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'admin@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Ana María', 'Rojas', '3001112233', 1, 'S', TIMESTAMP '2026-10-09 08:15:00', NULL);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'cperez@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Carlos', 'Pérez', '3002223344', 2, 'S', TIMESTAMP '2026-10-09 07:50:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'lmartinez@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Laura', 'Martínez', '3003334455', 3, 'S', TIMESTAMP '2026-10-09 07:30:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'jgarcia@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Juan', 'García', '3004445566', 3, 'S', TIMESTAMP '2026-10-08 16:20:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'mlopez@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'María', 'López', '3005556677', 3, 'S', TIMESTAMP '2026-10-08 09:05:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'dramirez@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Diego', 'Ramírez', '3006667788', 3, 'S', TIMESTAMP '2026-10-07 14:40:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'shernandez@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Sofía', 'Hernández', '3007778899', 3, 'S', TIMESTAMP '2026-10-09 10:00:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'ftorres@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Felipe', 'Torres', '3008889900', 3, 'S', TIMESTAMP '2026-10-06 11:25:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'vmoreno@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Valentina', 'Moreno', '3009990011', 3, 'S', TIMESTAMP '2026-10-09 08:45:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'agomez@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Andrés', 'Gómez', '3010001122', 3, 'S', TIMESTAMP '2026-10-05 15:10:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'cvargas@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Camila', 'Vargas', '3011112233', 3, 'S', TIMESTAMP '2026-10-09 09:30:00', 1);
INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, ultimo_acceso, created_by)
  VALUES (seq_usuario.NEXTVAL, 'rcastillo@enph.udi.edu.co', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Ricardo', 'Castillo', '3012223344', 3, 'N', TIMESTAMP '2026-09-20 17:00:00', 1);

-- ---------------------------------------------------------------------
-- Encuestadores (10): usuarios 3 al 12
-- ---------------------------------------------------------------------
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 3, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 4, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 5, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 6, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 7, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 8, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 9, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 10, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 11, 1);
INSERT INTO encuestador (id, usuario_id, created_by) VALUES (seq_encuestador.NEXTVAL, 12, 1);

-- ---------------------------------------------------------------------
-- Módulos (6) y permisos por rol (7)
-- Cada módulo agrupa sus pantallas: «viviendas» incluye los hogares y
-- «encuestas» incluye control de visitas, capítulo C, gastos y comidas.
-- ---------------------------------------------------------------------
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Usuarios', 'usuarios', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Roles y permisos', 'roles', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Viviendas y hogares', 'viviendas', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Encuestas', 'encuestas', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Reportes', 'reportes', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Verificación', 'verificacion', 1);

-- ADMIN: usuarios y roles | CRITICO_DIGITADOR: encuestas, reportes y verificación | ENCUESTADOR: viviendas y encuestas
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 1, 1, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 1, 2, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 2, 4, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 2, 5, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 2, 6, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 3, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 4, 1);
-- ---------------------------------------------------------------------
-- Viviendas (10)
-- ---------------------------------------------------------------------
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 118, 1, '0101', '01', '07', '001', 12, 1, 'Cabecera del Llano', 'Calle 45 # 12-30', '6076345678', 3);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 118, 1, '0102', '03', '15', '002', 4, 1, 'San Alonso', 'Carrera 27 # 18-45', '6076451234', 4);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 342, 1, '0201', '02', '22', '003', 8, 2, 'Canaveral', 'Calle 30 # 25-71', '6076389012', 5);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 374, 1, '0301', '01', '05', '004', 3, 1, 'Centro', 'Carrera 25 # 30-12', '6076467890', 6);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 661, 1, '0401', '04', '11', '005', 6, 1, 'La Candelaria', 'Calle 9 # 6-20', '6076553210', 7);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 275, 1, '0501', '02', '18', '006', 9, 3, 'La Riviera', 'Avenida 0 # 12-34', '6075781234', 8);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 1013, 1, '0601', '01', '02', '007', 1, 1, 'Centro', 'Calle 19 # 9-35', '6087423456', 9);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 547, 1, '0701', '05', '33', '008', 15, 2, 'Laureles', 'Circular 4 # 73-12', '6044112233', 10);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 88, 1, '0801', '03', '27', '009', 7, 1, 'El Prado', 'Carrera 54 # 64-120', '6053445566', 11);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 150, 1, '1001', '02', '09', '010', 5, 1, 'San Fernando', 'Calle 5 # 38-25', '6025567788', 12);

-- ---------------------------------------------------------------------
-- Hogares (10)
-- ---------------------------------------------------------------------
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 1, 1, 4, 'C01', 3);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 1, 2, 1, 'C01', 3);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 2, 1, 3, 'C01', 4);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 3, 1, 5, 'C01', 5);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 4, 1, 2, 'C02', 6);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 5, 1, 3, 'C02', 7);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 6, 1, 6, 'C02', 8);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 7, 1, 2, 'C02', 9);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 8, 1, 4, 'C03', 10);
INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) VALUES (seq_hogar.NEXTVAL, 9, 1, 3, 'C03', 11);

-- ---------------------------------------------------------------------
-- Encuestas (10): una por hogar. Las 3, 4 y 5 están cerradas y pendientes
-- de aprobación (cerró el usuario 2); la 6 fue reabierta por un
-- administrador y volvió a estar en curso; las demás están en curso.
-- ---------------------------------------------------------------------
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 1, 1, 'Paola Andrea Gómez', 1, 'EI', DATE '2026-09-28', DATE '2026-10-11', NULL, NULL, NULL, NULL, 'EN_CURSO', NULL, NULL, 3);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 2, 1, 'Julián Ortiz', 1, 'EI', DATE '2026-09-28', DATE '2026-10-11', NULL, NULL, NULL, 'Estudiante que arrienda una habitación', 'EN_CURSO', NULL, NULL, 3);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 3, 2, 'Marta Lucía Díaz', 2, 'EC', DATE '2026-09-01', DATE '2026-09-14', 'N', 'EFECTIVO', NULL, NULL, 'PENDIENTE_APROBACION', TIMESTAMP '2026-09-15 10:00:00', 2, 4);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 4, 3, 'Jorge Iván Castro', 1, 'EC', DATE '2026-09-01', DATE '2026-09-14', 'S', 'TARJETA_DEBITO', NULL, 'Cumpleanos el día 9', 'PENDIENTE_APROBACION', TIMESTAMP '2026-09-15 15:30:00', 2, 5);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 5, 4, 'Luz Dary Mantilla', 1, 'EC', DATE '2026-09-08', DATE '2026-09-21', 'N', 'FIADO', NULL, NULL, 'PENDIENTE_APROBACION', TIMESTAMP '2026-09-22 09:20:00', 2, 6);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 6, 5, 'Rosa Elena Pabón', 2, 'EI', DATE '2026-09-15', DATE '2026-09-28', 'N', 'OTRA', 'Transferencia bancaria', 'Reabierta para corregir el día 3', 'EN_CURSO', TIMESTAMP '2026-09-29 11:00:00', 2, 7);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 7, 6, 'Hernán Villamizar', 1, 'AT', DATE '2026-10-01', DATE '2026-10-14', NULL, NULL, NULL, 'Informante de viaje hasta el 6 de octubre', 'EN_CURSO', NULL, NULL, 8);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 8, 7, 'Claudia Suárez', 1, 'EI', DATE '2026-10-01', DATE '2026-10-14', NULL, NULL, NULL, NULL, 'EN_CURSO', NULL, NULL, 9);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 9, 8, 'Andrés Restrepo', 1, 'EI', DATE '2026-10-05', DATE '2026-10-18', NULL, NULL, NULL, NULL, 'EN_CURSO', NULL, NULL, 10);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 10, 9, 'Yolanda Charris', 2, 'EI', DATE '2026-10-05', DATE '2026-10-18', NULL, NULL, NULL, NULL, 'EN_CURSO', NULL, NULL, 11);

-- ---------------------------------------------------------------------
-- Visitas (10): 4 al hogar 1, 3 al hogar 3 y 3 al hogar 4
-- ---------------------------------------------------------------------
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 1, 1, DATE '2026-09-28', TIMESTAMP '2026-09-28 08:00:00', TIMESTAMP '2026-09-28 08:50:00', 'EI', 'Capítulo D, días 1 a 14', DATE '2026-10-01', TIMESTAMP '2026-10-01 09:00:00', 3);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 1, 2, DATE '2026-10-01', TIMESTAMP '2026-10-01 09:00:00', TIMESTAMP '2026-10-01 09:25:00', 'EI', 'Capítulo D, días 5 a 14', DATE '2026-10-05', TIMESTAMP '2026-10-05 09:00:00', 3);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 1, 3, DATE '2026-10-05', TIMESTAMP '2026-10-05 09:10:00', TIMESTAMP '2026-10-05 09:40:00', 'EI', 'Capítulo D, días 9 a 14', DATE '2026-10-09', TIMESTAMP '2026-10-09 14:00:00', 3);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 1, 4, DATE '2026-10-09', TIMESTAMP '2026-10-09 14:00:00', TIMESTAMP '2026-10-09 14:30:00', 'EI', 'Capítulo D, días 13 y 14, y D2', DATE '2026-10-12', TIMESTAMP '2026-10-12 08:30:00', 3);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 3, 1, DATE '2026-09-01', TIMESTAMP '2026-09-01 10:00:00', TIMESTAMP '2026-09-01 11:00:00', 'EI', 'Capítulo D, días 1 a 14', DATE '2026-09-07', TIMESTAMP '2026-09-07 10:00:00', 4);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 3, 2, DATE '2026-09-07', TIMESTAMP '2026-09-07 10:00:00', TIMESTAMP '2026-09-07 10:30:00', 'EI', 'Capítulo D, días 7 a 14', DATE '2026-09-15', TIMESTAMP '2026-09-15 09:00:00', 4);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 3, 3, DATE '2026-09-15', TIMESTAMP '2026-09-15 09:00:00', TIMESTAMP '2026-09-15 09:45:00', 'EC', NULL, NULL, NULL, 4);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 4, 1, DATE '2026-09-01', TIMESTAMP '2026-09-01 15:00:00', TIMESTAMP '2026-09-01 16:10:00', 'EI', 'Capítulo D, días 1 a 14', DATE '2026-09-08', TIMESTAMP '2026-09-08 15:00:00', 5);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 4, 2, DATE '2026-09-08', TIMESTAMP '2026-09-08 15:00:00', TIMESTAMP '2026-09-08 15:20:00', 'NH', 'Capítulo D, días 8 a 14', DATE '2026-09-15', TIMESTAMP '2026-09-15 14:00:00', 5);
INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by)
  VALUES (seq_visita.NEXTVAL, 4, 3, DATE '2026-09-15', TIMESTAMP '2026-09-15 14:00:00', TIMESTAMP '2026-09-15 15:15:00', 'EC', NULL, NULL, NULL, 5);

-- ---------------------------------------------------------------------
-- Capítulo C (10): 
-- ---------------------------------------------------------------------
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 1, 1, 'DIARIO', NULL, NULL, 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 2, 1, 'SEMANAL', NULL, NULL, 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 3, 1, 'QUINCENAL', NULL, NULL, 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 4, 1, 'SEMANAL', NULL, NULL, 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 5, 1, 'SEMANAL', NULL, NULL, 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 6, 1, 'MENSUAL', 15000, 'S', 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 7, 1, 'ESPORADICA', 25000, 'N', 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 8, 1, 'DIARIO', NULL, NULL, 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 9, 1, 'VARIAS_VECES_SEMANA', NULL, NULL, 3);
INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) VALUES (seq_caracteristica_gasto.NEXTVAL, 1, 10, 1, 'NUNCA', NULL, NULL, 3);

-- ---------------------------------------------------------------------
-- Registros diarios (10): días 1 a 6 de la encuesta 1 y días 1 a 4 de
-- la encuesta 3. Fecha = fecha_desde + numero_dia - 1.
-- ---------------------------------------------------------------------
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 1, 1, DATE '2026-09-28', NULL, 3);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 1, 2, DATE '2026-09-29', NULL, 3);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 1, 3, DATE '2026-09-30', 'Mesada semanal del hijo', 3);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 1, 4, DATE '2026-10-01', NULL, 3);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 1, 5, DATE '2026-10-02', 'Sin compras este día', 3);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 1, 6, DATE '2026-10-03', NULL, 3);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 3, 1, DATE '2026-09-01', NULL, 4);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 3, 2, DATE '2026-09-02', NULL, 4);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 3, 3, DATE '2026-09-03', NULL, 4);
INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, created_by) VALUES (seq_registro_diario.NEXTVAL, 3, 4, DATE '2026-09-04', NULL, 4);

-- ---------------------------------------------------------------------
-- Capítulo D (10): renglones de los días 1, 2 y 3 de la encuesta 1,
-- basados en el ejemplo de Paola del cuadernillo
-- ---------------------------------------------------------------------
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 1, 1, 1, 1, 'Hígado de res', 2, 'LIBRA', NULL, NULL, NULL, 'COMPRA', NULL, 14000, 'SEMANAL', 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 1, 1, 10, 2, 'Manzana grande', 1, 'KILO', NULL, NULL, NULL, 'COMPRA', NULL, 4000, 'QUINCENAL', 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 1, 1, 10, 3, 'Cilantro', 1, 'OTRA', 'Manojo', 50, NULL, 'COMPRA', NULL, 300, 'SEMANAL', 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 1, 1, 6, 4, 'Huevos', 4, 'OTRA', 'Huevo grande', 60, NULL, 'COMPRA', NULL, 1200, 'DIARIO', 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 1, 1, NULL, 5, 'Gaseosa', 2, 'LITRO', NULL, NULL, NULL, 'REGALO', NULL, 3500, NULL, 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 1, 1, NULL, 6, 'Pan blandito pequeno', 10, 'OTRA', 'Pan pequeno', NULL, NULL, 'REGALO', NULL, 2000, NULL, 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 2, 1, NULL, 1, 'Plátano verde mediano', 1, 'OTRA', 'Racimo', NULL, NULL, 'PRODUCCION_PROPIA', NULL, 8000, NULL, 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 2, 1, 16, 2, 'Gasolina corriente', 7, 'OTRA', 'Galón', NULL, 3785, 'COMPRA', NULL, 58000, 'SEMANAL', 'PERSONAL', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 2, 1, 6, 3, 'Leche entera en bolsa', 2, 'LITRO', NULL, NULL, NULL, 'COMPRA', NULL, 6400, 'DIARIO', 'HOGAR', NULL, 3);
INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by)
  VALUES (seq_gasto_diario.NEXTVAL, 3, 1, NULL, 1, 'Mesada semanal', 1, 'OTRA', 'Mesada', NULL, NULL, 'OTRA', 'Mesada entregada', 20000, NULL, 'HOGAR', 'Santiago Gómez', 3);

-- ---------------------------------------------------------------------
-- Capítulo D1 (10): comidas fuera del hogar de los días 1 a 4 de la
-- encuesta 1. Los gastos personales están en días <= 7.
-- ---------------------------------------------------------------------
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 1, 1, 19, 1, 'Sándwich de pollo', 'CENA', 0.5, 'COMPRA', NULL, 6000, 'MENSUAL', 'HOGAR', 'S', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 1, 1, 19, 2, 'Ensalada de frutas', 'DESAYUNO', 1, 'COMPRA', NULL, 6500, 'MENSUAL', 'HOGAR', 'N', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 1, 1, 18, 3, 'Almuerzo corriente (pollo, ensalada, arroz, sopa)', 'ALMUERZO', 1, 'COMPRA', NULL, 10000, 'ESPORADICA', 'PERSONAL', 'N', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 2, 1, 13, 1, 'Empanada de carne', 'ONCES', 2, 'COMPRA', NULL, 3000, 'VARIAS_VECES_SEMANA', 'HOGAR', 'N', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 2, 1, 22, 2, 'Pizza familiar', 'CENA', 1, 'COMPRA', NULL, 45000, 'MENSUAL', 'HOGAR', 'S', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 3, 1, 19, 1, 'Desayuno con huevos, chocolate y pan', 'DESAYUNO', 1, 'COMPRA', NULL, 8000, 'SEMANAL', 'PERSONAL', 'N', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 3, 1, 16, 2, 'Pollo asado', 'ALMUERZO', 1, 'COMPRA', NULL, 28000, 'QUINCENAL', 'HOGAR', 'S', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 4, 1, 13, 1, 'Salchipapa', 'CENA', 2, 'COMPRA', NULL, 12000, 'ESPORADICA', 'HOGAR', 'N', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 4, 1, 25, 2, 'Jugo natural de mora', 'ONCES', 1, 'COMPRA', NULL, 5000, 'SEMANAL', 'PERSONAL', 'N', 3);
INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by)
  VALUES (seq_comida_fuera_hogar.NEXTVAL, 4, 1, NULL, 3, 'Lechona', 'ALMUERZO', 1, 'REGALO', NULL, 15000, NULL, 'HOGAR', 'N', 3);

-- Historial de verificación (auditoria_encuesta): vacío, ningún caso se ha
-- aprobado ni rechazado todavía.


COMMIT;
