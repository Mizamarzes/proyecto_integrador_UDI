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
│   ├── arquitectura.md                       # Este archivo
│   └── db/
│       ├── modelo_relacional.dbml            # Modelo relacional (dbdiagram.io)
│       ├── modelo_relacional.md              # Documentación y diccionario de datos
│       ├── esquema_oracle10g.sql             # DDL + datos de prueba para Oracle 10g
│       └── *.pdf                             # Enunciado, documento de diseño y Cuadernillo 2
│
└── src/main/
    ├── java/
    │   ├── module-info.java                  # requires java.sql; opens com.udi.controller to javafx.fxml
    │   └── com/udi/
    │       ├── App.java                      # Punto de entrada y navegación entre pantallas (setRoot)
    │       │
    │       ├── config/
    │       │   └── ConexionBD.java           # URL JDBC, usuario y contraseña de Oracle
    │       │
    │       ├── model/                        # ── MODELO: entidades (una por tabla) ──
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
    │       ├── dao/                          # ── MODELO: acceso a datos (SQL con JDBC) ──
    │       │   ├── RolDAO.java
    │       │   ├── ModuloDAO.java
    │       │   ├── UsuarioDAO.java
    │       │   ├── EncuestadorDAO.java
    │       │   ├── UbicacionDAO.java         # region, departamento, municipio
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
    │       │   ├── UsuarioService.java       # RF-01 CRUD de usuarios y permisos por rol
    │       │   ├── HogarService.java         # RF-02 Vivienda y hogares
    │       │   ├── EncuestaService.java      # RF-02, RF-06 Periodo de 14 días, cerrar y reabrir el caso
    │       │   ├── VisitaService.java        # RF-08 Control de visitas
    │       │   ├── CaracteristicaService.java# RF-07 Los 25 grupos del capítulo C
    │       │   └── GastoDiarioService.java   # RF-03, RF-04, RF-09 Días, gastos y comidas fuera del hogar
    │       │
    │       ├── controller/                   # ── CONTROLADOR: una clase por pantalla ──
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
    │       │   └── ReglaNegocioException.java     # Error de validación que se muestra al usuario
    │       │
    │       └── util/
    │           ├── Sesion.java               # Usuario logueado (rol, permisos, created_by)
    │           └── Alertas.java              # Diálogos de información, error y confirmación
    │
    └── resources/com/udi/
        ├── view/                             # ── VISTA: pantallas FXML ──
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
| `created_by` toma el usuario de `Sesion`, y `updated_at` se asigna en cada `UPDATE` | Todos |
