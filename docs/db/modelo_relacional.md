# Modelo relacional – Cuadernillo 2 urbano (ENPH)

Este documento explica cómo se pasó el diagrama entidad-relación (`db_proyecto_integra-entidad-relacion.drawio.png`) al modelo relacional que está en `docs/db/modelo_relacional.dbml`. El modelo está pensado para Oracle y se puede ver pegando el `.dbml` en dbdiagram.io.

Resumen: **18 tablas, 7 enums y 36 llaves foráneas** (21 del modelo y 15 de auditoría).

---

## 1. Reglas de transformación aplicadas

| Elemento del E-R | Cómo se transformó | Ejemplo |
|---|---|---|
| Entidad | Tabla con una llave primaria `id` autoincremental | `HOGAR` → `hogar` |
| Relación 1:N | Llave foránea en la tabla del lado N | `MUNICIPIO` 1:N `HOGAR` → `hogar.municipio_id` |
| Relación N:M | Tabla intermedia con `id` propio y `UNIQUE` sobre las dos llaves foráneas | `ROL` N:M `MODULO` (TIENE PERMISO) → `rol_modulo` |
| Relación 1:1 | Llave foránea con restricción `UNIQUE` | `HOGAR` 1:1 `ENCUESTA` → `encuesta.hogar_id` único |
| Especialización | Tabla hija con llave foránea única hacia la tabla padre | `USUARIO` → `ENCUESTADOR` |
| Atributo con valores fijos del cuadernillo | Enum (en Oracle: `VARCHAR2` + `CHECK`) | `unidad_medida`, `forma_adquisicion`… |
| Atributo que también aparece como entidad | Se reemplaza por una llave foránea | `lugar_compra` (atributo) → `lugar_compra_id` |

---

## 2. Entidades y tablas

| Entidad (E-R) | Tabla | Grupo |
|---|---|---|
| MODULO | `modulo` | Seguridad |
| ROL | `rol` | Seguridad |
| TIENE PERMISO (relación) | `rol_modulo` | Seguridad |
| USUARIO | `usuario` | Seguridad |
| ENCUESTADOR | `encuestador` | Seguridad |
| REGION *(agregada)* | `region` | Ubicación |
| DEPARTAMENTO | `departamento` | Ubicación |
| MUNICIPIO | `municipio` | Ubicación |
| VIVIENDA *(agregada)* | `vivienda` | Capítulo A |
| HOGAR | `hogar` | Capítulo A |
| ENCUESTA | `encuesta` | Capítulo A y D2 |
| VISITA | `visita` | Capítulo B |
| GRUPO_ALIMENTO | `grupo_alimento` | Capítulo C |
| CARACTERISTICA_GASTO | `caracteristica_gasto` | Capítulo C |
| LUGAR_COMPRA | `lugar_compra` | Capítulos D y D1 |
| REGISTRO_DIARIO | `registro_diario` | Capítulos D y D1 |
| GASTO_DIARIO | `gasto_diario` | Capítulo D |
| COMIDA_FUERA_HOGAR | `comida_fuera_hogar` | Capítulo D1 |

---

## 3. Relaciones

