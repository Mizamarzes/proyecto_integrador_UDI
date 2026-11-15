# Proyecto Integrador UDI — ENPH Cuadernillo 2

Aplicación de escritorio para registrar y procesar los datos del **Cuadernillo 2
(gastos diarios urbanos del hogar)** de la *Encuesta Nacional de Presupuestos de los
Hogares (ENPH)* del DANE.

Proyecto Integrador de IV semestre de Ingeniería de Sistemas de la Universidad de
Investigación y Desarrollo (UDI), II-2026. Integra tres asignaturas: Ingeniería de
Software I, Bases de Datos II y Programación II.

## Resumen

- **Diseño orientado a objetos** con arquitectura **cliente–servidor**: la aplicación
  JavaFX (cliente) se conecta por JDBC a una base de datos **Oracle** (servidor).
- **Roles**: Administrador (`ADMIN`), Crítico-digitador (`CRITICO_DIGITADOR`) y
  Encuestador (`ENCUESTADOR`). Cada rol ve solo los módulos que tiene asignados
  (`rol_modulo`), y el administrador puede crear roles y editar sus permisos.
- **Flujo de una encuesta**: el encuestador registra el hogar y llena el cuadernillo
  durante 14 días. Luego cierra el caso, que pasa a *Pendiente de aprobación*. El
  crítico-digitador lo verifica y lo **aprueba** o lo **rechaza** con una observación.
  Una encuesta rechazada vuelve al encuestador para corregirla. Las aprobadas se
  exportan a Excel y se **sincronizan** con un esquema central.
- **Base de datos**: la mayoría de las reglas están en los servicios Java y se repiten
  en Oracle como segunda barrera. Los cambios de estado van por procedimientos
  almacenados, y hay disparadores de validación y de auditoría, funciones y reportes en
  PL/SQL.

Los requerimientos (RF-01…), el modelo entidad–relación y el diccionario de datos están
en `docs/referencias/` (ver `docs/README.md`).

## Módulos

| Módulo | Qué hace |
|---|---|
| **Reportes** | Tablero filtrado por ubicación y fechas: indicadores, estado de las encuestas, gasto por tipo, por forma de adquisición y por lugar de compra, grupos de alimentos y tabla por encuestador. Se exporta a Excel |
| **Encuestas** | Listado según el rol y detalle de la encuesta en pestañas: visitas (cap. B), identificación (cap. A), gasto en alimentos (cap. C), línea de 14 días (cap. D y D1), cierre (D2) y correcciones |
| **Viviendas y hogares** | Registro de viviendas y de los hogares de cada una. El encuestador solo ve los que creó |
| **Verificación** | Bandeja del crítico-digitador con validación cruzada (9 reglas), aprobar, rechazar e historial del caso. Además: exportar un caso o todas las aprobadas a Excel, y **«Sincronizar aprobadas»** |
| **Usuarios** | Alta, edición, activación y cambio de contraseña |
| **Roles y permisos** | Matriz de roles × módulos y creación o eliminación de roles |

## Requisitos

- JDK 11 o superior
- Apache Maven 3.6+
- Oracle: el contenedor Docker `oracle-xe-10g` para desarrollo, o el servidor de la UDI

## Tecnologías

- Java 11 con módulos (`module-info.java`)
- JavaFX 13 (`javafx-controls`, `javafx-fxml`) con FXML y CSS
- Oracle 10g por JDBC (`ojdbc6` 11.2.0.4; los drivers más nuevos no conectan con 10g)
- Maven (`javafx-maven-plugin`)
- Excel sin librerías externas (`util/LibroExcel` escribe el `.xlsx` con `java.util.zip`)

## Base de datos

El proyecto usa **dos esquemas** en la misma base Oracle:

| Esquema | Qué es |
|---|---|
| `DJGCJDCMPI` (origen) | El esquema con el que trabaja la aplicación |
| `DJGCJDCMPI_CENTRAL` (destino) | El esquema central: recibe las encuestas **aprobadas** con el botón «Sincronizar aprobadas» |

La sincronización sigue la guía `docs/referencias/SincronizaciónOracle(EsquemaAesquema).pdf`:
- en el origen están las vistas `vis_sinc_*` con los datos que se envían;
- en el destino está el procedimiento `pr_sincronizar_aprobadas`, que las lee e inserta los
  datos. Si una encuesta ya estaba y se volvió a aprobar, la reemplaza; si no cambió, no la
  toca.

### Crear los esquemas

Los scripts y el paso a paso están en
[`docs/sql-creaciones-servidor-udi/README.md`](docs/sql-creaciones-servidor-udi/README.md):

```
docs/sql-creaciones-servidor-udi/
├── db-origen/    # instalar.sql: tablas, catálogos, datos de prueba, PL/SQL y vistas de sincronización
└── db-destino/   # instalar.sql: tablas, solo catálogos, PL/SQL y pr_sincronizar_aprobadas
```

Resumen del orden. Cada comando se ejecuta desde la carpeta de su esquema, con
`NLS_LANG=AMERICAN_AMERICA.AL32UTF8`:

```bash
sqlplus system/<clave>@<servidor> @01_crear_usuario.sql DJGCJDCMPI            # db-origen
sqlplus system/<clave>@<servidor> @01_crear_usuario.sql DJGCJDCMPI_CENTRAL    # db-destino
sqlplus DJGCJDCMPI/<clave>@<servidor> @instalar.sql DJGCJDCMPI_CENTRAL        # db-origen
sqlplus DJGCJDCMPI_CENTRAL/<clave>@<servidor> @instalar.sql DJGCJDCMPI        # db-destino
```

El parámetro del final es el nombre del **otro** esquema:
- el origen lo usa para darle al destino permiso de leer sus vistas;
- el destino lo usa para saber de dónde leerlas.

