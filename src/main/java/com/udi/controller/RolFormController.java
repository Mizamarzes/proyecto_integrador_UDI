package com.udi.controller;

import com.udi.model.Rol;
import com.udi.service.RolService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/** Modal para crear un rol o cambiarle el nombre. Los permisos se asignan en la matriz. */
public class RolFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtNombre;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    private final RolService rolService = new RolService();

    private Rol rol;
    private Consumer<Rol> alGuardar;

    @FXML
    private void initialize() {
        Formato.mostrar(lblError, false);
    }

    /** {@code existente} null crea un rol nuevo. */
    public void preparar(Rol existente, Consumer<Rol> alGuardar) {
        this.alGuardar = alGuardar;
        if (existente == null) {
            rol = new Rol();
        } else {
            rol = new Rol();
            rol.setId(existente.getId());
            rol.setNombre(existente.getNombre());
            txtNombre.setText(existente.getNombre());
            lblTitulo.setText("Renombrar rol");
            lblSubtitulo.setText("Los usuarios y permisos del rol se conservan.");
            btnGuardar.setText("Guardar cambios");
        }
    }

    @FXML
    private void guardar() {
        String nombre = Formato.texto(txtNombre.getText());
        if (nombre == null) {
            lblError.setText("Escriba el nombre del rol.");
            Formato.mostrar(lblError, true);
            return;
        }
        rol.setNombre(nombre);
        boolean esNuevo = rol.getId() == null;
        Formato.mostrar(lblError, false);
        Tareas.ejecutar(() -> esNuevo ? rolService.crearRol(rol) : rolService.actualizarRol(rol), guardado -> {
            Modal.cerrar(btnGuardar);
            alGuardar.accept(guardado);
        });
    }

    @FXML
    private void cancelar() {
        Modal.cerrar(btnGuardar);
    }
}
