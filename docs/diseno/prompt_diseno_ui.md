# Prompt para el diseño de interfaz

> Copia todo lo que está debajo de la línea en la herramienta de diseño (Figma AI, Stitch, v0, Claude, etc.).

---

Diseña la interfaz completa de una **aplicación de escritorio** llamada **"ENPH Urbana — Cuadernillo 2"**. Sirve para registrar y procesar los datos del **Cuadernillo 2 (gastos diarios del hogar, área urbana)** de la *Encuesta Nacional de Presupuestos de los Hogares (ENPH)* del DANE (Colombia). Es un proyecto universitario (UDI, Ingeniería de Sistemas).

Todo el texto de la interfaz va en **español de Colombia**. Usa datos realistas colombianos: direcciones como "Calle 45 # 12-30", municipios como Bucaramanga, Floridablanca o Medellín, y valores en pesos con separador de miles ("$ 14.000").

## 1. Contexto

- **Plataforma:** aplicación de escritorio. Tienes libertad total para proponer la estructura, la navegación, los componentes, las interacciones y las animaciones que mejor sirvan a los usuarios.
- **Usuarios:** encuestadores de campo, supervisores, críticos digitadores y administradores. Lo usan muchas horas digitando datos, así que priorizan la **velocidad de captura con teclado** (Tab entre campos, Enter para guardar, foco automático en el primer campo) por encima de lo decorativo.

## 2. Sistema visual

Tienes libertad total para elegir la identidad visual: paleta de colores, tipografías, iconografía, estilo de los componentes y distribución de las pantallas. Propón lo que mejor funcione para una herramienta institucional de captura de datos que se usa durante muchas horas.

Entrega primero una página de **sistema de diseño**: paleta, tipografías, botones (primario, secundario, peligro, enlace), campos (normal, foco, error, deshabilitado), chips de estado, tabla, tarjeta de indicador, diálogo de confirmación, notificaciones de éxito y error, y estado vacío.

## 3. Roles y navegación

Al iniciar sesión, la barra lateral muestra **solo los módulos que el rol tiene permitidos**. Los permisos se configuran por rol (tabla rol × módulo).

| Rol | Qué hace (casos de uso) | Módulos que ve |
|---|---|---|
| **Administrador** | Gestión de usuarios; verificación y cierre de casos; reabrir casos cerrados | Inicio, Encuestas, Viviendas y hogares, Usuarios, Roles y permisos, Catálogos, Reportes |
| **Supervisor** | Revisa el avance y verifica y cierra los casos | Inicio, Encuestas, Verificación de casos, Reportes |
| **Encuestador** | Gestión de hogares, control de visitas, capítulo C, gastos diarios y comidas fuera del hogar | Inicio, Mis encuestas, Viviendas y hogares |
| **Crítico digitador** | Transcribe y consolida lo que se recogió en papel (capítulo C, gastos y comidas) | Inicio, Encuestas (digitación) |

Todos los roles tienen además **Mi perfil** (ver sus datos y cambiar contraseña), en el menú del usuario de la barra superior.

## 4. Reglas de negocio que la interfaz debe reflejar

Estas reglas ya están implementadas en el sistema. La interfaz debe **anticiparlas** (deshabilitando campos u ocultando opciones) y **mostrar el mensaje** cuando el sistema las rechace:

1. **Periodo de 14 días:** cada encuesta va de `fecha desde` a `fecha hasta`, y `fecha hasta` = `fecha desde` + 13 días (se calcula sola, no se escribe). El día N de la encuesta tiene fecha `fecha desde + N − 1`.
2. **Estado del caso:** `Abierta`, `Cerrada` o `Reabierta`. Una encuesta **cerrada** no permite crear, editar ni borrar nada: visitas, capítulo C, días, gastos y comidas quedan en solo lectura, con un aviso visible. Solo el **Administrador** puede reabrirla.
3. **Capítulo C primero:** no se pueden registrar días de gasto hasta completar los **25 grupos de alimentos** del capítulo C.
4. **Gastos personales solo los días 1 a 7:** en los días 8 a 14, la opción "Gasto personal" se deshabilita.
5. **Forma de adquisición:** si **no** es "Compra" (regalo, pago por trabajo, trueque, producción propia, negocio propio u otra), el **lugar de compra** y la **frecuencia** se deshabilitan y se vacían, y el valor se rotula **"Valor estimado"**. Si es "Compra", la frecuencia es obligatoria y el lugar es opcional (un pasaje de bus no tiene lugar).
6. **"Otra, ¿cuál?":** al elegir "Otra" en unidad de medida, forma de adquisición o forma de pago, aparece un campo de texto obligatorio "¿Cuál?".
7. **Equivalencia en gramos o cm³:** solo se habilita si la unidad es "Otra" (atado, bolsa, manojo, racimo…), y se llena solo una de las dos.
8. **Frecuencia "Nunca":** solo existe en el capítulo C, no en los gastos diarios.
9. **Capítulo C:** "Valor de la última compra" y "¿Comprará en los 14 días?" solo se habilitan si la frecuencia es Mensual, Bimestral, Trimestral o Esporádica.
10. **Vivienda y hogar:** una vivienda puede tener varios hogares (personas que comparten la vivienda pero tienen presupuesto de comida propio). El "Hogar número" va de 1 al "Total de hogares" de la vivienda. Cada hogar tiene **una** encuesta.
11. **Usuarios:** un usuario inactivo no puede iniciar sesión. Nadie puede desactivarse ni eliminarse a sí mismo. La contraseña tiene mínimo 6 caracteres.

## 5. Pantallas

Marca cada pantalla con su prioridad: **P1** = prototipo funcional (segundo avance), **P2** = siguiente iteración, **P3** = entrega final.

### 5.1 Inicio de sesión — P1
Contenido:
- Identidad del sistema: "ENPH" y el título "Encuesta Nacional de Presupuestos de los Hogares", con el subtítulo "Área urbana · Cuadernillo 2 · Gastos diarios del hogar".
- Formulario "Iniciar sesión" con **Correo**, **Contraseña** (con opción para mostrarla) y el botón "Ingresar".

Estados a diseñar:
- error "Correo o contraseña incorrectos";
- error "El usuario está inactivo. Comuníquese con el administrador";
- botón cargando;
- campos vacíos.

### 5.2 Inicio (dashboard) — P1
Tiene contenido distinto según el rol.

- **Encuestador:**
  - Indicadores "Mis encuestas abiertas", "Visitas programadas hoy", "Encuestas por cerrar" y "Días pendientes de registrar".
  - Lista **"Mis hogares en seguimiento"**: cada fila muestra dirección, hogar N°, encuestador, una barra **día X de 14**, el estado del capítulo C (n/25) y la próxima visita con fecha y hora. Al hacer clic se abre el detalle de la encuesta.
- **Supervisor y Administrador:**
  - Indicadores "Encuestas abiertas", "Cerradas", "Reabiertas" y "Hogares registrados".
  - Gráfico circular de encuestas por **estado** y por **resultado del cuadernillo**, y gráfico de barras con encuestas iniciadas por mes.
  - Tabla **"Casos listos para verificar"**: encuestas cuyo día 14 ya pasó y siguen abiertas.
  - Lista "Actividad reciente".

### 5.3 Viviendas y hogares (Capítulo A) — P1
- **Listado de viviendas:**
  - Filtros: buscar por dirección o barrio, Departamento → Municipio (en cascada).
  - Columnas: ID, dirección, barrio, municipio, hogares registrados/total (por ejemplo, "1/2"), acciones.
  - Botón "+ Nueva vivienda".
- **Formulario de vivienda** (diálogo con dos columnas, en el orden del capítulo A):
  - Ubicación: Región → Departamento → Municipio (selectores en cascada) y Clase (1 = Cabecera).
  - Datos muestrales: Sector, Sección, Manzana, Segmento, Edificación número, Vivienda número.
  - Total de hogares en la vivienda.
  - Dirección (obligatoria), Barrio o centro poblado, Teléfono.