| Relación (E-R) | Cardinalidad | Implementación |
|---|---|---|
| ROL – TIENE PERMISO – MODULO | N:M | `rol_modulo (rol_id, modulo_id)` |
| ROL – TIENE – USUARIO | 1:N | `usuario.rol_id` |
| USUARIO – ENCUESTADOR | 1:1 | `encuestador.usuario_id` (único) |
| *(agregada)* REGION – DEPARTAMENTO | 1:N | `departamento.region_id` |
| DEPARTAMENTO – TIENE – MUNICIPIO | 1:N | `municipio.departamento_id` |
| MUNICIPIO – PERTENECE – VIVIENDA | 1:N | `vivienda.municipio_id` (en el E-R la relación es con HOGAR) |
| *(agregada)* VIVIENDA – HOGAR | 1:N | `hogar.vivienda_id` |
| HOGAR – TIENE – ENCUESTA | 1:1 | `encuesta.hogar_id` (único) |
| ENCUESTADOR – DILIGENCIA Y CREA – ENCUESTA | 1:N | `encuesta.encuestador_id` |
| HOGAR – TIENE – VISITA | 1:N | `visita.hogar_id` |
| GRUPO_ALIMENTO – CONTIENE – CARACTERISTICA_GASTO | 1:N | `caracteristica_gasto.grupo_alimento_id` |
| ENCUESTADOR – REGISTRA – CARACTERISTICA_GASTO | 1:N | `caracteristica_gasto.encuestador_id` |
| ENCUESTADOR – REGISTRA – GASTO_DIARIO | 1:N | `gasto_diario.encuestador_id` |
| ENCUESTADOR – REGISTRA – COMIDA_FUERA_HOGAR | 1:N | `comida_fuera_hogar.encuestador_id` |
| REGISTRO_DIARIO – TIENE – GASTO_DIARIO | 1:N | `gasto_diario.registro_diario_id` |
| REGISTRO_DIARIO – TIENE – COMIDA_FUERA_HOGAR | 1:N | `comida_fuera_hogar.registro_diario_id` |
| LUGAR_COMPRA – TIENE – GASTO_DIARIO | 1:N | `gasto_diario.lugar_compra_id` |
| LUGAR_COMPRA – TIENE – COMIDA_FUERA_HOGAR | 1:N | `comida_fuera_hogar.lugar_compra_id` |
| *(agregada)* ENCUESTA – CARACTERISTICA_GASTO | 1:N | `caracteristica_gasto.encuesta_id` |
| *(agregada)* ENCUESTA – REGISTRO_DIARIO | 1:N | `registro_diario.encuesta_id` |

Las dos últimas no están en el E-R. Se explican en la sección 6.

---

## 4. Enums

En Oracle no existe el tipo ENUM. Cada uno se crea como una columna `VARCHAR2` con una restricción `CHECK (columna IN (...))`.

| Enum | Valores | Usado en |
|---|---|---|
| `resultado_entrevista` | EC, EI, NH, AT, R, OC, V, O | `encuesta.resultado_cuadernillo`, `visita.resultado_visita` |
| `forma_pago` | TARJETA_DEBITO, TARJETA_CREDITO, EFECTIVO, FIADO, BONOS, OTRA | `encuesta.forma_pago` |
| `frecuencia_compra` | NUNCA, DIARIO, VARIAS_VECES_SEMANA, SEMANAL, QUINCENAL, MENSUAL, BIMESTRAL, TRIMESTRAL, ESPORADICA | `caracteristica_gasto`, `gasto_diario`, `comida_fuera_hogar` |
| `unidad_medida` | CENTIMETROS_CUBICOS, LITRO, GRAMOS, ONZAS, LIBRA, KILO, ARROBA, OTRA | `gasto_diario` |
| `forma_adquisicion` | COMPRA, PAGO_TRABAJO, REGALO, INTERCAMBIO, PRODUCCION_PROPIA, NEGOCIO_PROPIO, OTRA | `gasto_diario`, `comida_fuera_hogar` |
| `tipo_gasto` | HOGAR, PERSONAL | `gasto_diario`, `comida_fuera_hogar` |
| `tipo_comida` | DESAYUNO, ALMUERZO, CENA, ONCES | `comida_fuera_hogar` |

`grupo_alimento` y `lugar_compra` siguen siendo tablas, como en el E-R:
- `grupo_alimento` tiene 25 valores con nombres largos, y el capítulo C necesita recorrerlos para armar el formulario.
- `lugar_compra` tiene códigos que no son consecutivos.

---

## 5. Diccionario de datos

Abreviaturas: **PK** = llave primaria, **FK** = llave foránea, **NN** = obligatorio (`NOT NULL`), **UQ** = único.

### Columnas de auditoría

