package com.udi.controller.roles;

import com.udi.model.Rol;
import com.udi.service.roles.RolService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/** Modal para crear un rol. Los permisos se asignan en la matriz; los roles no se renombran. */
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

    private Consumer<Rol> alGuardar;

    @FXML
    private void initialize() {
        Formato.mostrar(lblError, false);
    }

    public void preparar(Consumer<Rol> alGuardar) {
        this.alGuardar = alGuardar;
    }

    @FXML
    private void guardar() {
        String nombre = Formato.texto(txtNombre.getText());
        if (nombre == null) {
            lblError.setText("Escriba el nombre del rol.");
            Formato.mostrar(lblError, true);
            return;
        }
        Rol rol = new Rol();
        rol.setNombre(nombre);
        Formato.mostrar(lblError, false);
        Tareas.ejecutar(() -> rolService.crearRol(rol), guardado -> {
            Modal.cerrar(btnGuardar);
            alGuardar.accept(guardado);
        });
    }

    @FXML
    private void cancelar() {
        Modal.cerrar(btnGuardar);
    }
}
