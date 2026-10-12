module com.udi {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    // Preferencias del usuario (Recordarme en el login)
    requires java.prefs;
    // Driver de Oracle (módulo automático, toma el nombre del jar ojdbc6-11.2.0.4.jar)
    requires ojdbc6;

    opens com.udi to javafx.fxml;
    opens com.udi.controller to javafx.fxml;
    exports com.udi;
}
