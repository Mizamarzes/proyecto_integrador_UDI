# Documentación del proyecto

Software para el registro y procesamiento del **Cuadernillo 2 (gastos diarios del hogar, área urbana)** de la Encuesta Nacional de Presupuestos de los Hogares (ENPH) del DANE. Proyecto Integrador IV, UDI.

## Contenido

| Carpeta | Archivo | Qué contiene |
|---|---|---|
| `referencias/` | `Proyecto Integrador IV.pdf` | Enunciado del proyecto: requerimientos, entregables y fechas |
| | `Proyecto integrador IV semestre … .pdf` | Documento de diseño del equipo: requisitos funcionales (RF-01 a RF-09), prototipos, casos de uso y diagramas |
| | `Cuadernillo 2 urbano.pdf` | Formulario oficial del DANE con los campos que captura la aplicación |
| `arquitectura/` | `arquitectura.md` | Arquitectura MVC en capas, estructura de carpetas del código, acceso a datos y reglas de negocio |
| `db/` | `modelo_relacional.dbml` | Modelo relacional en formato dbdiagram.io. Es la fuente de verdad del esquema |
| | `modelo_relacional.md` | Explicación del paso del modelo entidad-relación al relacional, y diccionario de datos |
| | `esquema_oracle10g.sql` | Script para Oracle 10g: tablas, restricciones, secuencias, catálogos y datos de prueba |
| `db/diagramas/` | `modelo-entidad-relacion.png` | Diagrama entidad-relación |
| | `relacional_Db_diagram.png` | Diagrama del modelo relacional (exportado de dbdiagram.io) |
| `diseno/` | `prompt_diseno_ui.md` | Prompt para diseñar la interfaz: roles, módulos, pantallas y reglas que debe reflejar |

## Por dónde empezar

1. Para entender qué se pide: `referencias/Proyecto Integrador IV.pdf` y el documento de diseño.
2. Para entender el código: `arquitectura/arquitectura.md`.
3. Para crear la base de datos: `db/esquema_oracle10g.sql`. Los pasos para ejecutarlo en SQL*Plus están al inicio del archivo.
4. Si cambia el esquema: actualizar primero `db/modelo_relacional.dbml`, luego `modelo_relacional.md`, el script SQL y los diagramas.
