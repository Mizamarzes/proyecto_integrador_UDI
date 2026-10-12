# Proyecto Integrador UDI — ENPH Cuadernillo 2

Aplicación de escritorio para registrar y procesar los datos del **Cuadernillo 2
(gastos diarios urbanos del hogar)** de la *Encuesta Nacional de Presupuestos de los
Hogares (ENPH)* del DANE.

Proyecto Integrador de IV semestre de Ingeniería de Sistemas de la Universidad de
Investigación y Desarrollo (UDI), II-2026. Integra tres asignaturas: Ingeniería de
Software I, Bases de Datos II y Programación II.

## Resumen

- **Diseño orientado a objetos** con arquitectura **cliente–servidor**: la aplicación
  JavaFX (cliente) se conecta por JDBC a una base de datos **Oracle 10g XE** (servidor).
- **Usuarios y roles**: Administrador, Supervisor y Encuestador. Cada rol ve solo los
  módulos que tiene asignados (`rol_modulo`).
- **Encuestas**: registro del hogar con una ventana de seguimiento de 14 días, gastos
  diarios, formas de adquisición (compra, pago, regalo, trueque…), visitas y cierre del
  caso (después del cierre no se pueden agregar registros; solo el administrador puede
  reabrir).

Los requerimientos (RF-01…), el modelo entidad–relación y el diccionario de datos están
en `docs/referencias/` (ver `docs/README.md`).

## Estado actual

| Parte | Estado |
|---|---|
| Base de datos (tablas, secuencias, datos de prueba) | Hecho — `docs/db/esquema_oracle10g.sql` |
| Backend: modelo, DAO y servicios con reglas de negocio | Hecho |
| Inicio de sesión (contra la base de datos, opción "Recordarme") | Hecho |
| Estructura principal: navbar lateral + encabezado con migas | Hecho |
| Módulo **Encuestas**: listado con filtros por estado y periodo, formulario (capítulo A y D2), cerrar caso, reabrir, eliminar | Hecho |
| Módulo **Usuarios**: listado con búsqueda y filtro por rol, insignia Activo/Inactivo, formulario crear/editar, cambio de contraseña, activar/desactivar, eliminar | Hecho |
| Hogares, visitas, características del gasto y registro diario | Pendiente |

Por ahora el navbar solo muestra **Encuestas** y **Usuarios** (Inicio y Hogares se
quitaron hasta que estén listos).

## Requisitos

- JDK 11 o superior
- Apache Maven 3.6+
- Docker, con el contenedor de Oracle 10g XE `oracle-xe-10g`

## Tecnologías

- Java 11 con módulos (`module-info.java`)
- JavaFX 13 (`javafx-controls`, `javafx-fxml`) y FXML + CSS
- Oracle 10g XE por JDBC (`ojdbc6` 11.2.0.4; los drivers más nuevos no conectan con 10g)
- Maven (`javafx-maven-plugin`)

## Base de datos

La base de datos corre en Docker: contenedor `oracle-xe-10g`, puerto `1522`, SID `XE`,
administrador `system/oracle`.

```bash
docker start oracle-xe-10g      # si no está corriendo (ver con: docker ps)
```

### Entrar a sqlplus

```bash
docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<contraseña>
docker exec -it oracle-xe-10g sqlplus system/oracle      # como administrador
```

`NLS_LANG` es obligatorio: la base de datos usa `ZHS16GBK` y sin él las tildes se
guardan como `??`.

Para que los resultados se vean en una sola línea por registro:

```sql
SET LINESIZE 200
SET PAGESIZE 100
COLUMN nombre FORMAT A15
COLUMN email  FORMAT A30
```

### Crear el esquema

```bash
docker cp docs/db/esquema_oracle10g.sql oracle-xe-10g:/tmp/esquema_oracle10g.sql
docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<contraseña> @/tmp/esquema_oracle10g.sql
```

El script debe correr sobre un esquema vacío, porque los datos de prueba usan ids fijos
(1, 2, 3…). Al inicio del script hay un bloque de limpieza comentado.