- **Detalle de vivienda:**
  - Datos de la vivienda.
  - Lista de sus hogares: Hogar N°, Total de personas, Ciclo, Encuesta (chip de estado o el botón "Crear encuesta").
  - Botón "+ Agregar hogar". Se deshabilita si ya están registrados todos los hogares, con un tooltip que lo explique.
- **Formulario de hogar:** Hogar número (de 1 al total de hogares), Total de personas en el hogar y Ciclo.

### 5.4 Encuestas — P1
- **Listado:**
  - Filtros: buscar (ID, dirección o informante), Estado (Todos/Abierta/Cerrada/Reabierta), Encuestador y Periodo.
  - Columnas: ID encuesta, hogar y dirección, encuestador, periodo (dd/mm/aaaa – dd/mm/aaaa), avance (barra "día X de 14"), capítulo C (n/25), resultado del cuadernillo (EC, EI, NH, AT, R, OC, V, O, con tooltip que explique el código), estado (chip) y "Ver / editar".
  - Al encuestador le aparece como **"Mis encuestas"**, filtrada por él.
- **Nueva encuesta** (diálogo):
  - Hogar: buscador que solo muestra hogares sin encuesta, mostrando dirección y hogar N°.
  - Encuestador.
  - "Anote las adquisiciones desde el día" (selector de fecha) y "Hasta el día", calculado y de solo lectura.
  - Nombre y número de orden de la persona que responde.

### 5.5 Detalle de la encuesta ("el cuadernillo") — P1
Es la pantalla central del sistema.
- **Encabezado:**
  - ID de la encuesta, dirección y hogar N°, informante, encuestador, periodo y chip de estado grande.
  - Acciones según el rol: "Cerrar caso" (Supervisor/Admin) y "Reabrir caso" (solo Admin, solo si está cerrada).
- **Línea de tiempo de 14 días:**
  - 14 casillas (Día 1 … Día 14) con la fecha corta y el día de la semana.
  - Estado de cada casilla: registrado (con el número de renglones), sin registrar, hoy o futuro.
  - Una marca sutil sobre los días 1 a 7: "admite gastos personales".
- **Si la encuesta está cerrada:** aviso visible "Caso cerrado el dd/mm/aaaa por [usuario]. Solo lectura.", y todos los controles de edición deshabilitados.
- **Secciones** (pestañas u otra navegación que propongas):
  1. **Resumen:** totales del periodo (gasto del hogar, gasto personal, total), gasto por día (barras), gasto por forma de adquisición y lista de verificación de completitud (capítulo C 25/25, días registrados n/14, visitas, D2 respondida).
  2. **Capítulo C** (5.6).
  3. **Visitas (Capítulo B)** (5.7).
  4. **Gastos diarios (D y D1)** (5.8).
  5. **Cierre (D2)** (5.9).

### 5.6 Capítulo C — Características del gasto en alimentos — P1
Título: "¿Con qué frecuencia compran generalmente los siguientes alimentos o grupos de alimentos en el hogar?"

- **Tabla editable con los 25 grupos fijos**, sin paginar:
  - Código (01–25) y nombre del grupo (por ejemplo, "01 Pan, arepas, bollos y almojábanas", "04 Carne de res, cerdo, hueso y vísceras", "24 Agua, gaseosas, refrescos, jugos…").
  - **Frecuencia de compra:** 1 Nunca, 2 Diario, 2.1 Varias veces por semana, 3 Semanal, 4 Quincenal, 5 Mensual, 6 Bimestral, 7 Trimestral, 9 Esporádica.
  - **C.3.1 Valor de la última compra** ($) y **C.3.2 ¿Comprará durante los 14 días?** (Sí/No). Se habilitan solo para 5, 6, 7 y 9 (regla 9).
- **Indicador de avance** "18 de 25 grupos diligenciados" con barra, y filas pendientes resaltadas.
- **Mensaje fijo:** "Complete los 25 grupos para habilitar el registro de gastos diarios".
- Botón "Guardar capítulo C".

