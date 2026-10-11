module com.udi {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    // Driver de Oracle (módulo automático, toma el nombre del jar ojdbc6-11.2.0.4.jar)
    requires ojdbc6;

    opens com.udi to javafx.fxml;
    exports com.udi;
}
