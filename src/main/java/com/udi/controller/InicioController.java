package com.udi.controller;

import com.udi.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Pantalla de bienvenida. */
public class InicioController {

    @FXML
    private Label lblBienvenida;

    @FXML
    private void initialize() {
        lblBienvenida.setText("Bienvenido(a), " + Sesion.getUsuario().getNombre());
    }
}
