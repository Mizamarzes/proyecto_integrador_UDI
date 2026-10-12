package com.udi.controller;

import com.udi.App;
import com.udi.exception.DatosException;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.Usuario;
import com.udi.service.AuthService;
import com.udi.util.Alertas;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.prefs.Preferences;

public class LoginController {

    private static final String PREF_CORREO = "login.correo";
    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    private TextField txtCorreo;
    @FXML
    private PasswordField txtContrasena;
    @FXML
    private CheckBox chkRecordarme;
    @FXML
    private Label lblError;
    @FXML
    private Button btnIngresar;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        String correo = PREFS.get(PREF_CORREO, "");
        txtCorreo.setText(correo);
        chkRecordarme.setSelected(!correo.isEmpty());
        ocultarError();
        txtCorreo.textProperty().addListener((obs, antes, ahora) -> ocultarError());
        txtContrasena.textProperty().addListener((obs, antes, ahora) -> ocultarError());
        Platform.runLater(() -> (correo.isEmpty() ? txtCorreo : txtContrasena).requestFocus());
    }

    @FXML
    private void ingresar() {
        String correo = txtCorreo.getText();
        String contrasena = txtContrasena.getText();

        Task<Usuario> tarea = new Task<>() {
            @Override
            protected Usuario call() {
                return authService.iniciarSesion(correo, contrasena);
            }
        };
        tarea.setOnSucceeded(e -> {
            recordarCorreo(correo);
            App.setRoot("shared/layout/navbar");
        });
        tarea.setOnFailed(e -> {
            ocupado(false);
            Throwable error = tarea.getException();
            if (error instanceof ReglaNegocioException) {
                mostrarError(error.getMessage());
                txtContrasena.clear();
                txtContrasena.requestFocus();
            } else if (error instanceof DatosException) {
                Alertas.error("Error de conexión",
                        "No fue posible conectarse con la base de datos.\n" + error.getMessage());
            } else {
                Alertas.error("Error inesperado", String.valueOf(error));
            }
        });

        ocupado(true);
        Thread hilo = new Thread(tarea, "login");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void olvidoContrasena() {
        Alertas.info("Recuperar contraseña",
                "Comuníquese con el administrador del sistema para restablecer su contraseña.");
    }

    private void recordarCorreo(String correo) {
        if (chkRecordarme.isSelected()) {
            PREFS.put(PREF_CORREO, correo.trim());
        } else {
            PREFS.remove(PREF_CORREO);
        }
    }

    private void ocupado(boolean ocupado) {
        btnIngresar.setDisable(ocupado);
        btnIngresar.setText(ocupado ? "Ingresando…" : "Ingresar");
        txtCorreo.setDisable(ocupado);
        txtContrasena.setDisable(ocupado);
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