### 5.7 Control de visitas (Capítulo B) — P2
- **Tabla de visitas:** N° (1–14), fecha, hora de inicio y de terminación (con jornada AM/PM), resultado (chip con código y descripción), partes pendientes y próxima visita (fecha y hora).
- **Formulario de visita:**
  - N° (automático).
  - Fecha, Hora de inicio y Hora de terminación. Si la hora de terminación es anterior a la de inicio, se muestra un error en línea.
  - **Resultado de la entrevista:** EC Encuesta completa, EI Encuesta incompleta, NH Nadie en el hogar, AT Ausente temporal, R Rechazo, OC Ocupado, V Vacante, O Otro motivo.
  - "Personas y/o partes del cuadernillo pendientes por diligenciar".
  - "Fecha y hora de la próxima visita", con una sugerencia automática.
- **Vista lateral opcional:** mini calendario con las visitas realizadas y programadas.

### 5.8 Gastos diarios — Capítulos D y D1 — P1 (pantalla más importante)
Imita la **página diaria del cuadernillo**, pero optimizada para digitar.

- **Selector de día:** "Día [1–14 ▾]". Muestra el día de la semana y la fecha (calculados), "Total de renglones con información" (automático) y Observaciones del día.
  - Si el capítulo C no está completo, la pantalla queda bloqueada con un aviso y un botón "Ir al capítulo C".
  - Si el día aún no existe, muestra el estado vacío "Este día no tiene registro" y el botón "Crear registro del día N".

- **Tabla D. Gastos diarios** (editable; columnas con el número del cuadernillo):
  1. (3) N° de orden (automático)
  2. (4) Nombre del artículo o servicio
  3. (5.1) Cantidad
  4. (5.2) Unidad de medida: 1 cm³, 2 Litro, 3 Gramos, 4 Onzas, 5 Libra, 6 Kilo, 7 Arroba, 8 Otra → "¿Cuál?"
  5. (5.3) Equivalencia en g o cm³ (regla 7)
  6. (6) Forma de adquisición: 1 Compra, 2 Pago por trabajo, 3 Regalo o donación, 4 Intercambio o trueque, 5 Traído de la finca o producido por el hogar, 6 Tomado de un negocio del hogar, 7 Otra → "¿Cuál?"
  7. (7) Lugar de compra (catálogo con códigos: 1 Supermercado de cadena, 6 Tienda de barrio, 10 Plaza de mercado, 13 Vendedores ambulantes, 18 Restaurantes, 19 Cafeterías…)
  8. (8) Valor pagado, o **Valor estimado** si no fue compra
  9. (9) Frecuencia de compra (sin "Nunca")
  10. (10) Hogar / Personal
  11. Beneficiario de la mesada (opcional; solo para mesadas)

  Debajo de la tabla, una **fila de captura rápida** (o un formulario compacto en línea) que aplica las reglas 4 a 8 en vivo. Acciones por fila: editar y eliminar (con confirmación). Al pie, el subtotal del día.

- **Tabla D1. Comidas preparadas fuera del hogar:**
  1. (3) N° de orden
  2. (4) Descripción (por ejemplo, "Almuerzo corriente (pollo, ensalada, arroz, sopa)")
  3. (4.1) **Tipo de comida**: selección única con botones segmentados o radio (Desayuno / Almuerzo / Cena / Onces, medias nueves, merienda)
  4. (5) Cantidad
  5. (6) Forma de adquisición
  6. (7) Lugar
  7. (8) Valor
  8. (9) Frecuencia
  9. (10) Hogar / Personal
  10. (11) ¿A domicilio? (sí/no)

- **Ayuda visible:** una pestaña o panel lateral desplegable con la tabla de códigos (unidades, formas de adquisición, lugares y frecuencias), como la pestaña del final del cuadernillo físico.
- **Errores:** los mensajes de validación aparecen junto al campo y en una notificación. Por ejemplo: "Los gastos personales solo se registran los primeros 7 días (este es el día 8)" o "El campo «Lugar de compra» debe quedar vacío si la forma de adquisición no es compra".

