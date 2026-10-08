module com.udi {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.udi to javafx.fxml;
    exports com.udi;
}
