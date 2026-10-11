# Arquitectura del proyecto

Aplicación de escritorio **JavaFX** con arquitectura **cliente-servidor**. El cliente se organiza con el patrón **MVC** y se conecta por JDBC a **Oracle 10g**, que hace de servidor.

```
Cliente (JavaFX)                                              Servidor
VISTA          CONTROLADOR         MODELO
.fxml/.css ──▶ controller ──▶ service ──▶ dao ──▶ model    ──JDBC──▶  Oracle 10g
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Vista | `resources/com/udi/view`, `css` | Pantallas (FXML) y estilos. Sin lógica |
| Controlador | `controller` | Una clase por pantalla. Lee los campos, llama al servicio y muestra el resultado |
| Modelo: servicios | `service` | Reglas de negocio del cuadernillo y de los RF |
| Modelo: acceso a datos | `dao` | SQL con JDBC, una clase por tabla |
| Modelo: entidades | `model` | Clases que representan las tablas, y los enums |

## Estructura de carpetas

```
proyecto_integrador_UDI/
├── pom.xml                                   # Dependencias: JavaFX 13, driver Oracle (ojdbc6 11.2.0.4)
├── CLAUDE.md
├── README.md
├── docs/
│   ├── README.md                             # Índice de la documentación
│   ├── referencias/                          # Enunciado, documento de diseño y Cuadernillo 2 (PDF)
│   ├── arquitectura/
│   │   └── arquitectura.md                   # Este archivo
│   ├── db/
│   │   ├── modelo_relacional.dbml            # Modelo relacional (dbdiagram.io)
│   │   ├── modelo_relacional.md              # Documentación y diccionario de datos
│   │   ├── esquema_oracle10g.sql             # DDL + datos de prueba para Oracle 10g
│   │   └── diagramas/                        # Modelo entidad-relación y modelo relacional (PNG)
│   └── diseno/
│       └── prompt_diseno_ui.md               # Prompt para diseñar la interfaz
│
└── src/main/
    ├── java/
    │   ├── module-info.java                  # requires java.sql; opens com.udi.controller to javafx.fxml
    │   └── com/udi/
    │       ├── App.java                      # Punto de entrada y navegación entre pantallas (setRoot)
    │       │
    │       ├── config/
    │       │   └── ConexionBD.java           # Conexión única a Oracle; consultar() y enTransaccion()
    │       │
    │       ├── model/                        # ── MODELO: entidades (una por tabla) ──
    │       │   ├── Entidad.java              # Base: id y created_at
    │       │   ├── EntidadAuditable.java     # Base: + updated_at y created_by
    │       │   ├── Modulo.java
    │       │   ├── Rol.java
    │       │   ├── RolModulo.java
    │       │   ├── Usuario.java
    │       │   ├── Encuestador.java
    │       │   ├── Region.java
    │       │   ├── Departamento.java
    │       │   ├── Municipio.java
    │       │   ├── Vivienda.java
    │       │   ├── Hogar.java
    │       │   ├── Encuesta.java
    │       │   ├── Visita.java
    │       │   ├── GrupoAlimento.java
    │       │   ├── CaracteristicaGasto.java
    │       │   ├── LugarCompra.java
    │       │   ├── RegistroDiario.java
    │       │   ├── GastoDiario.java
    │       │   ├── ComidaFueraHogar.java
    │       │   └── enums/                    # Enums del modelo (en Oracle son VARCHAR2 + CHECK)
    │       │       ├── ResultadoEntrevista.java
    │       │       ├── FormaPago.java
    │       │       ├── FrecuenciaCompra.java
    │       │       ├── UnidadMedida.java
    │       │       ├── FormaAdquisicion.java
    │       │       ├── TipoGasto.java
    │       │       ├── TipoComida.java
    │       │       └── EstadoEncuesta.java
    │       │
    │       ├── dao/                          # ── MODELO: acceso a datos (SQL con JDBC), un DAO por tabla ──
    │       │   ├── BaseDAO.java              # buscarPorId, listar, eliminar, contar + conversión de tipos
    │       │   ├── ModuloDAO.java
    │       │   ├── RolDAO.java
    │       │   ├── RolModuloDAO.java
    │       │   ├── UsuarioDAO.java
    │       │   ├── EncuestadorDAO.java
    │       │   ├── RegionDAO.java
    │       │   ├── DepartamentoDAO.java
    │       │   ├── MunicipioDAO.java
    │       │   ├── ViviendaDAO.java
    │       │   ├── HogarDAO.java
    │       │   ├── EncuestaDAO.java
    │       │   ├── VisitaDAO.java
    │       │   ├── GrupoAlimentoDAO.java
    │       │   ├── CaracteristicaGastoDAO.java
    │       │   ├── LugarCompraDAO.java
    │       │   ├── RegistroDiarioDAO.java
    │       │   ├── GastoDiarioDAO.java
    │       │   └── ComidaFueraHogarDAO.java
    │       │
    │       ├── service/                      # ── MODELO: reglas de negocio ──
    │       │   ├── AuthService.java          # RF-01 Login, usuario activo, ultimo_acceso
    │       │   ├── UsuarioService.java       # RF-01 Usuarios, roles, módulos, permisos y encuestadores
    │       │   ├── UbicacionService.java     # Región, departamento y municipio
    │       │   ├── CatalogoService.java      # Grupos de alimentos y lugares de compra
    │       │   ├── HogarService.java         # RF-02 Vivienda y hogares
    │       │   ├── EncuestaService.java      # RF-02, RF-06 Periodo de 14 días, cerrar y reabrir el caso
    │       │   ├── VisitaService.java        # RF-08 Control de visitas
    │       │   ├── CaracteristicaService.java# RF-07 Los 25 grupos del capítulo C
    │       │   ├── GastoDiarioService.java   # RF-03, RF-04, RF-09 Días, gastos y comidas fuera del hogar
    │       │   └── ReglasEncuesta.java       # Regla compartida: no se modifica una encuesta CERRADA
    │       │
    │       ├── controller/                   # ── CONTROLADOR: una clase por pantalla (pendiente) ──
    │       │   ├── LoginController.java
    │       │   ├── MenuController.java
    │       │   ├── UsuarioController.java
    │       │   ├── HogarController.java
    │       │   ├── EncuestaController.java
    │       │   ├── VisitaController.java
    │       │   ├── CaracteristicaController.java
    │       │   └── RegistroDiarioController.java  # Página D + D1 del cuadernillo
    │       │
    │       ├── exception/
    │       │   ├── ReglaNegocioException.java     # Error de validación que se muestra al usuario
    │       │   └── DatosException.java            # Error técnico de base de datos (envuelve SQLException)
    │       │
    │       └── util/
    │           ├── Sesion.java               # Usuario logueado (rol, permisos, created_by)
    │           ├── Seguridad.java            # Hash SHA-256 de las contraseñas
    │           ├── Validar.java              # Validaciones comunes de los servicios
    │           └── Alertas.java              # Diálogos de información, error y confirmación (pendiente)
    │
    └── resources/com/udi/
        ├── config/
        │   └── db.properties                 # URL, usuario y contraseña de Oracle
        ├── view/                             # ── VISTA: pantallas FXML (pendiente) ──
        │   ├── login.fxml
        │   ├── menu.fxml
        │   ├── usuario.fxml
        │   ├── hogar.fxml
        │   ├── encuesta.fxml
        │   ├── visita.fxml
        │   ├── caracteristica.fxml
        │   └── registro_diario.fxml
        ├── css/
        │   └── estilos.css
        └── img/
            └── logo.png
