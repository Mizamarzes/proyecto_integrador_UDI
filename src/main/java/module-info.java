module com.udi {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    // Preferencias del usuario (Recordarme en el login)
    requires java.prefs;
    // Driver de Oracle (módulo automático, toma el nombre del jar ojdbc6-11.2.0.4.jar)
    requires ojdbc6;

    opens com.udi to javafx.fxml;
    // Controladores de las vistas FXML, uno por módulo
    opens com.udi.controller.auth to javafx.fxml;
    opens com.udi.controller.encuestas to javafx.fxml;
    opens com.udi.controller.roles to javafx.fxml;
    opens com.udi.controller.shared to javafx.fxml;
    opens com.udi.controller.usuarios to javafx.fxml;
    opens com.udi.controller.viviendas to javafx.fxml;
    exports com.udi;
}