Para no repetirlas en cada tabla, se describen aquí una sola vez. Todas las tablas las tienen, menos `region`, `departamento` y `municipio`, que solo llevan `created_at` porque son catálogos que se cargan una vez y no se editan.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| created_at | TIMESTAMP | NN, default `SYSTIMESTAMP` | Fecha y hora de creación del registro |
| updated_at | TIMESTAMP | | Fecha y hora de la última modificación |
| created_by | NUMBER(10) | FK → usuario | Usuario que creó el registro (nulo en el primer administrador) |

### Seguridad

**`modulo`**: secciones de la aplicación a las que se da acceso por rol.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| nombre | VARCHAR2(60) | NN | Nombre visible del módulo |
| slug | VARCHAR2(60) | NN, UQ | Identificador para el menú (`usuarios`, `digitacion`…) |

**`rol`**

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| nombre | VARCHAR2(30) | NN, UQ | ADMIN, SUPERVISOR, ENCUESTADOR |

**`rol_modulo`**: permisos (relación TIENE PERMISO).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| rol_id | NUMBER(10) | NN, FK → rol | Rol |
| modulo_id | NUMBER(10) | NN, FK → modulo | Módulo permitido |

Restricción única: `(rol_id, modulo_id)`, para que un rol no tenga el mismo módulo dos veces.

**`usuario`**

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| email | VARCHAR2(120) | NN, UQ | Correo para iniciar sesión |
| password | VARCHAR2(255) | NN | Hash de la contraseña |
| nombre | VARCHAR2(80) | NN | Nombres |
| apellido | VARCHAR2(80) | NN | Apellidos |
| telefono | VARCHAR2(20) | | Teléfono de contacto |
| rol_id | NUMBER(10) | NN, FK → rol | Rol del usuario |

**`encuestador`**: usuarios que hacen trabajo de campo.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| usuario_id | NUMBER(10) | NN, UQ, FK → usuario | Usuario asociado |

### Ubicación

**`region`**

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| nombre | VARCHAR2(60) | NN, UQ | Nombre de la región (A.1) |

**`departamento`**

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| nombre | VARCHAR2(60) | NN, UQ | Nombre del departamento (A.2) |
| region_id | NUMBER(10) | NN, FK → region | Región a la que pertenece |

**`municipio`**

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| nombre | VARCHAR2(80) | NN | Nombre del municipio (A.3) |
| departamento_id | NUMBER(10) | NN, FK → departamento | Departamento al que pertenece |

### Capítulo A, B y D2

**`vivienda`**: el espacio físico (casa, apartamento, habitación con entrada independiente) donde viven uno o más hogares (capítulo A).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| municipio_id | NUMBER(10) | NN, FK → municipio | Municipio (A.3) |
| clase | NUMBER(1) | NN | A.4 Clase |
| sector | VARCHAR2(10) | | A.5 Sector |
| seccion | VARCHAR2(10) | | A.6 Sección |
| manzana | VARCHAR2(10) | | A.7 Manzana |
| segmento | VARCHAR2(10) | | A.8 Segmento |
| edificacion_numero | NUMBER(4) | | A.10 Edificación número |
| vivienda_numero | NUMBER(4) | | A.11 Vivienda número |
| total_hogares | NUMBER(2) | NN | A.12 Total de hogares en la vivienda |
| barrio | VARCHAR2(120) | | A.15 Barrio, centro poblado o vereda |
| direccion | VARCHAR2(200) | NN | A.16 Dirección de la vivienda |
| telefono | VARCHAR2(20) | | A.17 Teléfono |

**`hogar`**: persona o grupo de personas que comparten la vivienda y un presupuesto común para la comida. Cada hogar recibe su propio cuadernillo (capítulo A).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| vivienda_id | NUMBER(10) | NN, FK → vivienda | Vivienda donde vive el hogar |
| hogar_numero | NUMBER(2) | NN | A.13 Hogar número dentro de la vivienda |
| total_personas | NUMBER(2) | NN | A.14 Total de personas en el hogar |
| ciclo | VARCHAR2(10) | | A.9 Ciclo |

