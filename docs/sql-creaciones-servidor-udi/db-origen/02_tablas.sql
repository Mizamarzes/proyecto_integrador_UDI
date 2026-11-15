-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Tablas, llaves foráneas, índices y secuencias (Oracle 10g)
--
-- Lo ejecuta instalar.sql. Es igual en db-origen y db-destino: si cambia
-- una tabla, se cambia en las dos carpetas (y en las vistas y el
-- procedimiento de sincronización, que listan las columnas).
-- =====================================================================


SET DEFINE OFF

-- ---------------------------------------------------------------------
-- Seguridad
-- ---------------------------------------------------------------------

CREATE TABLE modulo (
  id          NUMBER(10)    NOT NULL,
  nombre      VARCHAR2(60)  NOT NULL,
  slug        VARCHAR2(60)  NOT NULL,
  created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at  TIMESTAMP,
  created_by  NUMBER(10),
  CONSTRAINT pk_modulo PRIMARY KEY (id),
  CONSTRAINT uq_modulo_slug UNIQUE (slug)
);

CREATE TABLE rol (
  id          NUMBER(10)    NOT NULL,
  nombre      VARCHAR2(30)  NOT NULL,
  created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at  TIMESTAMP,
  created_by  NUMBER(10),
  CONSTRAINT pk_rol PRIMARY KEY (id),
  CONSTRAINT uq_rol_nombre UNIQUE (nombre)
);

CREATE TABLE rol_modulo (
  id          NUMBER(10)    NOT NULL,
  rol_id      NUMBER(10)    NOT NULL,
  modulo_id   NUMBER(10)    NOT NULL,
  created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at  TIMESTAMP,
  created_by  NUMBER(10),
  CONSTRAINT pk_rol_modulo PRIMARY KEY (id),
  CONSTRAINT uq_rol_modulo UNIQUE (rol_id, modulo_id)
);

CREATE TABLE usuario (
  id             NUMBER(10)     NOT NULL,
  email          VARCHAR2(120)  NOT NULL,
  password       VARCHAR2(255)  NOT NULL,
  nombre         VARCHAR2(80)   NOT NULL,
  apellido       VARCHAR2(80)   NOT NULL,
  telefono       VARCHAR2(20),
  rol_id         NUMBER(10)     NOT NULL,
  activo         CHAR(1)        DEFAULT 'S' NOT NULL,
  ultimo_acceso  TIMESTAMP,
  created_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at     TIMESTAMP,
  created_by     NUMBER(10),
  CONSTRAINT pk_usuario PRIMARY KEY (id),
  CONSTRAINT uq_usuario_email UNIQUE (email),
  CONSTRAINT ck_usuario_activo CHECK (activo IN ('S', 'N'))
);

CREATE TABLE encuestador (
  id          NUMBER(10)  NOT NULL,
  usuario_id  NUMBER(10)  NOT NULL,
  created_at  TIMESTAMP   DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at  TIMESTAMP,
  created_by  NUMBER(10),
  CONSTRAINT pk_encuestador PRIMARY KEY (id),
  CONSTRAINT uq_encuestador_usuario UNIQUE (usuario_id)
);

-- ---------------------------------------------------------------------
-- Ubicación
-- ---------------------------------------------------------------------

CREATE TABLE region (
  id          NUMBER(10)    NOT NULL,
  nombre      VARCHAR2(60)  NOT NULL,
  created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_region PRIMARY KEY (id),
  CONSTRAINT uq_region_nombre UNIQUE (nombre)
);

CREATE TABLE departamento (
  id          NUMBER(10)    NOT NULL,
  nombre      VARCHAR2(60)  NOT NULL,
  region_id   NUMBER(10)    NOT NULL,
  created_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_departamento PRIMARY KEY (id),
  CONSTRAINT uq_departamento_nombre UNIQUE (nombre)
);

