# Arquitectura del proyecto

Aplicación de escritorio **JavaFX** con arquitectura **cliente-servidor**. El cliente se organiza con el patrón **MVC** y se conecta por JDBC a **Oracle 10g**, que hace de servidor.

```
Cliente (JavaFX)                                              Servidor
VISTA          CONTROLADOR         MODELO
.fxml/.css ──▶ controller ──▶ service ──▶ dao ──▶ model    ──JDBC──▶  Oracle 10g
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Vista | `resources/com/udi/ui` (por módulo) | Pantallas (FXML) y estilos. Sin lógica |
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
    │   ├── module-info.java                  # requires java.sql, java.prefs; opens com.udi.controller to javafx.fxml
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
    │       │   ├── EncuestaResumen.java      # Fila del listado de encuestas (no es tabla)
    │       │   ├── Opcion.java               # Id + texto para listas desplegables (no es tabla)
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
    │       │   ├── UsuarioService.java       # RF-01 Usuarios y encuestadores (módulo «usuarios»)
│       │   ├── RolService.java           # RF-01 Roles, módulos y matriz de permisos rol_modulo (módulo «roles»)
    │       │   ├── UbicacionService.java     # Región, departamento y municipio
    │       │   ├── CatalogoService.java      # Grupos de alimentos y lugares de compra
    │       │   ├── HogarService.java         # RF-02 Vivienda y hogares
    │       │   ├── EncuestaService.java      # RF-02, RF-06 Periodo de 14 días, cerrar y reabrir el caso
    │       │   ├── VisitaService.java        # RF-08 Control de visitas
    │       │   ├── CaracteristicaService.java# RF-07 Los 25 grupos del capítulo C
    │       │   ├── GastoDiarioService.java   # RF-03, RF-04, RF-09 Días, gastos y comidas fuera del hogar
    │       │   └── ReglasEncuesta.java       # Regla compartida: no se modifica una encuesta CERRADA
    │       │
    │       ├── controller/                   # ── CONTROLADOR: una clase por pantalla ──
    │       │   ├── LoginController.java      # Hecho
    │       │   ├── NavbarController.java     # Hecho: barra lateral, encabezado y navegación (abrir, migas, marcarModulo)
    │       │   ├── UsuarioController.java    # Hecho: listado de usuarios con búsqueda y filtros por rol y estado
    │       │   ├── UsuarioFormController.java     # Hecho: crear, editar, contraseña, activar/desactivar y eliminar
│       │   ├── RolController.java        # Hecho: matriz de permisos roles × módulos, renombrar y eliminar roles
│       │   ├── RolFormController.java    # Hecho: modal nuevo/renombrar rol
    │       │   ├── EncuestaController.java   # Hecho: «Mis Encuestas» (encuestador) o listado global con filtros
    │       │   ├── NuevaEncuestaController.java   # Hecho: modal, elegir un hogar sin encuesta
    │       │   ├── EncuestaDetalleController.java # Hecho: encabezado y pestañas de la encuesta
    │       │   ├── SeccionEncuesta.java      # Interfaz de los controladores de cada pestaña
    │       │   ├── EncuestaVisitasController.java # Pestaña Control de visitas (RF-08)
    │       │   ├── VisitaFormController.java      # Modal registrar/editar visita
    │       │   ├── EncuestaHogarController.java   # Pestaña Cap. A: informante, periodo, hogar y vivienda
    │       │   ├── EncuestaCapituloCController.java # Pestaña Cap. C: los 25 grupos (RF-07)
    │       │   ├── EncuestaGastosController.java  # Pestaña Línea de 14 días: tablas D y D1 por día
    │       │   ├── RenglonFormController.java     # Modal agregar gasto (D) o comida (D1)
    │       │   ├── EncuestaCierreController.java  # Pestaña Cierre: D2, A.18 y verificación del caso
    │       │   ├── CerrarCasoController.java      # Modal de confirmación para cerrar el caso (RF-06)
    │       │   ├── ViviendaController.java   # Hecho: listado de viviendas
    │       │   ├── ViviendaDetalleController.java # Hecho: vivienda y sus hogares
    │       │   ├── ViviendaFormController.java    # Modal nueva/editar vivienda (Cap. A)
    │       │   └── HogarFormController.java       # Modal agregar/editar hogar (A.13, A.14)
    │       │
    │       ├── middleware/                   # ── Control de acceso a las vistas (como los middlewares de una API) ──
│       │   ├── Middleware.java           # Interfaz: manejar(vista) o lanzar AccesoDenegadoException
│       │   ├── AutenticacionMiddleware.java   # isAuth: exige sesión iniciada
│       │   ├── PermisoModuloMiddleware.java   # ModulePermission: el rol debe tener el módulo de la vista (ADMIN pasa)
│       │   ├── RutasModulo.java          # Vista → slugs de módulo (prefijo más largo) y conjuntos para los servicios
│       │   └── Rutas.java                # Router: cadena de middlewares por ruta; exigir, puedeAbrir y permitir(nodo, vista)
│       │
│       ├── exception/
    │       │   ├── ReglaNegocioException.java     # Error de validación que se muestra al usuario
    │       │   ├── AccesoDenegadoException.java   # Sin sesión o rol sin permiso (subclase de ReglaNegocioException)
    │       │   └── DatosException.java            # Error técnico de base de datos (envuelve SQLException)
    │       │
    │       └── util/
    │           ├── Sesion.java               # Usuario logueado (rol, permisos, created_by)
        │           ├── Seguridad.java            # Hash SHA-256 de las contraseñas
    │           ├── Validar.java              # Validaciones comunes de los servicios
    │           ├── Alertas.java              # Diálogos de información, error y confirmación
    │           ├── Formato.java              # Fechas, horas, pesos, códigos E-000001 y filtros de los campos
    │           ├── Modal.java                # Ventanas modales (partials) con la hoja de estilos
    │           └── Tareas.java               # Llamadas a servicios en segundo plano (una a la vez)
    │
    └── resources/com/udi/
        ├── config/
        │   └── db.properties.example         # Plantilla: copiar como db.properties (ignorado por git)
        └── ui/                               # ── VISTA: pantallas FXML organizadas por módulo ──
            ├── shared/                       # Lo común a todos los módulos
            │   ├── estilos.css               # Hoja de estilos única
            │   └── layout/
            │       ├── navbar.fxml           # Hecho: estructura con barra lateral y encabezado
            │       └── pendiente.fxml        # Aviso de módulo en construcción
            └── modulos/
                ├── auth/
                │   └── login.fxml            # Hecho
                ├── encuestas/
                │   ├── encuesta.fxml         # Hecho: listado (Mis Encuestas / global)
                │   ├── encuesta_detalle.fxml # Hecho: encabezado y pestañas
                │   └── partials/
                │       ├── nueva_encuesta.fxml       # Modal: elegir el hogar
                │       ├── encuesta_visitas.fxml     # Pestaña Control de visitas
                │       ├── encuesta_hogar.fxml       # Pestaña Cap. A
                │       ├── encuesta_capitulo_c.fxml  # Pestaña Cap. C
                │       ├── encuesta_gastos.fxml      # Pestaña Línea de 14 días (D y D1)
                │       ├── encuesta_cierre.fxml      # Pestaña Cierre (D2 y verificación)
                │       ├── visita_form.fxml          # Modal visita
                │       ├── renglon_form.fxml         # Modal gasto D / comida D1
                │       └── cerrar_caso.fxml          # Modal confirmar cierre
                ├── viviendas/
                │   ├── vivienda.fxml         # Hecho: listado
                │   ├── vivienda_detalle.fxml # Hecho: vivienda y hogares
                │   └── partials/
                │       ├── vivienda_form.fxml        # Modal vivienda
                │       └── hogar_form.fxml           # Modal hogar
                ├── usuarios/
                │   ├── usuario.fxml          # Hecho: listado
                │   └── usuario_form.fxml     # Hecho: formulario
                └── roles/
                    ├── rol.fxml              # Hecho: matriz de permisos roles × módulos
                    └── partials/
                        └── rol_form.fxml             # Modal nuevo/renombrar rol
```

