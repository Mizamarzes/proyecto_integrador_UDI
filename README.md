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
    │       ├── App.java                  # Clase principal (punto de entrada)
    │       ├── PrimaryController.java    # Controlador de la vista primaria
    │       └── SecondaryController.java  # Controlador de la vista secundaria
    └── resources/com/udi/
        ├── primary.fxml                  # Vista primaria
        └── secondary.fxml                # Vista secundaria
```

## Descripción

El proyecto parte de una plantilla JavaFX con dos vistas definidas en archivos
FXML. La clase `App` carga la vista inicial (`primary.fxml`) en una ventana de
640x480 y permite alternar entre vistas mediante el método `setRoot`.