CREATE TABLE municipio (
  id               NUMBER(10)    NOT NULL,
  nombre           VARCHAR2(80)  NOT NULL,
  departamento_id  NUMBER(10)    NOT NULL,
  created_at       TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_municipio PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------
-- Capítulo A (identificación), B (visitas) y D2 (cierre)
-- ---------------------------------------------------------------------

CREATE TABLE vivienda (
  id                  NUMBER(10)     NOT NULL,
  municipio_id        NUMBER(10)     NOT NULL,
  clase               NUMBER(1)      NOT NULL,
  sector              VARCHAR2(10),
  seccion             VARCHAR2(10),
  manzana             VARCHAR2(10),
  segmento            VARCHAR2(10),
  edificacion_numero  NUMBER(4),
  vivienda_numero     NUMBER(4),
  barrio              VARCHAR2(120),
  direccion           VARCHAR2(200)  NOT NULL,
  telefono            VARCHAR2(20),
  created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at          TIMESTAMP,
  created_by          NUMBER(10),
  CONSTRAINT pk_vivienda PRIMARY KEY (id),
  CONSTRAINT ck_vivienda_clase CHECK (clase IN (1, 2, 3))
);

CREATE TABLE hogar (
  id              NUMBER(10)    NOT NULL,
  vivienda_id     NUMBER(10)    NOT NULL,
  hogar_numero    NUMBER(2)     NOT NULL,
  total_personas  NUMBER(2)     NOT NULL,
  ciclo           VARCHAR2(10),
  created_at      TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at      TIMESTAMP,
  created_by      NUMBER(10),
  CONSTRAINT pk_hogar PRIMARY KEY (id),
  CONSTRAINT uq_hogar_vivienda_numero UNIQUE (vivienda_id, hogar_numero),
  CONSTRAINT ck_hogar_numero CHECK (hogar_numero >= 1),
  CONSTRAINT ck_hogar_total_personas CHECK (total_personas >= 1)
);

CREATE TABLE encuesta (
  id                     NUMBER(10)      NOT NULL,
  hogar_id               NUMBER(10)      NOT NULL,
  encuestador_id         NUMBER(10)      NOT NULL,
  informante_nombre      VARCHAR2(120),
  informante_orden       NUMBER(2),
  resultado_cuadernillo  VARCHAR2(2),
  fecha_desde            DATE            NOT NULL,
  fecha_hasta            DATE            NOT NULL,
  gasto_fiestas          CHAR(1),
  forma_pago             VARCHAR2(20),
  forma_pago_otra        VARCHAR2(100),
  observaciones          VARCHAR2(1000),
  estado                 VARCHAR2(20)    DEFAULT 'EN_CURSO' NOT NULL,
  fecha_cierre           TIMESTAMP,
  cerrada_por            NUMBER(10),
  created_at             TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at             TIMESTAMP,
  created_by             NUMBER(10),
  CONSTRAINT pk_encuesta PRIMARY KEY (id),
  CONSTRAINT uq_encuesta_hogar UNIQUE (hogar_id),
  CONSTRAINT ck_encuesta_resultado CHECK (resultado_cuadernillo IN ('EC', 'EI', 'NH', 'AT', 'R', 'OC', 'V', 'O')),
  CONSTRAINT ck_encuesta_periodo CHECK (fecha_hasta = fecha_desde + 13),
  CONSTRAINT ck_encuesta_gasto_fiestas CHECK (gasto_fiestas IN ('S', 'N')),
  CONSTRAINT ck_encuesta_forma_pago CHECK (forma_pago IN ('TARJETA_DEBITO', 'TARJETA_CREDITO', 'EFECTIVO', 'FIADO', 'BONOS', 'OTRA')),
  CONSTRAINT ck_encuesta_forma_pago_otra CHECK (
    (forma_pago = 'OTRA' AND forma_pago_otra IS NOT NULL)
    OR (NVL(forma_pago, '-') <> 'OTRA' AND forma_pago_otra IS NULL)),
  CONSTRAINT ck_encuesta_estado CHECK (estado IN ('EN_CURSO', 'PENDIENTE_APROBACION', 'APROBADA', 'RECHAZADA')),
  CONSTRAINT ck_encuesta_cierre CHECK (
    estado NOT IN ('PENDIENTE_APROBACION', 'APROBADA') OR (fecha_cierre IS NOT NULL AND cerrada_por IS NOT NULL))
);

CREATE TABLE visita (
  id                 NUMBER(10)     NOT NULL,
  hogar_id           NUMBER(10)     NOT NULL,
  numero_visita      NUMBER(2)      NOT NULL,
  fecha              DATE           NOT NULL,
  hora_inicio        TIMESTAMP      NOT NULL,
  hora_fin           TIMESTAMP,
  resultado_visita   VARCHAR2(2)    NOT NULL,
  partes_pendientes  VARCHAR2(200),
  fecha_proxima      DATE,
  hora_proxima       TIMESTAMP,
  created_at         TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at         TIMESTAMP,
  created_by         NUMBER(10),
  CONSTRAINT pk_visita PRIMARY KEY (id),
  CONSTRAINT uq_visita_hogar_numero UNIQUE (hogar_id, numero_visita),
  CONSTRAINT ck_visita_numero CHECK (numero_visita BETWEEN 1 AND 14),
  CONSTRAINT ck_visita_resultado CHECK (resultado_visita IN ('EC', 'EI', 'NH', 'AT', 'R', 'OC', 'V', 'O')),
  CONSTRAINT ck_visita_horas CHECK (hora_fin IS NULL OR hora_fin >= hora_inicio)
);

-- ---------------------------------------------------------------------
-- Capítulo C. Características del gasto en alimentos
-- ---------------------------------------------------------------------

CREATE TABLE grupo_alimento (
  id          NUMBER(10)     NOT NULL,
  nombre      VARCHAR2(200)  NOT NULL,
  created_at  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at  TIMESTAMP,
  created_by  NUMBER(10),
  CONSTRAINT pk_grupo_alimento PRIMARY KEY (id),
  CONSTRAINT uq_grupo_alimento_nombre UNIQUE (nombre)
);

CREATE TABLE caracteristica_gasto (
  id                   NUMBER(10)    NOT NULL,
  encuesta_id          NUMBER(10)    NOT NULL,
  grupo_alimento_id    NUMBER(10)    NOT NULL,
  encuestador_id       NUMBER(10)    NOT NULL,
  frecuencia_compra    VARCHAR2(20)  NOT NULL,
  valor_ultima_compra  NUMBER(12,2),
  comprara_en_periodo  CHAR(1),
  created_at           TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at           TIMESTAMP,
  created_by           NUMBER(10),
  CONSTRAINT pk_caracteristica_gasto PRIMARY KEY (id),
  CONSTRAINT uq_carac_gasto_encuesta_grupo UNIQUE (encuesta_id, grupo_alimento_id),
  CONSTRAINT ck_carac_gasto_frecuencia CHECK (frecuencia_compra IN (
    'NUNCA', 'DIARIO', 'VARIAS_VECES_SEMANA', 'SEMANAL', 'QUINCENAL',
    'MENSUAL', 'BIMESTRAL', 'TRIMESTRAL', 'ESPORADICA')),
  CONSTRAINT ck_carac_gasto_valor CHECK (valor_ultima_compra IS NULL OR valor_ultima_compra >= 0),
  CONSTRAINT ck_carac_gasto_comprara CHECK (comprara_en_periodo IN ('S', 'N')),
  CONSTRAINT ck_carac_gasto_bisemana CHECK (
    frecuencia_compra IN ('MENSUAL', 'BIMESTRAL', 'TRIMESTRAL', 'ESPORADICA')
    OR (valor_ultima_compra IS NULL AND comprara_en_periodo IS NULL))
);

-- ---------------------------------------------------------------------
-- Capítulo D y D1. Gastos diarios (14 días)
-- ---------------------------------------------------------------------

CREATE TABLE lugar_compra (
  id          NUMBER(10)     NOT NULL,
  nombre      VARCHAR2(120)  NOT NULL,
  created_at  TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at  TIMESTAMP,
  created_by  NUMBER(10),
  CONSTRAINT pk_lugar_compra PRIMARY KEY (id),
  CONSTRAINT uq_lugar_compra_nombre UNIQUE (nombre)
);

CREATE TABLE registro_diario (
  id             NUMBER(10)      NOT NULL,
  encuesta_id    NUMBER(10)      NOT NULL,
  numero_dia     NUMBER(2)       NOT NULL,
  fecha          DATE            NOT NULL,
  observaciones  VARCHAR2(1000),
  created_at     TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at     TIMESTAMP,
  created_by     NUMBER(10),
  CONSTRAINT pk_registro_diario PRIMARY KEY (id),
  CONSTRAINT uq_reg_diario_encuesta_dia UNIQUE (encuesta_id, numero_dia),
  CONSTRAINT uq_reg_diario_encuesta_fecha UNIQUE (encuesta_id, fecha),
  CONSTRAINT ck_reg_diario_numero_dia CHECK (numero_dia BETWEEN 1 AND 14)
);

CREATE TABLE gasto_diario (
  id                      NUMBER(10)     NOT NULL,
  registro_diario_id      NUMBER(10)     NOT NULL,
  encuestador_id          NUMBER(10)     NOT NULL,
  lugar_compra_id         NUMBER(10),
  numero_orden            NUMBER(3)      NOT NULL,
  nombre_articulo         VARCHAR2(200)  NOT NULL,
  cantidad                NUMBER(10,2)   NOT NULL,
  unidad_medida           VARCHAR2(20)   NOT NULL,
  unidad_medida_otra      VARCHAR2(60),
  equivalencia_gr         NUMBER(10,2),
  equivalencia_cm3        NUMBER(10,2),
  forma_adquisicion       VARCHAR2(20)   NOT NULL,
  forma_adquisicion_otra  VARCHAR2(100),
  valor                   NUMBER(12,2)   NOT NULL,
  frecuencia_compra       VARCHAR2(20),
  tipo_gasto              VARCHAR2(10)   NOT NULL,
  beneficiario_mesada     VARCHAR2(120),
  created_at              TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at              TIMESTAMP,
  created_by              NUMBER(10),
  CONSTRAINT pk_gasto_diario PRIMARY KEY (id),
  CONSTRAINT uq_gasto_diario_orden UNIQUE (registro_diario_id, numero_orden),
  CONSTRAINT ck_gasto_diario_numero_orden CHECK (numero_orden >= 1),
  CONSTRAINT ck_gasto_diario_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_gasto_diario_valor CHECK (valor >= 0),
  CONSTRAINT ck_gasto_diario_unidad CHECK (unidad_medida IN (
    'CENTIMETROS_CUBICOS', 'LITRO', 'GRAMOS', 'ONZAS', 'LIBRA', 'KILO', 'ARROBA', 'OTRA')),
  CONSTRAINT ck_gasto_diario_unidad_otra CHECK (
    (unidad_medida = 'OTRA' AND unidad_medida_otra IS NOT NULL)
    OR (unidad_medida <> 'OTRA' AND unidad_medida_otra IS NULL)),
  CONSTRAINT ck_gasto_diario_equivalencia CHECK (
    (unidad_medida = 'OTRA' OR (equivalencia_gr IS NULL AND equivalencia_cm3 IS NULL))
    AND (equivalencia_gr IS NULL OR equivalencia_cm3 IS NULL)),
  CONSTRAINT ck_gasto_diario_forma CHECK (forma_adquisicion IN (
    'COMPRA', 'PAGO_TRABAJO', 'REGALO', 'INTERCAMBIO', 'PRODUCCION_PROPIA', 'NEGOCIO_PROPIO', 'OTRA')),
  CONSTRAINT ck_gasto_diario_forma_otra CHECK (
    (forma_adquisicion = 'OTRA' AND forma_adquisicion_otra IS NOT NULL)
    OR (forma_adquisicion <> 'OTRA' AND forma_adquisicion_otra IS NULL)),
  CONSTRAINT ck_gasto_diario_frecuencia CHECK (frecuencia_compra IN (
    'DIARIO', 'VARIAS_VECES_SEMANA', 'SEMANAL', 'QUINCENAL',
    'MENSUAL', 'BIMESTRAL', 'TRIMESTRAL', 'ESPORADICA')),
  CONSTRAINT ck_gasto_diario_no_compra CHECK (
    forma_adquisicion = 'COMPRA' OR (lugar_compra_id IS NULL AND frecuencia_compra IS NULL)),
  CONSTRAINT ck_gasto_diario_tipo CHECK (tipo_gasto IN ('HOGAR', 'PERSONAL'))
);

CREATE TABLE comida_fuera_hogar (
  id                      NUMBER(10)     NOT NULL,
  registro_diario_id      NUMBER(10)     NOT NULL,
  encuestador_id          NUMBER(10)     NOT NULL,
  lugar_compra_id         NUMBER(10),
  numero_orden            NUMBER(3)      NOT NULL,
  descripcion             VARCHAR2(200)  NOT NULL,
  tipo_comida             VARCHAR2(10)   NOT NULL,
  cantidad                NUMBER(10,2)   NOT NULL,
  forma_adquisicion       VARCHAR2(20)   NOT NULL,
  forma_adquisicion_otra  VARCHAR2(100),
  valor                   NUMBER(12,2)   NOT NULL,
  frecuencia_compra       VARCHAR2(20),
  tipo_gasto              VARCHAR2(10)   NOT NULL,
  a_domicilio             CHAR(1)        DEFAULT 'N' NOT NULL,
  created_at              TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at              TIMESTAMP,
  created_by              NUMBER(10),
  CONSTRAINT pk_comida_fuera_hogar PRIMARY KEY (id),
  CONSTRAINT uq_cfh_orden UNIQUE (registro_diario_id, numero_orden),
  CONSTRAINT ck_cfh_numero_orden CHECK (numero_orden >= 1),
  CONSTRAINT ck_cfh_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_cfh_valor CHECK (valor >= 0),
  CONSTRAINT ck_cfh_tipo_comida CHECK (tipo_comida IN ('DESAYUNO', 'ALMUERZO', 'CENA', 'ONCES')),
  CONSTRAINT ck_cfh_forma CHECK (forma_adquisicion IN (
    'COMPRA', 'PAGO_TRABAJO', 'REGALO', 'INTERCAMBIO', 'PRODUCCION_PROPIA', 'NEGOCIO_PROPIO', 'OTRA')),
  CONSTRAINT ck_cfh_forma_otra CHECK (
    (forma_adquisicion = 'OTRA' AND forma_adquisicion_otra IS NOT NULL)
    OR (forma_adquisicion <> 'OTRA' AND forma_adquisicion_otra IS NULL)),
  CONSTRAINT ck_cfh_frecuencia CHECK (frecuencia_compra IN (
    'DIARIO', 'VARIAS_VECES_SEMANA', 'SEMANAL', 'QUINCENAL',
    'MENSUAL', 'BIMESTRAL', 'TRIMESTRAL', 'ESPORADICA')),
  CONSTRAINT ck_cfh_no_compra CHECK (
    forma_adquisicion = 'COMPRA' OR (lugar_compra_id IS NULL AND frecuencia_compra IS NULL)),
  CONSTRAINT ck_cfh_tipo_gasto CHECK (tipo_gasto IN ('HOGAR', 'PERSONAL')),
  CONSTRAINT ck_cfh_a_domicilio CHECK (a_domicilio IN ('S', 'N'))
);

-- ---------------------------------------------------------------------
-- Verificación (RF-10): historial de cada caso
-- ---------------------------------------------------------------------

CREATE TABLE auditoria_encuesta (
  id           NUMBER(10)      NOT NULL,
  encuesta_id  NUMBER(10)      NOT NULL,
  accion       VARCHAR2(20)    NOT NULL,
  observacion  VARCHAR2(1000),
  created_at   TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at   TIMESTAMP,
  created_by   NUMBER(10),
  CONSTRAINT pk_auditoria_encuesta PRIMARY KEY (id),
  CONSTRAINT ck_auditoria_accion CHECK (accion IN ('APROBADA', 'RECHAZADA')),
  CONSTRAINT ck_auditoria_rechazo CHECK (accion <> 'RECHAZADA' OR observacion IS NOT NULL)
);

-- =====================================================================
-- 2. LLAVES FORÁNEAS
-- =====================================================================

-- Seguridad
ALTER TABLE rol_modulo  ADD CONSTRAINT fk_rol_modulo_rol    FOREIGN KEY (rol_id)     REFERENCES rol (id) ON DELETE CASCADE;
ALTER TABLE rol_modulo  ADD CONSTRAINT fk_rol_modulo_modulo FOREIGN KEY (modulo_id)  REFERENCES modulo (id) ON DELETE CASCADE;
ALTER TABLE usuario     ADD CONSTRAINT fk_usuario_rol       FOREIGN KEY (rol_id)     REFERENCES rol (id);
ALTER TABLE encuestador ADD CONSTRAINT fk_encuestador_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id);

