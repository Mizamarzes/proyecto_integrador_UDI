-- =====================================================================
-- ENPH - Cuadernillo 2 Urbano
-- Script DDL para Oracle 10g
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
  total_hogares       NUMBER(2)      NOT NULL,
  barrio              VARCHAR2(120),
  direccion           VARCHAR2(200)  NOT NULL,
  telefono            VARCHAR2(20),
  created_at          TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at          TIMESTAMP,
  created_by          NUMBER(10),
  CONSTRAINT pk_vivienda PRIMARY KEY (id),
  CONSTRAINT ck_vivienda_clase CHECK (clase IN (1, 2, 3)),
  CONSTRAINT ck_vivienda_total_hogares CHECK (total_hogares >= 1)
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
  estado                 VARCHAR2(10)    DEFAULT 'ABIERTA' NOT NULL,
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
  CONSTRAINT ck_encuesta_estado CHECK (estado IN ('ABIERTA', 'CERRADA', 'REABIERTA')),
  CONSTRAINT ck_encuesta_cierre CHECK (
    estado <> 'CERRADA' OR (fecha_cierre IS NOT NULL AND cerrada_por IS NOT NULL))
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

-- =====================================================================
-- 4. SECUENCIAS
-- =====================================================================

CREATE SEQUENCE seq_modulo               START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_rol                  START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_rol_modulo           START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_usuario              START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_encuestador          START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_region               START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_departamento         START WITH 1  INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE seq_municipio            START WITH 1  INCREMENT BY 1 NOCACHE;
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

-- =====================================================================
-- 5. CATÁLOGOS DEL CUADERNILLO
-- =====================================================================

INSERT INTO grupo_alimento (id, nombre) VALUES (1,  'Pan, arepas, bollos y almojábanas');
INSERT INTO grupo_alimento (id, nombre) VALUES (2,  'Galletas de sal y de dulce');
INSERT INTO grupo_alimento (id, nombre) VALUES (3,  'Arroz, pastas alimenticias, avena, maíz, harinas y otros cereales (para el desayuno)');
INSERT INTO grupo_alimento (id, nombre) VALUES (4,  'Carne de res, cerdo, hueso y vísceras');
INSERT INTO grupo_alimento (id, nombre) VALUES (5,  'Pollo, otras aves y menudencias');
INSERT INTO grupo_alimento (id, nombre) VALUES (6,  'Salchichas, jamón, mortadela, salchichón y otras carnes frías preparadas (incluidos los embutidos vegetarianos)');
INSERT INTO grupo_alimento (id, nombre) VALUES (7,  'Pescado de río, mar y otros productos marinos (frescos, congelados y enlatados)');
INSERT INTO grupo_alimento (id, nombre) VALUES (8,  'Leche (animal y vegetal)');
INSERT INTO grupo_alimento (id, nombre) VALUES (9,  'Queso (animal y vegetal)');
INSERT INTO grupo_alimento (id, nombre) VALUES (10, 'Otros derivados de la leche: yogur, kumis, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (11, 'Huevos');
INSERT INTO grupo_alimento (id, nombre) VALUES (12, 'Aceites y grasas: aceite, manteca, mantequilla, margarina, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (13, 'Frutas');
INSERT INTO grupo_alimento (id, nombre) VALUES (14, 'Verduras y hortalizas: tomate, cebolla, arveja, espinaca, apio, zanahoria, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (15, 'Granos secos: fríjol, lenteja, garbanzo, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (16, 'Enlatados y encurtidos: arveja, fríjol, maíz, espárragos, zanahoria, habichuela, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (17, 'Plátano, yuca, arracacha, name, papa y otros tubérculos');
INSERT INTO grupo_alimento (id, nombre) VALUES (18, 'Azúcar, panela y otros endulzantes');
INSERT INTO grupo_alimento (id, nombre) VALUES (19, 'Mermeladas, arequipe, bocadillos, compotas, dulces, helados, chocolatinas, ponquecitos, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (20, 'Salsas, mayonesa, mostaza, vinagre, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (21, 'Sal y condimentos');
INSERT INTO grupo_alimento (id, nombre) VALUES (22, 'Café, chocolate, otras bebidas con cacao (milo, chocolisto), té y hierbas aromáticas');
INSERT INTO grupo_alimento (id, nombre) VALUES (23, 'Papas fritas, chitos, maicitos, patacones, besitos, maní, etc.');
INSERT INTO grupo_alimento (id, nombre) VALUES (24, 'Agua, gaseosas, refrescos, jugos, té frío y otras bebidas no alcohólicas');
INSERT INTO grupo_alimento (id, nombre) VALUES (25, 'Alimentos y bebidas preparados fuera del hogar, para consumir dentro o fuera del hogar');

INSERT INTO lugar_compra (id, nombre) VALUES (1,  'Almacenes, supermercado de cadena, tiendas por departamento o hipermercados');
INSERT INTO lugar_compra (id, nombre) VALUES (4,  'Supermercados de cajas de compensación, cooperativas, fondos de empleados y comisariatos');
INSERT INTO lugar_compra (id, nombre) VALUES (6,  'Supermercado de barrio, tiendas de barrio, cigarrerías, salsamentarias y delicatessen');
INSERT INTO lugar_compra (id, nombre) VALUES (7,  'Misceláneas de barrio y cacharrerías');
INSERT INTO lugar_compra (id, nombre) VALUES (10, 'Plazas de mercado, galerías, mercados móviles, central mayorista de abastecimiento y graneros');
INSERT INTO lugar_compra (id, nombre) VALUES (13, 'Vendedores ambulantes');
INSERT INTO lugar_compra (id, nombre) VALUES (14, 'Sanandresitos, bodegas y fábricas');
INSERT INTO lugar_compra (id, nombre) VALUES (16, 'Establecimiento especializado en la venta del artículo o la prestación del servicio');
INSERT INTO lugar_compra (id, nombre) VALUES (17, 'Farmacias y droguerías');
INSERT INTO lugar_compra (id, nombre) VALUES (18, 'Restaurantes');
INSERT INTO lugar_compra (id, nombre) VALUES (19, 'Cafeterías y establecimientos de comidas rápidas');
INSERT INTO lugar_compra (id, nombre) VALUES (20, 'Persona particular');
INSERT INTO lugar_compra (id, nombre) VALUES (21, 'Ferias especializadas: artesanal, del libro, del hogar, de tecnología, etc.');
INSERT INTO lugar_compra (id, nombre) VALUES (22, 'A través de internet');
INSERT INTO lugar_compra (id, nombre) VALUES (23, 'Televentas y ventas por catálogo');
INSERT INTO lugar_compra (id, nombre) VALUES (24, 'Otro');
INSERT INTO lugar_compra (id, nombre) VALUES (25, 'Plazoleta de comidas');

-- =====================================================================
-- 6. DATOS DE PRUEBA
-- =====================================================================

-- ---------------------------------------------------------------------
-- Roles (10)
-- ---------------------------------------------------------------------
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'ADMIN');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'SUPERVISOR');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'ENCUESTADOR');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'CRITICO_DIGITADOR');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'COORDINADOR_CAMPO');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'DIGITADOR');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'ANALISTA');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'AUDITOR');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'CONSULTA');
INSERT INTO rol (id, nombre) VALUES (seq_rol.NEXTVAL, 'SOPORTE');