### Servidores

| | Desarrollo (Docker) | Servidor UDI |
|---|---|---|
| Host | `localhost` | `192.168.254.215` |
| Puerto | `1522` | `1521` |
| SID | `XE` | `ORCL` |
| SQL\*Plus | `docker exec -it -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 oracle-xe-10g sqlplus <usuario>/<clave>` | `sqlplus <usuario>/<clave>@192.168.254.215:1521/ORCL` |

En Docker:

```bash
docker start oracle-xe-10g      # si no está corriendo (ver con: docker ps)
docker exec -it oracle-xe-10g sqlplus system/oracle      # como administrador
```

`NLS_LANG` es obligatorio: la base de datos local usa `ZHS16GBK`, y sin esa variable
las tildes se guardan como `??`.

### Conexión de la aplicación

La conexión se configura en `src/main/resources/com/udi/config/db.properties`, que se crea a partir
de la plantilla:

```bash
cp src/main/resources/com/udi/config/db.properties.example src/main/resources/com/udi/config/db.properties
```

```properties
db.url=jdbc:oracle:thin:@localhost:1522:XE
# Servidor UDI: db.url=jdbc:oracle:thin:@192.168.254.215:1521:ORCL
db.usuario=DJGCJDCMPI
db.contrasena=<clave>
db.esquema=
db.esquemaDestino=DJGCJDCMPI_CENTRAL
```

- `db.esquema`: solo se usa si la aplicación se conecta con `DJGCJDCMPI_APP` en vez del
  dueño de las tablas (ver `db-origen/usuarios_roles.sql`).
- `db.esquemaDestino`: el esquema central al que sincroniza el botón de Verificación.

Cada valor se puede cambiar al ejecutar, por ejemplo
`mvn clean javafx:run -Ddb.url=jdbc:oracle:thin:@192.168.254.215:1521:ORCL`.

### Usuarios de prueba

Todos tienen la contraseña `123456`.

| Correo | Rol |
|---|---|
| `admin@enph.udi.edu.co` | Administrador |
| `cperez@enph.udi.edu.co` | Crítico-digitador (verifica, aprueba y sincroniza) |
| `lmartinez@enph.udi.edu.co` (y otros) | Encuestador |
| `rcastillo@enph.udi.edu.co` | Inactivo (no puede iniciar sesión) |

## Ejecución

Con la base de datos disponible:

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
│   ├── arquitectura/arquitectura.md       # Estructura completa, capas y reglas de negocio
│   ├── sql-creaciones-servidor-udi/       # Scripts para crear los esquemas origen y destino
│   ├── db-documentacion/                  # Modelo relacional, diagramas, migraciones, pruebas y seguridad
│   └── referencias/                       # Enunciado, documento de diseño, Cuadernillo 2 y guías (PDF)
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/udi/
    │       ├── App.java                   # Clase principal y navegación (setRoot)
    │       ├── controller/<modulo>/       # Un controlador por pantalla, por módulo
    │       ├── service/<modulo>/          # Reglas de negocio
    │       ├── dao/<modulo>/              # SQL, un DAO por tabla
    │       ├── model/                     # Entidades y enums
    │       ├── middleware/                # Control de acceso a las vistas (autenticación y permisos)
    │       ├── componentes/               # Controles reutilizables (SelectBuscable, Paginador)
    │       ├── config/                    # ConexionBD (una conexión compartida)
    │       ├── util/                      # Sesion, Tareas, Alertas, Formato, LibroExcel…
    │       └── exception/
    └── resources/com/udi/
        ├── config/db.properties.example   # Plantilla de conexión (copiar como db.properties)
        └── ui/
            ├── shared/                    # estilos.css compartidos y layout/ (navbar, pendiente)
            └── modulos/                   # auth, encuestas, viviendas, reportes, verificacion, usuarios, roles
```

Los módulos Java (`controller`, `service`, `dao`) usan los mismos nombres de módulo:
`auth`, `encuestas`, `viviendas`, `reportes`, `verificacion`, `usuarios`, `roles`,
`catalogos` y `shared`.

## Arquitectura

MVC con el modelo separado en capas:

```
controller  →  service  →  dao  →  model
(pantallas)    (reglas)    (SQL)   (entidades)
```

- Los controladores nunca llaman a los DAO; siempre pasan por un servicio.
- Las reglas de negocio están en los servicios y lanzan `ReglaNegocioException`. Por
  ejemplo: encuesta cerrada, gastos personales solo los días 1–7, o solo se aprueba sin
  errores en la validación.
- La base repite algunas reglas como segunda barrera:
  - disparadores de validación `trg_val_<tabla>`;
  - cambios de estado con procedimientos (`pr_cerrar_encuesta`, `pr_aprobar_encuesta`…);
  - auditoría de cada cambio con los disparadores `trg_cud_<tabla>`.

  Los errores de Oracle `ORA-20001…20008` llegan a la aplicación como mensajes claros.
- **Navegación y permisos**:
  - `App.setRoot("<vista>")` cambia la ventana completa (login ↔ navbar).
  - `NavbarController.abrir("<vista>", migas…)` carga una pantalla dentro del navbar.
  - Antes de cargar cualquier vista, `middleware` revisa la sesión y los permisos del rol.
- Las consultas a la base de datos se ejecutan en segundo plano con `util/Tareas`, para
  que la ventana no se congele; los errores se muestran con `util/Alertas`.
- El código está documentado con Javadoc (clases y métodos principales), y los scripts
  SQL explican en su encabezado qué hacen, cómo se ejecutan y en qué orden.

El detalle completo está en `docs/arquitectura/arquitectura.md`.