-- Ubicación
ALTER TABLE departamento ADD CONSTRAINT fk_departamento_region   FOREIGN KEY (region_id)       REFERENCES region (id);
ALTER TABLE municipio    ADD CONSTRAINT fk_municipio_departamento FOREIGN KEY (departamento_id) REFERENCES departamento (id);
ALTER TABLE vivienda     ADD CONSTRAINT fk_vivienda_municipio    FOREIGN KEY (municipio_id)    REFERENCES municipio (id);

-- Vivienda, hogar y encuesta
ALTER TABLE hogar    ADD CONSTRAINT fk_hogar_vivienda        FOREIGN KEY (vivienda_id)    REFERENCES vivienda (id);
ALTER TABLE encuesta ADD CONSTRAINT fk_encuesta_hogar        FOREIGN KEY (hogar_id)       REFERENCES hogar (id);
ALTER TABLE encuesta ADD CONSTRAINT fk_encuesta_encuestador  FOREIGN KEY (encuestador_id) REFERENCES encuestador (id);
ALTER TABLE encuesta ADD CONSTRAINT fk_encuesta_cerrada_por  FOREIGN KEY (cerrada_por)    REFERENCES usuario (id);
ALTER TABLE visita   ADD CONSTRAINT fk_visita_hogar          FOREIGN KEY (hogar_id)       REFERENCES hogar (id) ON DELETE CASCADE;

