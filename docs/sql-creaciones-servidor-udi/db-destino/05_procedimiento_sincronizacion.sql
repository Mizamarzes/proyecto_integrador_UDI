-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Sincronización: procedimiento del esquema DESTINO
--
-- Lo ejecuta instalar.sql al final, como dueño del DESTINO. Necesita el
-- SELECT sobre las vistas del origen (db-origen/06_vistas_sincronizacion.sql),
-- así que el origen se instala primero. Parámetro: el 1er argumento =
-- origen. Para un destino ya instalado se puede ejecutar solo, desde esta
-- carpeta:
--   sqlplus <destino>/<clave> @05_procedimiento_sincronizacion.sql <ORIGEN>
--
-- pr_sincronizar_aprobadas copia al destino las encuestas APROBADAS del
-- origen (paso 4 de la guía) y devuelve cuántas fueron nuevas y cuántas se
-- reemplazaron. Cada encuesta de <origen>.vis_sinc_encuesta:
--   - no existe en el destino                      -> se inserta (nueva)
--   - existe, pero en el origen cambió después     -> se borra y se vuelve a
--     (se reabrió y se aprobó de nuevo: updated_at    insertar (actualizada)
--     más reciente) o en el destino no está aprobada
--   - existe y no cambió                           -> no se toca
-- Así ejecutarlo dos veces no duplica nada. Se conservan los ids del origen
-- (el destino es una copia que solo alimenta este origen).
--
-- Orden y por qué:
--   1. Roles, usuarios y encuestadores que le falten al destino (created_by,
--      cerrada_por y encuestador_id son llaves foráneas). Los existentes no
--      se modifican. Los catálogos (región, departamento, municipio, grupos
--      de alimentos, lugares de compra) no se copian: son los mismos.
--   2. Por encuesta: se borran del destino su versión anterior (en cascada
--      se van el capítulo C, los días, los gastos D y D1 y su historial) y
--      las visitas del hogar (cuelgan del hogar, no de la encuesta). Los
--      disparadores trg_val_* no se disparan al borrar.
--   3. MERGE de la vivienda y el hogar (inserta o actualiza).
--   4. La encuesta se inserta EN_CURSO y luego sus visitas, capítulo C,
--      días, gastos y comidas: los disparadores trg_val_* del destino no
--      dejan registrar nada en una encuesta cerrada (-20002).
--   5. Se pasa a PENDIENTE_APROBACION y luego a APROBADA (los cambios de
--      estado que permite trg_val_encuesta), dejando el updated_at del
--      origen para compararlo en la próxima sincronización.
--   6. Se copia su historial de revisiones (auditoria_encuesta).
--
-- No hace COMMIT: la aplicación lo llama dentro de una transacción
-- (ConexionBD.enTransaccion), así que si algo falla no queda nada a medias.
-- Para probarlo a mano: db-origen/prueba_sincronizacion.sql.
--
-- Al final concede EXECUTE al origen y al rol de su aplicación
-- (ROL_<origen>_APLICACION de seguridad/usuarios_roles.sql, si existe). Si
-- se vuelve a ejecutar usuarios_roles.sql el rol se recrea y pierde ese
-- permiso: hay que volver a ejecutar este script.
-- =====================================================================

SET DEFINE ON
SET VERIFY OFF
-- <origen>.vista: el primer punto cierra la variable de SQL*Plus
DEFINE origen = &1

CREATE OR REPLACE PROCEDURE pr_sincronizar_aprobadas
(v_nuevas OUT NUMBER,
v_actualizadas OUT NUMBER)
IS
CURSOR c_encuestas IS
   SELECT * FROM &origen..vis_sinc_encuesta
   ORDER BY id;
v_existe BOOLEAN;
v_estado encuesta.estado%TYPE;
v_actualizadaEn encuesta.updated_at%TYPE;
v_nunca CONSTANT TIMESTAMP:=TIMESTAMP '1900-01-01 00:00:00';
BEGIN
v_nuevas:=0;
v_actualizadas:=0;

-- 1. Roles, usuarios y encuestadores que falten. Si el rol lo creó un
--    usuario que el destino aún no tiene, queda sin created_by (los
--    usuarios necesitan el rol primero).
INSERT INTO rol (id, nombre, created_at, updated_at, created_by)
SELECT o.id, o.nombre, o.created_at, o.updated_at,
       (SELECT u.id FROM usuario u WHERE u.id=o.created_by)
FROM &origen..vis_sinc_rol o
WHERE NOT EXISTS (SELECT 1 FROM rol d WHERE d.id=o.id);

INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo,
                     ultimo_acceso, created_at, updated_at, created_by)
