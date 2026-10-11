# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

University "Proyecto Integrador" (UDI, Ingeniería de Sistemas, 4th semester, II-2026) spanning three courses: Ingeniería de Software I, Bases de Datos II and Programación II. The goal is a desktop app to register and process data from **Cuadernillo 2 (urban daily household expenses)** of the DANE *Encuesta Nacional de Presupuestos de los Hogares (ENPH)*.

Mandated constraints from the assignment: object-oriented design, **client–server architecture**, and persistence in a **relational database (Oracle)**. The UI is JavaFX.

Reference material lives in `docs/db/` (PDFs, in Spanish):
- `Proyecto Integrador IV.pdf` — assignment statement, deliverables and deadlines.
- `Proyecto integrador IV semestre ... .pdf` — the team's design document: functional requirements (RF-01…, e.g. users/roles Encuestador/Supervisor/Admin, household registration with a 14-day follow-up window, daily expense capture, special acquisition forms such as pago/regalo/trueque, case closure locking further inserts after day 14, visit tracking), ER/relational model and data dictionary. Consult it before implementing features or schema.
- `Cuadernillo 2 urbano.pdf` — the official DANE form whose fields the app must capture.

Use `pdftotext` to read these. Code, UI text and docs are written in Spanish; keep that convention.

## Commands

Requires JDK 11+ and Maven 3.6+.

```bash
mvn clean javafx:run     # run the app (main class com.udi.App)
mvn clean package        # compile and package
mvn compile              # compile only
```

There are no tests or test dependencies yet.

## Architecture

Java 11, JavaFX 13, `javafx-maven-plugin` 0.0.6, Oracle 10g via JDBC (`ojdbc6` 11.2.0.4 — newer drivers cannot connect to a 10g server). MVC with the model split into layers; the full tree, layer rules and business rules are in `docs/arquitectura.md`. The backend (model, DAO, service) exists; controllers and FXML views are still pending (only the archetype's `primary`/`secondary` remain).

- Layers: `controller` → `service` (business rules, throws `ReglaNegocioException`) → `dao` (SQL only, one per table, extends `BaseDAO`) → `model` (entities extend `Entidad`/`EntidadAuditable`; enums in `model/enums` map to `VARCHAR2 + CHECK`). Controllers must never call DAOs.
- Database access: `config/ConexionBD` keeps a single shared connection (opening one per operation exhausts Oracle XE processes, ORA-12519). Services wrap reads in `ConexionBD.consultar(c -> ...)` and writes in `ConexionBD.enTransaccion(c -> ...)`; DAO methods take the `Connection` as first parameter. Connection settings live in `src/main/resources/com/udi/config/db.properties` and can be overridden with `-Ddb.url`, `-Ddb.usuario`, `-Ddb.contrasena`.
- The schema has no triggers: ids come from `seq_<tabla>.NEXTVAL` (assigned in each DAO's `insertar`), `updated_at = SYSTIMESTAMP` is set in each DAO's `actualizar`, and all cross-table rules (closed survey RF-06, personal expenses only days 1–7, day date = `fecha_desde + numero_dia - 1`, chapter C complete before daily records) are enforced in the services. `created_by` comes from `util/Sesion`.
- Schema source of truth: `docs/db/modelo_relacional.dbml` (documented in `modelo_relacional.md`); DDL and seed data in `docs/db/esquema_oracle10g.sql`.
- Java Platform Module System: `src/main/java/module-info.java` declares module `com.udi` and `requires java.sql` and `requires ojdbc6` (automatic module). Any new package that FXML controllers live in must be `opens ... to javafx.fxml` (and `opens com.udi.model to javafx.base` if `PropertyValueFactory` is used), and new dependencies must be added as `requires`, or runtime/compile will fail. Don't `opens` a package before it exists — compilation fails.
- Navigation: `App` holds a single static `Scene`; views are swapped by calling `App.setRoot("<name>")`, which loads `src/main/resources/com/udi/<name>.fxml` (resolved relative to the `App` class, so FXML must stay in the same package path as `App` unless `loadFXML` is changed).
- Each FXML binds its controller via `fx:controller` and handler methods via `onAction="#method"`; handlers are `@FXML private` methods in the controller.
