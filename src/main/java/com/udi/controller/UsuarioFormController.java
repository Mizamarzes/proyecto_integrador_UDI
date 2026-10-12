package com.udi.controller;

import com.udi.exception.ReglaNegocioException;
import com.udi.model.Rol;
import com.udi.model.Usuario;
import com.udi.service.UsuarioService;
import com.udi.util.Alertas;
import com.udi.util.Formato;
import com.udi.util.Sesion;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Formulario de un usuario (RF-01): datos personales, rol y contraseña.
 * Permite activar o desactivar el acceso y eliminar el usuario.
 */
public class UsuarioFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblDetalle;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtApellido;
    @FXML
    private TextField txtCorreo;
    @FXML
    private TextField txtTelefono;
    @FXML
    private ComboBox<Rol> cmbRol;
    @FXML
    private Label lblContrasena;
    @FXML
    private PasswordField txtContrasena;
    @FXML
    private PasswordField txtConfirmacion;
    @FXML
    private Label lblAyudaContrasena;
    @FXML
    private Button btnActivo;
    @FXML
    private Button btnEliminar;

    private final UsuarioService usuarioService = new UsuarioService();

    /** Usuario que se edita; null si es nuevo. */
    private Usuario usuario;

    @FXML
    private void initialize() {
        txtTelefono.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("[0-9 +()-]{0,20}") ? c : null));
        cmbRol.setConverter(new StringConverter<>() {
            @Override
            public String toString(Rol rol) {
                return rol == null ? "" : Formato.nombreRol(rol.getNombre());
            }

            @Override
            public Rol fromString(String texto) {
                return null;
            }
        });
        mostrar(lblEstado, false);
        mostrar(lblDetalle, false);
    }

    /** Prepara el formulario para registrar un usuario nuevo. */
    public void nuevo() {
        usuario = null;
        lblTitulo.setText("Nuevo usuario");
        lblContrasena.setText("Contraseña *");
        lblAyudaContrasena.setText("Mínimo 6 caracteres. Si el rol es Encuestador, se registra también como encuestador.");
        actualizarBotones();
        Tareas.ejecutar(() -> usuarioService.listarRoles(), roles -> cmbRol.getItems().setAll(roles));
    }

    /** Carga el usuario {@code id} para editarlo. */
    public void editar(long id) {
        Tareas.ejecutar(() -> usuarioService.listarRoles(), roles -> cmbRol.getItems().setAll(roles));
        Tareas.ejecutar(() -> usuarioService.buscarUsuario(id)
                .orElseThrow(() -> new ReglaNegocioException("No existe el usuario " + id + ".")), this::mostrarUsuario);
    }

    @FXML
    private void guardar() {
        String contrasena = txtContrasena.getText();
        if (!contrasena.equals(txtConfirmacion.getText())) {
            Alertas.error("Contraseña", "La contraseña y su confirmación no coinciden.");
            return;
        }
        Usuario datos = leerFormulario();
        boolean esNuevo = usuario == null;
        boolean cambiaContrasena = !esNuevo && !contrasena.isEmpty();
        Tareas.ejecutar(() -> {
            if (esNuevo) {
                return usuarioService.crearUsuario(datos, contrasena);
            }
            Usuario guardado = usuarioService.actualizarUsuario(datos);
            if (cambiaContrasena) {
                usuarioService.cambiarContrasena(guardado.getId(), contrasena);
            }
            return guardado;
        }, guardado -> {
            Alertas.info("Usuario guardado", "El usuario " + guardado.getNombreCompleto()
                    + (esNuevo ? " se registró correctamente." : " se actualizó correctamente."));
            volver();
        });
    }

    @FXML
    private void cambiarActivo() {
        boolean activar = !usuario.isActivo();
        String pregunta = activar
                ? "¿Desea activar el usuario? Podrá volver a iniciar sesión."
                : "¿Desea desactivar el usuario? No podrá iniciar sesión, pero sus registros se conservan.";
        if (Alertas.confirmar(activar ? "Activar usuario" : "Desactivar usuario", pregunta)) {
            long id = usuario.getId();
            Tareas.accion(() -> usuarioService.cambiarActivo(id, activar), () -> editar(id));
        }
    }

    @FXML
    private void eliminar() {
        if (Alertas.confirmar("Eliminar usuario", "Se eliminará el usuario " + usuario.getNombreCompleto()
                + ". Si ya registró información no se podrá eliminar; en ese caso desactívelo.\n\n¿Desea continuar?")) {
            long id = usuario.getId();
            Tareas.accion(() -> usuarioService.eliminarUsuario(id), this::volver);
        }
    }

    @FXML
    private void volver() {
        NavbarController.abrir("modulos/usuarios/usuario", "Usuarios");
    }

    private void mostrarUsuario(Usuario u) {
        usuario = u;
        lblTitulo.setText(u.getNombreCompleto());
        lblEstado.setText(u.isActivo() ? "Activo" : "Inactivo");
        lblEstado.getStyleClass().removeIf(c -> c.startsWith("insignia-"));
        lblEstado.getStyleClass().add(u.isActivo() ? "insignia-activo" : "insignia-inactivo");
        mostrar(lblEstado, true);
        lblDetalle.setText(u.getUltimoAcceso() == null ? "Todavía no ha iniciado sesión."
                : "Último acceso: " + Formato.fechaHora(u.getUltimoAcceso()) + ".");
        mostrar(lblDetalle, true);

        txtNombre.setText(u.getNombre());
        txtApellido.setText(u.getApellido());
        txtCorreo.setText(u.getEmail());
        txtTelefono.setText(u.getTelefono());
        seleccionarRol(u.getRolId());
        lblContrasena.setText("Nueva contraseña");
        lblAyudaContrasena.setText("Déjela vacía para conservar la contraseña actual.");
        actualizarBotones();
    }

    private Usuario leerFormulario() {
        Usuario u = new Usuario();
        if (usuario != null) {
            u.setId(usuario.getId());
            // El estado se cambia con el botón Activar / Desactivar
            u.setActivo(usuario.isActivo());
        } else {
            u.setActivo(true);
        }
        u.setNombre(texto(txtNombre));
        u.setApellido(texto(txtApellido));
        u.setEmail(texto(txtCorreo));
        u.setTelefono(texto(txtTelefono));
        u.setRolId(cmbRol.getValue() == null ? null : cmbRol.getValue().getId());
        return u;
    }

    private void actualizarBotones() {
        boolean existe = usuario != null;
        // Un administrador no puede desactivarse ni eliminarse a sí mismo
        boolean esPropio = existe && usuario.getId().equals(Sesion.idUsuario());
        mostrar(btnActivo, existe && !esPropio);
        mostrar(btnEliminar, existe && !esPropio);
        if (existe) {
            btnActivo.setText(usuario.isActivo() ? "Desactivar" : "Activar");
        }
    }

    /** Selecciona el rol cuando la lista ya está cargada (las tareas se ejecutan en orden). */
    private void seleccionarRol(Long rolId) {
        List<Rol> roles = cmbRol.getItems();
        roles.stream().filter(r -> r.getId().equals(rolId)).findFirst().ifPresent(cmbRol::setValue);
    }

    private static String texto(TextField campo) {
        String valor = campo.getText();
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    private static void mostrar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }
}