Restricción única: `(vivienda_id, hogar_numero)`, para que no haya dos hogares con el mismo número en una vivienda.

**`encuesta`**: el cuadernillo aplicado a un hogar, con el periodo de 14 días y las preguntas del cierre (D2).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| hogar_id | NUMBER(10) | NN, UQ, FK → hogar | Hogar encuestado |
| encuestador_id | NUMBER(10) | NN, FK → encuestador | Quien diligencia y crea la encuesta |
| informante_nombre | VARCHAR2(120) | | Nombre de la persona que responde el cuadernillo |
| informante_orden | NUMBER(2) | | Número de orden de la persona que responde |
| resultado_cuadernillo | enum `resultado_entrevista` | | A.18 Resultado del cuadernillo |
| fecha_desde | DATE | NN | A.21 Inicio de los 14 días |
| fecha_hasta | DATE | NN | A.22 Fin (fecha_desde + 13 días) |
| gasto_fiestas | CHAR(1) | | D2.1 ¿Hubo gasto por fiestas o invitados? (S/N) |
| forma_pago | enum `forma_pago` | | D2.2 Principal forma de pago |
| forma_pago_otra | VARCHAR2(100) | | D2.2 Cuál, si la forma de pago es OTRA |
| observaciones | VARCHAR2(1000) | | Observaciones generales |

**`visita`**: control de visitas (capítulo B).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| hogar_id | NUMBER(10) | NN, FK → hogar | Hogar visitado |
| numero_visita | NUMBER(2) | NN | Número de visita (1 a 14) |
| fecha | DATE | NN | Fecha de la visita |
| hora_inicio | TIMESTAMP | NN | Hora de inicio (incluye la jornada AM/PM) |
| hora_fin | TIMESTAMP | | Hora de terminación |
| resultado_visita | enum `resultado_entrevista` | NN | Resultado de la entrevista |
| partes_pendientes | VARCHAR2(200) | | Personas o partes pendientes por diligenciar |
| fecha_proxima | DATE | | Fecha de la próxima visita |
| hora_proxima | TIMESTAMP | | Hora de la próxima visita |

Restricción única: `(hogar_id, numero_visita)`.

### Capítulo C

**`grupo_alimento`**: catálogo de los 25 grupos de alimentos.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Código del grupo (1 a 25) |
| nombre | VARCHAR2(200) | NN, UQ | Nombre del grupo |

**`caracteristica_gasto`**: un renglón por cada grupo de alimentos de la encuesta.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| encuesta_id | NUMBER(10) | NN, FK → encuesta | Encuesta a la que pertenece |
| grupo_alimento_id | NUMBER(10) | NN, FK → grupo_alimento | C.1 Grupo de alimentos |
| encuestador_id | NUMBER(10) | NN, FK → encuestador | Quien registra |
| frecuencia_compra | enum `frecuencia_compra` | NN | C.2 Frecuencia de compra |
| valor_ultima_compra | NUMBER(12,2) | | C.3.1 Valor de la última compra (solo si supera la bisemana) |
| comprara_en_periodo | CHAR(1) | | C.3.2 ¿Comprará en los 14 días? (S/N) |

Restricción única: `(encuesta_id, grupo_alimento_id)`. Una encuesta completa tiene 25 filas.

### Capítulos D y D1

**`lugar_compra`**: catálogo de lugares de compra.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Código del cuadernillo (1, 4, 6, 7, 10, 13, 14, 16…25) |
| nombre | VARCHAR2(120) | NN, UQ | Nombre del lugar |

**`registro_diario`**: encabezado de la página de cada día.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| encuesta_id | NUMBER(10) | NN, FK → encuesta | Encuesta a la que pertenece |
| numero_dia | NUMBER(2) | NN | Día del periodo (1 a 14) |
| fecha | DATE | NN | D.1.2 Fecha (el día de la semana se calcula a partir de ella) |
| observaciones | VARCHAR2(1000) | | Observaciones al pie de la página del día |