-- Capítulo C
ALTER TABLE caracteristica_gasto ADD CONSTRAINT fk_carac_gasto_encuesta    FOREIGN KEY (encuesta_id)       REFERENCES encuesta (id) ON DELETE CASCADE;
ALTER TABLE caracteristica_gasto ADD CONSTRAINT fk_carac_gasto_grupo       FOREIGN KEY (grupo_alimento_id) REFERENCES grupo_alimento (id);
ALTER TABLE caracteristica_gasto ADD CONSTRAINT fk_carac_gasto_encuestador FOREIGN KEY (encuestador_id)    REFERENCES encuestador (id);

-- Capítulo D y D1
ALTER TABLE registro_diario    ADD CONSTRAINT fk_reg_diario_encuesta   FOREIGN KEY (encuesta_id)        REFERENCES encuesta (id) ON DELETE CASCADE;
ALTER TABLE gasto_diario       ADD CONSTRAINT fk_gasto_diario_registro FOREIGN KEY (registro_diario_id) REFERENCES registro_diario (id) ON DELETE CASCADE;
ALTER TABLE gasto_diario       ADD CONSTRAINT fk_gasto_diario_encuestador FOREIGN KEY (encuestador_id)  REFERENCES encuestador (id);
ALTER TABLE gasto_diario       ADD CONSTRAINT fk_gasto_diario_lugar    FOREIGN KEY (lugar_compra_id)    REFERENCES lugar_compra (id);
ALTER TABLE comida_fuera_hogar ADD CONSTRAINT fk_cfh_registro          FOREIGN KEY (registro_diario_id) REFERENCES registro_diario (id) ON DELETE CASCADE;
ALTER TABLE comida_fuera_hogar ADD CONSTRAINT fk_cfh_encuestador       FOREIGN KEY (encuestador_id)     REFERENCES encuestador (id);
ALTER TABLE comida_fuera_hogar ADD CONSTRAINT fk_cfh_lugar             FOREIGN KEY (lugar_compra_id)    REFERENCES lugar_compra (id);