### Conexión de la aplicación

La conexión se configura en `src/main/resources/com/udi/config/db.properties`
(`db.url`, `db.usuario`, `db.contrasena`) y se puede cambiar al ejecutar con
`-Ddb.url`, `-Ddb.usuario` y `-Ddb.contrasena`.

### Usuarios de prueba

Todos tienen la contraseña `123456`.

| Correo | Rol |
|---|---|
| `admin@enph.udi.edu.co` | Administrador |
| `cperez@enph.udi.edu.co` | Supervisor |
| `lmartinez@enph.udi.edu.co` (y otros) | Encuestador |
| `rcastillo@enph.udi.edu.co` | Inactivo (no puede iniciar sesión) |

### Ejemplo de SQL

`docs/db/crud_usuario.sql` tiene el CRUD de la tabla `usuario` (INSERT, SELECT, UPDATE,
DELETE) para practicar en sqlplus. Ejecute cada comando por separado, no el archivo
completo.

## Ejecución

Con el contenedor de Oracle corriendo:

```bash
mvn clean javafx:run     # ejecutar (clase principal com.udi.App)
mvn clean package        # compilar y empaquetar
mvn compile              # solo compilar
```

## Estructura del proyecto

```
proyecto_integrador_UDI/
├── pom.xml
├── docs/
│   ├── arquitectura/arquitectura.md      # Estructura completa, capas y reglas de negocio
│   ├── db/                               # Modelo relacional, esquema Oracle, diagramas, CRUD de ejemplo
│   ├── diseno/                           # Prompt de diseño de la interfaz
│   └── referencias/                      # Enunciado, documento de diseño y Cuadernillo 2 (PDF)
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/udi/
    │       ├── App.java                  # Clase principal y navegación (setRoot)
    │       ├── controller/               # Un controlador por pantalla (Login, Navbar, Encuesta, Usuario…)
    │       ├── service/                  # Reglas de negocio
    │       ├── dao/                      # SQL, un DAO por tabla
    │       ├── model/                    # Entidades y enums
    │       ├── config/                   # ConexionBD (una conexión compartida)
    │       ├── util/                     # Sesion, Tareas, Alertas, Formato, Seguridad, Validar
    │       └── exception/
    └── resources/com/udi/
        ├── config/db.properties          # Conexión a Oracle
        └── ui/
            ├── shared/
            │   ├── estilos.css
            │   └── layout/               # navbar.fxml (estructura principal), pendiente.fxml
            └── modulos/
                ├── auth/                 # login.fxml
                ├── encuestas/            # encuesta.fxml (listado), encuesta_form.fxml
                └── usuarios/             # usuario.fxml (listado), usuario_form.fxml
```

## Arquitectura

MVC con el modelo separado en capas:

```
controller  →  service  →  dao  →  model
(pantallas)    (reglas)    (SQL)   (entidades)
```

- Los controladores nunca llaman a los DAO; siempre pasan por un servicio.
- Las reglas de negocio están en los servicios y lanzan `ReglaNegocioException`
  (por ejemplo: encuesta cerrada, gastos personales solo días 1–7, solo el administrador
  modifica usuarios).
- El esquema no usa triggers: los ids salen de `seq_<tabla>.NEXTVAL` y `updated_at` se
  asigna en cada DAO.
- Los controladores Java están todos en `com.udi.controller`, pero las vistas FXML se
  organizan por módulo en `resources/com/udi/ui/modulos/<modulo>/`.
- **Navegación**: `App.setRoot("<vista>")` cambia la ventana completa (login ↔ navbar) y
  `NavbarController.abrir("<vista>", migas…)` carga una pantalla dentro del navbar. Una
  vista se nombra por su ruta dentro de `ui` sin extensión, por ejemplo
  `"modulos/usuarios/usuario"`.
- Las consultas a la base de datos se ejecutan en segundo plano con `util/Tareas`, para
  que la ventana no se congele; los errores se muestran con `util/Alertas`.

El detalle completo está en `docs/arquitectura/arquitectura.md`.