### 5.9 Cierre del caso (D2) y verificación — P2
- **Pestaña Cierre**, con las preguntas que se hacen el día que se recoge el cuadernillo:
  - D2.1 "Durante los catorce días… ¿el hogar tuvo algún gasto en alimentos debido a fiestas, reuniones o invitados?" (Sí/No).
  - D2.2 "La principal forma de pago fue": a. Tarjeta débito, b. Tarjeta de crédito, c. Efectivo, d. Fiado, e. Bonos, f. Otra → "¿Cuál?".
  - Resultado del cuadernillo (EC, EI, NH, AT, R, OC, V, O) y observaciones generales.
  - **Lista de verificación** con cada ítem marcado como cumplido o pendiente: capítulo C 25/25, días registrados n/14, visitas registradas y D2 respondida.
  - Botón **"Cerrar caso"** (peligro). Abre un diálogo de confirmación: "Al cerrar el caso no se podrán agregar ni modificar datos. Solo un administrador puede reabrirlo".
- **Pantalla "Verificación de casos"** (Supervisor/Admin): tabla de encuestas listas para cierre o con el periodo vencido, con su lista de verificación resumida y acciones "Revisar" y "Cerrar".
- **Reabrir** (solo Admin): diálogo de confirmación.

### 5.10 Usuarios — P1
- **Listado:** filtros por nombre/correo y por rol. Columnas: nombre, correo, rol, estado (chip Activo/Inactivo), último acceso y acciones (Editar, Activar/Desactivar, Cambiar contraseña).
- **Formulario** (diálogo): Nombres, Apellidos, Correo, Teléfono, Rol, Contraseña y Confirmar contraseña (solo al crear, mínimo 6 caracteres) y un interruptor Activo.
  - Nota: al crear un usuario con rol Encuestador, queda registrado automáticamente como encuestador.
- **Errores:** correo repetido, correo inválido, contraseña corta, e intentar desactivarse a sí mismo (opción deshabilitada con tooltip).

### 5.11 Roles y permisos — P2
- Lista de roles a la izquierda (crear, renombrar, eliminar; no se puede eliminar un rol con usuarios).
- A la derecha, **matriz de permisos**: un permiso activable por módulo (Usuarios, Roles y permisos, Viviendas, Hogares, Encuestas, Control de visitas, Características del gasto, Gastos diarios, Comidas fuera del hogar, Reportes) y el botón "Guardar permisos".

### 5.12 Catálogos — P3
Pantalla con pestañas, solo para el Administrador:
- **Ubicación:** tres listas encadenadas Región → Departamento → Municipio, cada una con agregar, editar y eliminar.
- **Grupos de alimentos:** código (01–25) y nombre.
- **Lugares de compra:** código del cuadernillo y nombre.

### 5.13 Reportes — P3
Informes con filtros por periodo, municipio y encuestador, y botón "Exportar":
- gasto promedio por hogar;
- gasto por grupo de alimentos;
- distribución por forma de adquisición (compra frente a regalo, trueque y producción propia);
- distribución por lugar de compra;
- comidas fuera del hogar por tipo de comida y a domicilio;
- avance del operativo por encuestador (encuestas abiertas o cerradas y días registrados).

### 5.14 Mi perfil — P2
Datos del usuario (solo lectura, salvo el teléfono), rol, último acceso y formulario "Cambiar contraseña" (actual, nueva, confirmar).

## 6. Estados y componentes transversales

Diseña para cada listado y formulario:
- **estado vacío**, con ilustración sobria y acción principal;
- **cargando**;
- **error de conexión con la base de datos**, con un botón "Reintentar";
- **validación en línea** (campo marcado y mensaje junto al campo);
- **notificación de éxito** ("Gasto guardado");
- **diálogo de confirmación** para eliminar y para cerrar caso;
- **modo solo lectura** (encuesta cerrada).

## 7. Entregables esperados

1. Página de sistema de diseño (sección 2).
2. Todas las pantallas de la sección 5, empezando por las **P1**.
3. Para la pantalla 5.8 (Gastos diarios): variantes con forma "Compra", con forma "Regalo" (campos deshabilitados), con unidad "Otra", en el día 8 (personal deshabilitado) y con la encuesta cerrada.
4. Flujo navegable: Login → Inicio → Encuestas → Detalle de encuesta → Capítulo C → Gastos diarios → Cierre.