## Acceso a datos

- **Una sola conexión.** La aplicación es de escritorio y la usa una persona a la vez, así que `ConexionBD` mantiene una única conexión abierta durante la sesión y la vuelve a abrir si se cae. Abrir una conexión por cada operación agota los procesos de Oracle XE (error `ORA-12519`). `App.stop()` la cierra al salir.
- **Lecturas y escrituras.** Los servicios usan `ConexionBD.consultar(c -> ...)` para leer y `ConexionBD.enTransaccion(c -> ...)` para escribir. Esta última hace commit si todo sale bien y rollback si se lanza cualquier excepción.
- **Los DAO reciben la conexión** en cada método. Así un servicio puede agrupar varias operaciones en una misma transacción, por ejemplo crear un usuario y su encuestador.
- **Ids:** cada `insertar` toma el id de `seq_<tabla>.NEXTVAL`. Si la entidad ya trae id, lo respeta; los catálogos lo usan para guardar el código del cuadernillo.
- **Conversión de tipos:** `LocalDate` ↔ `DATE`, `LocalDateTime` ↔ `TIMESTAMP`, `boolean` ↔ `CHAR(1)` `'S'`/`'N'` y enum ↔ `VARCHAR2` con el nombre del enum.
- **Configuración:** `db.properties`, que no se versiona y se crea copiando `db.properties.example` (por defecto `jdbc:oracle:thin:@localhost:1522:XE`, usuario `enph`). Cada valor se puede sobrescribir al ejecutar con `-Ddb.url=...`, `-Ddb.usuario=...` o `-Ddb.contrasena=...`.

## Vistas por módulo

Los controladores Java están todos en el paquete `controller`, pero sus vistas se agrupan por módulo en `resources/com/udi/ui`:

- `ui/modulos/<modulo>/`: las pantallas de cada módulo (por ejemplo `encuestas/encuesta.fxml` y `encuestas/encuesta_detalle.fxml`). Si un módulo necesita diálogos modales o fragmentos reutilizables, van en subcarpetas `partials/` y `components/` dentro del módulo.
- `ui/shared/`: lo común (`estilos.css`, la estructura `layout/navbar.fxml` y `layout/pendiente.fxml`).

Una vista se nombra con su ruta dentro de `ui`, sin extensión: `"modulos/encuestas/encuesta"`. Para agregar un módulo se crea su carpeta en `ui/modulos/`, su controlador en `controller/` y su botón en `navbar.fxml`/`NavbarController`.

## Navegación

- `App.setRoot("<vista>")` cambia toda la ventana (login ↔ menú), por ejemplo `App.setRoot("shared/layout/navbar")`.
- Dentro del menú, `NavbarController.abrir("<vista>", migas...)` carga `ui/<vista>.fxml` en el área de contenido, pone el título y la ruta del encabezado y devuelve el controlador para pasarle datos (por ejemplo `formulario.editar(id)`).
- **Middlewares de acceso** (`middleware/`). Igual que una API registra `router.get('/chats', isAuth, ModulePermissionMiddleware, controller)`, `Rutas` asigna a cada prefijo de vista una cadena de middlewares, y `App.cargar` la ejecuta con `Rutas.exigir(vista)` antes de cargar cualquier pantalla, pestaña, formulario o modal:

  | Ruta | Cadena |
  |---|---|
  | `modulos/auth/` | (pública) |
  | `modulos/usuarios/`, `modulos/roles/`, `modulos/viviendas/`, `modulos/encuestas/` | `AutenticacionMiddleware` → `PermisoModuloMiddleware` |
  | cualquier otra (`shared/layout/navbar`…) | `AutenticacionMiddleware` |

  `PermisoModuloMiddleware` deja pasar al ADMIN y, para los demás roles, busca los slugs de la vista en `RutasModulo` (gana el prefijo más largo) y exige que el rol tenga alguno en `rol_modulo`:

  | Vista | Slugs |
  |---|---|
  | `modulos/encuestas/` (lista, detalle, Cap. A, cierre, nueva encuesta) | encuestas, visitas, caracteristicas-gasto, gastos-diarios, comidas-fuera-hogar |
  | `encuestas/partials/encuesta_visitas` · `encuesta_capitulo_c` · `encuesta_gastos` | encuestas o el slug de su captura |
  | `encuestas/partials/visita_form` · `renglon_form` | visitas · gastos-diarios o comidas-fuera-hogar |
  | `modulos/viviendas/` · `viviendas/partials/vivienda_form` · `hogar_form` | viviendas u hogares · viviendas · hogares |
  | `modulos/usuarios/` · `modulos/roles/` | usuarios · roles |

  Así el crítico-digitador (solo «encuestas») ve las pestañas de la encuesta en solo lectura y el encuestador las diligencia.
- Los controladores no validan roles: muestran cada botón o pestaña con `Rutas.permitir(nodo, vista)` (los mismos middlewares de la vista que abre) y, para las ediciones en línea de las pestañas, `EncuestaDetalleController.puedeEditar(RutasModulo.X)`. Los servicios repiten la validación con `Sesion.exigirModulo(RutasModulo.X)`, como una API que valida aunque el frontend oculte el botón.
- Los permisos se cargan en `Sesion` al iniciar sesión; los cambios de la pantalla Roles y permisos aplican en el siguiente inicio de sesión (salvo el rol propio, que se recarga al guardar).
- Los modales se abren con `Modal.cargar("<vista>", titulo)`: se prepara su controlador (`modal.controlador()...`) y se muestra con `modal.mostrar()`.
- El detalle de una encuesta se abre con `EncuestaDetalleController.abrir(id, Pestana.X)`. Cada pestaña es una vista de `encuestas/partials/` cuyo controlador implementa `SeccionEncuesta`.
- Los controladores llaman a los servicios con `Tareas.ejecutar(...)` para no congelar la ventana. Las tareas se ejecutan una a la vez porque la conexión es única, y los errores se muestran con `Alertas`.

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
| Cada escritura exige el módulo de su pantalla (`Sesion.exigirModulo`, el ADMIN siempre puede); catálogos, ubicación y módulos solo los cambia un administrador | Todos los servicios; `UbicacionService`, `CatalogoService`, `RolService` |
| El rol ADMIN no se edita, renombra ni elimina; solo un administrador asigna el rol ADMIN o modifica a otro administrador; no se elimina un rol con usuarios | `RolService`, `UsuarioService` |
| Si no fue compra, el lugar y la frecuencia van vacíos; si fue compra, la frecuencia es obligatoria; `NUNCA` solo en el capítulo C; "Otra" exige el ¿cuál?; la equivalencia solo con unidad OTRA | `GastoDiarioService` |
| C.3.1 y C.3.2 solo cuando la frecuencia supera la bisemana | `CaracteristicaService` |
| El número del hogar va de 1 al total de hogares de la vivienda | `HogarService` |