-- Verificación
ALTER TABLE auditoria_encuesta ADD CONSTRAINT fk_auditoria_encuesta FOREIGN KEY (encuesta_id) REFERENCES encuesta (id) ON DELETE CASCADE;

-- Auditoría (created_by -> usuario)
ALTER TABLE modulo               ADD CONSTRAINT fk_modulo_created_by       FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE rol                  ADD CONSTRAINT fk_rol_created_by          FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE rol_modulo           ADD CONSTRAINT fk_rol_modulo_created_by   FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE usuario              ADD CONSTRAINT fk_usuario_created_by      FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE encuestador          ADD CONSTRAINT fk_encuestador_created_by  FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE vivienda             ADD CONSTRAINT fk_vivienda_created_by     FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE hogar                ADD CONSTRAINT fk_hogar_created_by        FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE encuesta             ADD CONSTRAINT fk_encuesta_created_by     FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE visita               ADD CONSTRAINT fk_visita_created_by       FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE grupo_alimento       ADD CONSTRAINT fk_grupo_alim_created_by   FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE caracteristica_gasto ADD CONSTRAINT fk_carac_gasto_created_by  FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE lugar_compra         ADD CONSTRAINT fk_lugar_compra_created_by FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE registro_diario      ADD CONSTRAINT fk_reg_diario_created_by   FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE gasto_diario         ADD CONSTRAINT fk_gasto_diario_created_by FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE comida_fuera_hogar   ADD CONSTRAINT fk_cfh_created_by          FOREIGN KEY (created_by) REFERENCES usuario (id);
ALTER TABLE auditoria_encuesta   ADD CONSTRAINT fk_auditoria_created_by    FOREIGN KEY (created_by) REFERENCES usuario (id);