-- ---------------------------------------------------------------------
-- Usuarios (12): 1 administrador, 1 supervisor y 10 encuestadores.
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
-- Módulos (10) y permisos por rol (10)
-- ---------------------------------------------------------------------
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Usuarios', 'usuarios', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Roles y permisos', 'roles', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Viviendas', 'viviendas', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Hogares', 'hogares', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Encuestas', 'encuestas', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Control de visitas', 'visitas', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Características del gasto', 'caracteristicas-gasto', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Gastos diarios', 'gastos-diarios', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Comidas fuera del hogar', 'comidas-fuera-hogar', 1);
INSERT INTO modulo (id, nombre, slug, created_by) VALUES (seq_modulo.NEXTVAL, 'Reportes', 'reportes', 1);

-- ADMIN: usuarios y roles | SUPERVISOR: encuestas y reportes | ENCUESTADOR: captura
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 1, 1, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 1, 2, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 2, 5, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 2, 10, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 3, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 4, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 6, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 7, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 8, 1);
INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) VALUES (seq_rol_modulo.NEXTVAL, 3, 9, 1);

-- ---------------------------------------------------------------------
-- Ubicación: regiones (10), departamentos (10) y municipios (10)
-- ---------------------------------------------------------------------
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Caribe');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Oriental');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Central');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Pacífica');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Bogotá');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Antioquia');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Valle del Cauca');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'San Andrés');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Orinoquía-Amazonía');
INSERT INTO region (id, nombre) VALUES (seq_region.NEXTVAL, 'Eje Cafetero');

INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Santander', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Norte de Santander', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Boyacá', 2);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Antioquia', 6);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Atlántico', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Bolívar', 1);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Valle del Cauca', 7);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Cundinamarca', 3);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'Bogotá D.C.', 5);
INSERT INTO departamento (id, nombre, region_id) VALUES (seq_departamento.NEXTVAL, 'San Andrés y Providencia', 8);

INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Bucaramanga', 1);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Floridablanca', 1);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Girón', 1);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Piedecuesta', 1);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Cúcuta', 2);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Tunja', 3);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Medellín', 4);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Barranquilla', 5);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Cartagena', 6);
INSERT INTO municipio (id, nombre, departamento_id) VALUES (seq_municipio.NEXTVAL, 'Cali', 7);

-- ---------------------------------------------------------------------
-- Viviendas (10)
-- ---------------------------------------------------------------------
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 1, 1, '0101', '01', '07', '001', 12, 1, 2, 'Cabecera del Llano', 'Calle 45 # 12-30', '6076345678', 3);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 1, 1, '0102', '03', '15', '002', 4, 1, 1, 'San Alonso', 'Carrera 27 # 18-45', '6076451234', 4);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 2, 1, '0201', '02', '22', '003', 8, 2, 1, 'Canaveral', 'Calle 30 # 25-71', '6076389012', 5);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 3, 1, '0301', '01', '05', '004', 3, 1, 1, 'Centro', 'Carrera 25 # 30-12', '6076467890', 6);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 4, 1, '0401', '04', '11', '005', 6, 1, 1, 'La Candelaria', 'Calle 9 # 6-20', '6076553210', 7);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 5, 1, '0501', '02', '18', '006', 9, 3, 1, 'La Riviera', 'Avenida 0 # 12-34', '6075781234', 8);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 6, 1, '0601', '01', '02', '007', 1, 1, 1, 'Centro', 'Calle 19 # 9-35', '6087423456', 9);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 7, 1, '0701', '05', '33', '008', 15, 2, 1, 'Laureles', 'Circular 4 # 73-12', '6044112233', 10);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 8, 1, '0801', '03', '27', '009', 7, 1, 1, 'El Prado', 'Carrera 54 # 64-120', '6053445566', 11);
INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by)
  VALUES (seq_vivienda.NEXTVAL, 10, 1, '1001', '02', '09', '010', 5, 1, 1, 'San Fernando', 'Calle 5 # 38-25', '6025567788', 12);

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
-- Encuestas (10): una por hogar. Las 3, 4 y 5 están cerradas por el
-- supervisor (usuario 2); la 6 fue reabierta; las demás están abiertas.
-- ---------------------------------------------------------------------
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 1, 1, 'Paola Andrea Gómez', 1, 'EI', DATE '2026-09-28', DATE '2026-10-11', NULL, NULL, NULL, NULL, 'ABIERTA', NULL, NULL, 3);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 2, 1, 'Julián Ortiz', 1, 'EI', DATE '2026-09-28', DATE '2026-10-11', NULL, NULL, NULL, 'Estudiante que arrienda una habitación', 'ABIERTA', NULL, NULL, 3);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 3, 2, 'Marta Lucía Díaz', 2, 'EC', DATE '2026-09-01', DATE '2026-09-14', 'N', 'EFECTIVO', NULL, NULL, 'CERRADA', TIMESTAMP '2026-09-15 10:00:00', 2, 4);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 4, 3, 'Jorge Iván Castro', 1, 'EC', DATE '2026-09-01', DATE '2026-09-14', 'S', 'TARJETA_DEBITO', NULL, 'Cumpleanos el día 9', 'CERRADA', TIMESTAMP '2026-09-15 15:30:00', 2, 5);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 5, 4, 'Luz Dary Mantilla', 1, 'EC', DATE '2026-09-08', DATE '2026-09-21', 'N', 'FIADO', NULL, NULL, 'CERRADA', TIMESTAMP '2026-09-22 09:20:00', 2, 6);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 6, 5, 'Rosa Elena Pabón', 2, 'EI', DATE '2026-09-15', DATE '2026-09-28', 'N', 'OTRA', 'Transferencia bancaria', 'Reabierta para corregir el día 3', 'REABIERTA', TIMESTAMP '2026-09-29 11:00:00', 2, 7);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 7, 6, 'Hernán Villamizar', 1, 'AT', DATE '2026-10-01', DATE '2026-10-14', NULL, NULL, NULL, 'Informante de viaje hasta el 6 de octubre', 'ABIERTA', NULL, NULL, 8);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 8, 7, 'Claudia Suárez', 1, 'EI', DATE '2026-10-01', DATE '2026-10-14', NULL, NULL, NULL, NULL, 'ABIERTA', NULL, NULL, 9);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 9, 8, 'Andrés Restrepo', 1, 'EI', DATE '2026-10-05', DATE '2026-10-18', NULL, NULL, NULL, NULL, 'ABIERTA', NULL, NULL, 10);
INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by)
  VALUES (seq_encuesta.NEXTVAL, 10, 9, 'Yolanda Charris', 2, 'EI', DATE '2026-10-05', DATE '2026-10-18', NULL, NULL, NULL, NULL, 'ABIERTA', NULL, NULL, 11);

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

COMMIT;
