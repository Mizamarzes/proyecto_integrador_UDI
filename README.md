# Proyecto Integrador UDI

Aplicación de escritorio desarrollada en Java con JavaFX como proyecto integrador
de la Universidad de Investigación y Desarrollo (UDI).

## Requisitos

- JDK 11 o superior
- Apache Maven 3.6+

## Tecnologías

- Java 11
- JavaFX 13 (`javafx-controls`, `javafx-fxml`)
- Maven

## Ejecución

```bash
mvn clean javafx:run
```

## Compilación

```bash
mvn clean package
```

## Estructura del proyecto

```
proyectointegrador/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── com/udi/
    │       ├── App.java                  # Punto de entrada y navegación (setRoot)
    │       ├── controller/               # Un controlador por pantalla
    │       ├── service/ dao/ model/      # Reglas de negocio, acceso a datos y entidades
    │       └── util/ config/ exception/
    └── resources/com/udi/
        ├── ui/
        │   ├── modulos/                  # Pantallas FXML por módulo (auth, inicio, encuestas…)
        │   └── shared/                   # estilos.css y la estructura principal (menu.fxml)
        └── config/db.properties          # Conexión a Oracle
```

La estructura completa y las reglas de cada capa están en `docs/arquitectura/arquitectura.md`.

## Descripción

La clase `App` abre la pantalla de inicio de sesión (`ui/modulos/auth/login.fxml`) y
cambia de pantalla con `App.setRoot("<vista>")`, que carga `ui/<vista>.fxml`
(por ejemplo `App.setRoot("shared/layout/menu")`).