Restricciones únicas: `(encuesta_id, numero_dia)` y `(encuesta_id, fecha)`.

**`gasto_diario`**: cada renglón de la tabla D.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| registro_diario_id | NUMBER(10) | NN, FK → registro_diario | Día al que pertenece |
| encuestador_id | NUMBER(10) | NN, FK → encuestador | Quien registra |
| lugar_compra_id | NUMBER(10) | FK → lugar_compra | (7) Lugar de compra; nulo si no fue compra |
| numero_orden | NUMBER(3) | NN | (3) Número de renglón |
| nombre_articulo | VARCHAR2(200) | NN | (4) Artículo o servicio |
| cantidad | NUMBER(10,2) | NN | (5.1) Cantidad adquirida |
| unidad_medida | enum `unidad_medida` | NN | (5.2) Unidad de medida |
| unidad_medida_otra | VARCHAR2(60) | | (5.2) Cuál, si la unidad es OTRA |
| equivalencia_gr | NUMBER(10,2) | | (5.3) Equivalencia en gramos (solo unidad OTRA) |
| equivalencia_cm3 | NUMBER(10,2) | | (5.3) Equivalencia en cm³ (solo unidad OTRA) |
| forma_adquisicion | enum `forma_adquisicion` | NN | (6) Forma de adquisición |
| forma_adquisicion_otra | VARCHAR2(100) | | (6) Cuál, si la forma es OTRA |
| valor | NUMBER(12,2) | NN | (8) Valor pagado, o estimado si no fue compra |
| frecuencia_compra | enum `frecuencia_compra` | | (9) Frecuencia; nula si no fue compra |
| tipo_gasto | enum `tipo_gasto` | NN | (10) Gasto del hogar o personal |
| beneficiario_mesada | VARCHAR2(120) | | Nombre de la persona a quien se entregó la mesada |

Restricción única: `(registro_diario_id, numero_orden)`.

**`comida_fuera_hogar`**: cada renglón de la tabla D1.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| id | NUMBER(10) | PK | Identificador |
| registro_diario_id | NUMBER(10) | NN, FK → registro_diario | Día al que pertenece |
| encuestador_id | NUMBER(10) | NN, FK → encuestador | Quien registra |
| lugar_compra_id | NUMBER(10) | FK → lugar_compra | (7) Lugar de compra; nulo si no fue compra |
| numero_orden | NUMBER(3) | NN | (3) Número de renglón |
| descripcion | VARCHAR2(200) | NN | (4) Descripción de la comida |
| tipo_comida | enum `tipo_comida` | NN | (4.1) Tipo de comida |
| cantidad | NUMBER(10,2) | NN | (5) Cantidad adquirida |
| forma_adquisicion | enum `forma_adquisicion` | NN | (6) Forma de adquisición |
| forma_adquisicion_otra | VARCHAR2(100) | | (6) Cuál, si la forma es OTRA |
| valor | NUMBER(12,2) | NN | (8) Valor pagado o estimado |
| frecuencia_compra | enum `frecuencia_compra` | | (9) Frecuencia; nula si no fue compra |
| tipo_gasto | enum `tipo_gasto` | NN | (10) Gasto del hogar o personal |
| a_domicilio | CHAR(1) | NN, default 'N' | (11) ¿Se pidió a domicilio? (S/N) |

Restricción única: `(registro_diario_id, numero_orden)`.

---

## 6. Correcciones hechas al pasar del E-R al modelo relacional