-- =====================================================================
-- 3. ÍNDICES
-- =====================================================================

CREATE INDEX ix_rol_modulo_modulo        ON rol_modulo (modulo_id);
CREATE INDEX ix_usuario_rol              ON usuario (rol_id);
CREATE INDEX ix_departamento_region      ON departamento (region_id);
CREATE INDEX ix_municipio_departamento   ON municipio (departamento_id);
CREATE INDEX ix_vivienda_municipio       ON vivienda (municipio_id);
CREATE INDEX ix_encuesta_encuestador     ON encuesta (encuestador_id);
CREATE INDEX ix_carac_gasto_grupo        ON caracteristica_gasto (grupo_alimento_id);
CREATE INDEX ix_gasto_diario_lugar       ON gasto_diario (lugar_compra_id);
CREATE INDEX ix_cfh_lugar                ON comida_fuera_hogar (lugar_compra_id);
CREATE INDEX ix_auditoria_encuesta       ON auditoria_encuesta (encuesta_id);

-- =====================================================================
-- 4. SECUENCIAS
-- =====================================================================

CREATE SEQUENCE seq_modulo               START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_rol                  START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_rol_modulo           START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_usuario              START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_encuestador          START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_region               START WITH 7  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_departamento         START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_municipio            START WITH 1103 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_vivienda             START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_hogar                START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_encuesta             START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_visita               START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_grupo_alimento       START WITH 26 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_caracteristica_gasto START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_lugar_compra         START WITH 26 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_registro_diario      START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_gasto_diario         START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_comida_fuera_hogar   START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_auditoria_encuesta   START WITH 1  INCREMENT BY 1 NOCACHE;