```

## Acceso a datos

- **Una sola conexión.** La aplicación es de escritorio y la usa una persona a la vez, así que `ConexionBD` mantiene una única conexión abierta durante la sesión y la vuelve a abrir si se cae. Abrir una conexión por cada operación agota los procesos de Oracle XE (error `ORA-12519`). `App.stop()` la cierra al salir.
- **Lecturas y escrituras.** Los servicios usan `ConexionBD.consultar(c -> ...)` para leer y `ConexionBD.enTransaccion(c -> ...)` para escribir. Esta última hace commit si todo sale bien y rollback si se lanza cualquier excepción.
- **Los DAO reciben la conexión** en cada método. Así un servicio puede agrupar varias operaciones en una misma transacción, por ejemplo crear un usuario y su encuestador.
- **Ids:** cada `insertar` toma el id de `seq_<tabla>.NEXTVAL`. Si la entidad ya trae id, lo respeta; los catálogos lo usan para guardar el código del cuadernillo.
- **Conversión de tipos:** `LocalDate` ↔ `DATE`, `LocalDateTime` ↔ `TIMESTAMP`, `boolean` ↔ `CHAR(1)` `'S'`/`'N'` y enum ↔ `VARCHAR2` con el nombre del enum.
- **Configuración:** `db.properties` (por defecto `jdbc:oracle:thin:@localhost:1522:XE`, usuario `enph`). Cada valor se puede sobrescribir al ejecutar con `-Ddb.url=...`, `-Ddb.usuario=...` o `-Ddb.contrasena=...`.

## Reglas de dependencia

- La **vista** solo conoce a su **controlador**, que se enlaza con `fx:controller` y `onAction="#metodo"`.
- El **controlador** solo llama a **servicios**. Nunca usa un DAO ni escribe SQL.
- El **servicio** aplica las reglas de negocio y llama a uno o varios **DAO**. Si algo no se cumple, lanza `ReglaNegocioException`.
- El **DAO** solo ejecuta SQL y convierte filas en objetos de `model`.
- Las clases de **`model`** no dependen de ninguna otra capa.

## Reglas de negocio que implementan los servicios

El esquema de base de datos no usa triggers, así que estas reglas son responsabilidad de la capa `service`:

| Regla | Servicio |
|---|---|
| Un usuario inactivo (`activo = 'N'`) no puede iniciar sesión, y en cada login exitoso se actualiza `ultimo_acceso` | `AuthService` |
| No se registran visitas, capítulo C, días, gastos ni comidas si la encuesta está `CERRADA` (RF-06) | `VisitaService`, `CaracteristicaService`, `GastoDiarioService` |
| Al cerrar un caso se llenan `fecha_cierre` y `cerrada_por`; solo un administrador puede reabrirlo | `EncuestaService` |
| `fecha_hasta = fecha_desde + 13` | `EncuestaService` |
| La fecha de cada día es `fecha_desde + numero_dia - 1` | `GastoDiarioService` |
| Los gastos `PERSONAL` solo se registran del día 1 al 7 | `GastoDiarioService` |
| El capítulo C debe tener los 25 grupos antes de pasar a los registros diarios (RF-07) | `CaracteristicaService` |
| `created_by` toma el usuario de `Sesion` | Todos |
| `updated_at = SYSTIMESTAMP` en cada `UPDATE` | Todos los DAO |
| Las operaciones de usuarios, roles, módulos y catálogos solo las hace un administrador | `UsuarioService`, `UbicacionService`, `CatalogoService` |
| Si no fue compra, el lugar y la frecuencia van vacíos; si fue compra, la frecuencia es obligatoria; `NUNCA` solo en el capítulo C; "Otra" exige el ¿cuál?; la equivalencia solo con unidad OTRA | `GastoDiarioService` |
| C.3.1 y C.3.2 solo cuando la frecuencia supera la bisemana | `CaracteristicaService` |
| El número del hogar va de 1 al total de hogares de la vivienda | `HogarService` |