1. **`registro_diario` no estaba vinculado a nada.** En el E-R, REGISTRO_DIARIO solo se relaciona con GASTO_DIARIO y COMIDA_FUERA_HOGAR. Sin un vínculo con la encuesta no hay forma de saber de qué hogar es cada día. Se agregó `registro_diario.encuesta_id`.
2. **`caracteristica_gasto` tampoco tenía hogar.** En el E-R solo se relaciona con ENCUESTADOR y GRUPO_ALIMENTO, así que se sabía quién la registró pero no de qué hogar era. Se agregó `caracteristica_gasto.encuesta_id`.
3. **Atributos repetidos en CARACTERISTICA_GASTO.** `valor_ultima_compra` y `comprara_en_periodo` aparecen dos veces. Se dejó una sola columna de cada uno.
4. **`lugar_compra` estaba a la vez como atributo y como entidad.** En GASTO_DIARIO y COMIDA_FUERA_HOGAR aparece como atributo y además como relación con LUGAR_COMPRA. Se conservó solo la llave foránea `lugar_compra_id`.
5. **`tipo_compra` en COMIDA_FUERA_HOGAR** se renombró a `tipo_gasto`. La columna (10) del cuadernillo es "gasto del hogar (1) o gasto personal (2)", y así queda igual que en `gasto_diario`.
6. **Cardinalidad de HOGAR – VISITA.** En el dibujo la relación tiene flecha simple en ambos lados, lo que se lee como 1:1. Pero un hogar recibe varias visitas durante los 14 días, así que se implementó como **1:N**. Se recomienda corregir la flecha en el E-R.
7. **HOGAR – ENCUESTA** se implementó como **1:1**, con `encuesta.hogar_id` único. Esto se apoya en que la visita cuelga del hogar en el E-R.
8. **Se separó VIVIENDA de HOGAR.** En el E-R, HOGAR guarda los datos de la vivienda (dirección, manzana, sector…). Como una vivienda puede tener varios hogares (A.12), esos datos se repetirían en cada hogar, lo que no cumple la 3FN. Se creó la tabla `vivienda` con los datos del espacio físico, y `hogar` quedó con `vivienda_id`, `hogar_numero`, `total_personas` y `ciclo`. La relación con MUNICIPIO pasó a `vivienda`. Se recomienda agregar la entidad VIVIENDA en el E-R, con la relación VIVIENDA 1:N HOGAR.
9. **Campos del cuadernillo agregados** que no estaban en el E-R:
   - Tabla `region` (A.1), relacionada con `departamento`.
   - En `hogar`: `ciclo` (A.9), `hogar_numero` (A.13) y `total_personas` (A.14).
   - En `encuesta`: `informante_nombre`, `informante_orden` y `forma_pago_otra`.
   - En `registro_diario`: `observaciones`.
   - En `gasto_diario`: `unidad_medida_otra`, `forma_adquisicion_otra` y `beneficiario_mesada`.
   - En `comida_fuera_hogar`: `forma_adquisicion_otra`.

---

## 7. Recomendaciones (no aplicadas)

Estos campos o reglas no están en el modelo. Se dejan anotados por si el equipo decide agregarlos:

| Campo del cuadernillo | Dónde iría |
|---|---|
| Estado del caso (abierto/cerrado), para bloquear inserciones después del día 14 (RF-06) | `encuesta.estado` |
| D.2 Total de renglones con información | `registro_diario.total_renglones`, o calcularlo con un `COUNT` |
| C.4/C.5 Valor total del mercado (códigos 95–98) | Tabla `mercado_total (encuesta_id, tipo_mercado, valor_total)` |
| Firmas de Recolector, Supervisor y Crítico-digitador con fecha | Tabla `control_cuadernillo` |

También conviene tener en cuenta lo siguiente:
- **`encuestador`** solo guarda `usuario_id`. Si no va a tener atributos propios, se puede eliminar y apuntar las llaves foráneas directamente a `usuario`, porque el rol ya dice quién es encuestador.
- **`visita`** podría colgar de `encuesta` en lugar de `hogar`. Así todo lo que forma parte del cuadernillo (capítulos B, C y D) dependería de la encuesta. Esto también permitiría que un hogar tenga más de una encuesta en el futuro.
