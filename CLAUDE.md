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

Currently the stock JavaFX Maven archetype (Java 11, JavaFX 13, `javafx-maven-plugin` 0.0.6) — no database, networking or domain code exists yet.

- Java Platform Module System: `src/main/java/module-info.java` declares module `com.udi`. Any new package that FXML controllers live in must be `opens ... to javafx.fxml`, and new dependencies (e.g. JDBC driver `java.sql`) must be added as `requires`, or runtime/compile will fail.
- Navigation: `App` holds a single static `Scene`; views are swapped by calling `App.setRoot("<name>")`, which loads `src/main/resources/com/udi/<name>.fxml` (resolved relative to the `App` class, so FXML must stay in the same package path as `App` unless `loadFXML` is changed).
- Each FXML binds its controller via `fx:controller` and handler methods via `onAction="#method"`; handlers are `@FXML private` methods in the controller.