SELECT o.id, o.email, o.password, o.nombre, o.apellido, o.telefono, o.rol_id, o.activo,
       o.ultimo_acceso, o.created_at, o.updated_at, o.created_by
FROM &origen..vis_sinc_usuario o
WHERE NOT EXISTS (SELECT 1 FROM usuario d WHERE d.id=o.id);

INSERT INTO encuestador (id, usuario_id, created_at, updated_at, created_by)
SELECT o.id, o.usuario_id, o.created_at, o.updated_at, o.created_by
FROM &origen..vis_sinc_encuestador o
WHERE NOT EXISTS (SELECT 1 FROM encuestador d WHERE d.id=o.id);

FOR r_encuesta IN c_encuestas LOOP
   BEGIN
      SELECT estado, updated_at INTO v_estado, v_actualizadaEn
      FROM encuesta
      WHERE id=r_encuesta.id;
      v_existe:=TRUE;
   EXCEPTION
   WHEN NO_DATA_FOUND THEN
      v_existe:=FALSE;
   END;

   -- Oracle 10g no tiene CONTINUE: lo que no cambió se salta con este IF
   IF NOT v_existe OR v_estado<>'APROBADA'
      OR NVL(r_encuesta.updated_at,v_nunca)>NVL(v_actualizadaEn,v_nunca) THEN

      -- 2. Versión anterior (o encuesta de ese hogar) y visitas del hogar
      DELETE FROM visita WHERE hogar_id=r_encuesta.hogar_id;
      DELETE FROM encuesta WHERE id=r_encuesta.id OR hogar_id=r_encuesta.hogar_id;

      -- 3. Vivienda y hogar
      MERGE INTO vivienda d
      USING (SELECT * FROM &origen..vis_sinc_vivienda
             WHERE id=(SELECT h.vivienda_id FROM &origen..vis_sinc_hogar h
                       WHERE h.id=r_encuesta.hogar_id)) o
      ON (d.id=o.id)
      WHEN MATCHED THEN UPDATE SET
         d.municipio_id=o.municipio_id, d.clase=o.clase, d.sector=o.sector,
         d.seccion=o.seccion, d.manzana=o.manzana, d.segmento=o.segmento,
         d.edificacion_numero=o.edificacion_numero, d.vivienda_numero=o.vivienda_numero,
         d.barrio=o.barrio, d.direccion=o.direccion, d.telefono=o.telefono,
         d.created_at=o.created_at, d.updated_at=o.updated_at, d.created_by=o.created_by
      WHEN NOT MATCHED THEN INSERT
         (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero,
          vivienda_numero, barrio, direccion, telefono, created_at, updated_at, created_by)
      VALUES
         (o.id, o.municipio_id, o.clase, o.sector, o.seccion, o.manzana, o.segmento,
          o.edificacion_numero, o.vivienda_numero, o.barrio, o.direccion, o.telefono,
          o.created_at, o.updated_at, o.created_by);

      MERGE INTO hogar d
      USING (SELECT * FROM &origen..vis_sinc_hogar WHERE id=r_encuesta.hogar_id) o
      ON (d.id=o.id)
      WHEN MATCHED THEN UPDATE SET
         d.vivienda_id=o.vivienda_id, d.hogar_numero=o.hogar_numero,
         d.total_personas=o.total_personas, d.ciclo=o.ciclo,
         d.created_at=o.created_at, d.updated_at=o.updated_at, d.created_by=o.created_by
      WHEN NOT MATCHED THEN INSERT
         (id, vivienda_id, hogar_numero, total_personas, ciclo, created_at, updated_at, created_by)
      VALUES
         (o.id, o.vivienda_id, o.hogar_numero, o.total_personas, o.ciclo,
          o.created_at, o.updated_at, o.created_by);

      -- 4. Encuesta abierta y sus capítulos
      INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden,
                            resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas,
                            forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre,
                            cerrada_por, created_at, updated_at, created_by)
      VALUES (r_encuesta.id, r_encuesta.hogar_id, r_encuesta.encuestador_id,
              r_encuesta.informante_nombre, r_encuesta.informante_orden,
              r_encuesta.resultado_cuadernillo, r_encuesta.fecha_desde, r_encuesta.fecha_hasta,
              r_encuesta.gasto_fiestas, r_encuesta.forma_pago, r_encuesta.forma_pago_otra,
              r_encuesta.observaciones, 'EN_CURSO', r_encuesta.fecha_cierre,
              r_encuesta.cerrada_por, r_encuesta.created_at, r_encuesta.updated_at,
              r_encuesta.created_by);

      INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin,
                          resultado_visita, partes_pendientes, fecha_proxima, hora_proxima,
                          created_at, updated_at, created_by)
      SELECT id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin,
             resultado_visita, partes_pendientes, fecha_proxima, hora_proxima,
             created_at, updated_at, created_by
      FROM &origen..vis_sinc_visita
      WHERE hogar_id=r_encuesta.hogar_id;

      INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id,
                                        frecuencia_compra, valor_ultima_compra,
                                        comprara_en_periodo, created_at, updated_at, created_by)
      SELECT id, encuesta_id, grupo_alimento_id, encuestador_id,
             frecuencia_compra, valor_ultima_compra,
             comprara_en_periodo, created_at, updated_at, created_by
      FROM &origen..vis_sinc_caracteristica_gasto
      WHERE encuesta_id=r_encuesta.id;

      INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones,
                                   created_at, updated_at, created_by)
      SELECT id, encuesta_id, numero_dia, fecha, observaciones,
             created_at, updated_at, created_by
      FROM &origen..vis_sinc_registro_diario
      WHERE encuesta_id=r_encuesta.id;

      INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id,
                                numero_orden, nombre_articulo, cantidad, unidad_medida,
                                unidad_medida_otra, equivalencia_gr, equivalencia_cm3,
                                forma_adquisicion, forma_adquisicion_otra, valor,
                                frecuencia_compra, tipo_gasto, beneficiario_mesada,
                                created_at, updated_at, created_by)
      SELECT id, registro_diario_id, encuestador_id, lugar_compra_id,
             numero_orden, nombre_articulo, cantidad, unidad_medida,
             unidad_medida_otra, equivalencia_gr, equivalencia_cm3,
             forma_adquisicion, forma_adquisicion_otra, valor,
             frecuencia_compra, tipo_gasto, beneficiario_mesada,
             created_at, updated_at, created_by
      FROM &origen..vis_sinc_gasto_diario
      WHERE encuesta_id=r_encuesta.id;

      INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, lugar_compra_id,
                                      numero_orden, descripcion, tipo_comida, cantidad,
                                      forma_adquisicion, forma_adquisicion_otra, valor,
                                      frecuencia_compra, tipo_gasto, a_domicilio,
                                      created_at, updated_at, created_by)
      SELECT id, registro_diario_id, encuestador_id, lugar_compra_id,
             numero_orden, descripcion, tipo_comida, cantidad,
             forma_adquisicion, forma_adquisicion_otra, valor,
             frecuencia_compra, tipo_gasto, a_domicilio,
             created_at, updated_at, created_by
      FROM &origen..vis_sinc_comida_fuera_hogar
      WHERE encuesta_id=r_encuesta.id;

      -- 5. Cierre y aprobación, con la fecha de cambio del origen
      UPDATE encuesta
      SET estado='PENDIENTE_APROBACION'
      WHERE id=r_encuesta.id;

      UPDATE encuesta
      SET estado='APROBADA', updated_at=r_encuesta.updated_at
      WHERE id=r_encuesta.id;

      -- 6. Historial de revisiones
      INSERT INTO auditoria_encuesta (id, encuesta_id, accion, observacion,
                                      created_at, updated_at, created_by)
      SELECT id, encuesta_id, accion, observacion, created_at, updated_at, created_by
      FROM &origen..vis_sinc_auditoria_encuesta
      WHERE encuesta_id=r_encuesta.id;

      IF v_existe THEN
         v_actualizadas:=v_actualizadas+1;
      ELSE
         v_nuevas:=v_nuevas+1;
      END IF;
   END IF;
END LOOP;
END;
/
SHOW ERRORS PROCEDURE pr_sincronizar_aprobadas

WHENEVER SQLERROR EXIT FAILURE

-- La aplicación del origen ejecuta el procedimiento con su propio usuario
GRANT EXECUTE ON pr_sincronizar_aprobadas TO &origen;

-- Y con <origen>_APP, si se crearon los usuarios de seguridad/usuarios_roles.sql
BEGIN
EXECUTE IMMEDIATE 'GRANT EXECUTE ON pr_sincronizar_aprobadas TO ROL_'||UPPER('&origen')||'_APLICACION';
EXCEPTION
WHEN OTHERS THEN
   IF SQLCODE=-1917 OR SQLCODE=-1919 THEN
      DBMS_OUTPUT.PUT_LINE('No existe ROL_'||UPPER('&origen')||'_APLICACION: solo se concede a &origen.');
   ELSE
      RAISE;
   END IF;
END;
/

PROMPT Procedimiento pr_sincronizar_aprobadas creado (origen &origen).
