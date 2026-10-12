package com.udi.controller;

import com.udi.model.Rol;
import com.udi.model.Usuario;
import com.udi.service.UsuarioService;
import com.udi.util.Formato;
import com.udi.util.Tareas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.Map;

/** Listado de usuarios con búsqueda y filtro por rol (RF-01). */
public class UsuarioController {

    private static final String TODOS = "Todos";

    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<String> cmbRol;
    @FXML
    private TableView<Usuario> tabla;
    @FXML
    private TableColumn<Usuario, String> colNombre;
    @FXML
    private TableColumn<Usuario, String> colCorreo;
    @FXML
    private TableColumn<Usuario, String> colRol;
    @FXML
    private TableColumn<Usuario, Boolean> colEstado;
    @FXML
    private TableColumn<Usuario, Usuario> colAcciones;
    @FXML
    private Label lblTotal;

    private final UsuarioService usuarioService = new UsuarioService();
    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> filtrados = new FilteredList<>(usuarios);
    /** Nombre visible de cada rol por su id. */
    private final Map<Long, String> roles = new HashMap<>();

    @FXML
    private void initialize() {
        configurarTabla();
        txtBuscar.textProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbRol.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbRol.getItems().add(TODOS);
        cmbRol.setValue(TODOS);

        // Las tareas se ejecutan en orden: los roles quedan cargados antes que los usuarios
        Tareas.ejecutar(() -> usuarioService.listarRoles(), lista -> {
            for (Rol rol : lista) {
                roles.put(rol.getId(), Formato.nombreRol(rol.getNombre()));
                cmbRol.getItems().add(Formato.nombreRol(rol.getNombre()));
            }
        });
        Tareas.ejecutar(() -> usuarioService.listarUsuarios(), lista -> usuarios.setAll(lista));
    }

    @FXML
    private void limpiar() {
        txtBuscar.clear();
        cmbRol.setValue(TODOS);
    }

    @FXML
    private void nuevoUsuario() {
        UsuarioFormController formulario = NavbarController.abrir("modulos/usuarios/usuario_form", "Usuarios", "Nuevo usuario");
        formulario.nuevo();
    }

    private void abrir(Usuario usuario) {
        UsuarioFormController formulario = NavbarController.abrir("modulos/usuarios/usuario_form", "Usuarios",
                usuario.getNombreCompleto());
        formulario.editar(usuario.getId());
    }

    private void configurarTabla() {
        tabla.setItems(filtrados);
        tabla.setPlaceholder(new Label("No hay usuarios que coincidan con la búsqueda."));
        // Ancho de cada columna como porcentaje de la tabla (el correo necesita más espacio)
        anchoRelativo(colNombre, 0.24);
        anchoRelativo(colCorreo, 0.32);
        anchoRelativo(colRol, 0.16);
        anchoRelativo(colEstado, 0.13);
        anchoRelativo(colAcciones, 0.13);

        colNombre.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNombreCompleto()));
        colCorreo.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        colRol.setCellValueFactory(d -> new ReadOnlyStringWrapper(roles.getOrDefault(d.getValue().getRolId(), "")));
        colEstado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().isActivo()));
        colAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean activo, boolean vacia) {
                super.updateItem(activo, vacia);
                if (vacia || activo == null) {
                    setGraphic(null);
                    return;
                }
                Label etiqueta = new Label(activo ? "Activo" : "Inactivo");
                etiqueta.getStyleClass().addAll("insignia", activo ? "insignia-activo" : "insignia-inactivo");
                setGraphic(etiqueta);
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink enlace = new Hyperlink("Editar");

            {
                enlace.getStyleClass().add("enlace");
                enlace.setOnAction(e -> abrir(getItem()));
            }

            @Override
            protected void updateItem(Usuario usuario, boolean vacia) {
                super.updateItem(usuario, vacia);
                setGraphic(vacia || usuario == null ? null : enlace);
            }
        });

        tabla.setRowFactory(t -> {
            TableRow<Usuario> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    abrir(fila.getItem());
                }
            });
            return fila;
        });

        filtrados.addListener((ListChangeListener<Usuario>) c -> actualizarTotal());
    }

    private void anchoRelativo(TableColumn<Usuario, ?> columna, double porcentaje) {
        columna.prefWidthProperty().bind(tabla.widthProperty().multiply(porcentaje));
    }

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String rol = cmbRol.getValue();
        filtrados.setPredicate(u -> {
            boolean coincideTexto = texto.isEmpty()
                    || u.getNombreCompleto().toLowerCase().contains(texto)
                    || u.getEmail().toLowerCase().contains(texto);
            boolean coincideRol = rol == null || TODOS.equals(rol) || rol.equals(roles.get(u.getRolId()));
            return coincideTexto && coincideRol;
        });
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(filtrados.size() + " de " + usuarios.size() + " usuarios");
    }
}
