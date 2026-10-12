package com.udi.controller;

import com.udi.App;
import com.udi.service.AuthService;
import com.udi.util.Alertas;
import com.udi.util.Formato;
import com.udi.util.Sesion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * Estructura principal después del login: barra lateral con los módulos,
 * encabezado con el usuario y área de contenido donde se cargan las pantallas.
 */
public class NavbarController {

    private static final String ACTIVO = "activo";

    /** Menú visible; lo usan las pantallas para navegar con {@link #abrir}. */
    private static NavbarController actual;

    @FXML
    private StackPane contenido;
    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblMigas;
    @FXML
    private Label lblUsuario;
    @FXML
    private Label lblRol;
    @FXML
    private Button btnEncuestas;
    @FXML
    private Button btnUsuarios;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        actual = this;
        lblUsuario.setText(Sesion.getUsuario().getNombreCompleto());
        lblRol.setText(Formato.nombreRol(Sesion.getRol().getNombre()));
        permitir(btnEncuestas, "encuestas");
        permitir(btnUsuarios, "usuarios");
        if (btnEncuestas.isVisible()) {
            irEncuestas();
        } else if (btnUsuarios.isVisible()) {
            irUsuarios();
        } else {
            sinModulos();
        }
    }

    /**
     * Muestra {@code ui/<vista>.fxml} en el área de contenido y devuelve su
     * controlador para pasarle datos. Las migas forman el título y la ruta del
     * encabezado, por ejemplo ("Encuestas", "E-000003").
     */
    public static <T> T abrir(String vista, String... migas) {
        FXMLLoader loader = App.cargar(vista);
        actual.contenido.getChildren().setAll((Parent) loader.getRoot());
        actual.lblTitulo.setText(migas[migas.length - 1]);
        actual.lblMigas.setText("ENPH / " + String.join(" / ", migas));
        return loader.getController();
    }

    @FXML
    private void irEncuestas() {
        navegar(btnEncuestas, "modulos/encuestas/encuesta", "Encuestas");
    }

    @FXML
    private void irUsuarios() {
        navegar(btnUsuarios, "modulos/usuarios/usuario", "Usuarios");
    }

    @FXML
    private void cerrarSesion() {
        if (Alertas.confirmar("Cerrar sesión", "¿Desea cerrar la sesión?")) {
            authService.cerrarSesion();
            actual = null;
            App.setRoot("modulos/auth/login");
        }
    }

    private void navegar(Button boton, String vista, String titulo) {
        for (Button b : List.of(btnEncuestas, btnUsuarios)) {
            b.getStyleClass().remove(ACTIVO);
        }
        boton.getStyleClass().add(ACTIVO);
        abrir(vista, titulo);
    }

    /** El rol no tiene ninguno de los módulos del menú. */
    private void sinModulos() {
        Label aviso = new Label("Su rol no tiene módulos asignados. Comuníquese con el administrador.");
        aviso.getStyleClass().add("subtitulo-pagina");
        contenido.getChildren().setAll(aviso);
        lblTitulo.setText("Sin módulos");
        lblMigas.setText("ENPH");
    }

    /** El administrador ve todos los módulos; los demás roles, los que tienen asignados. */
    private static void permitir(Button boton, String slugModulo) {
        boolean visible = Sesion.esAdmin() || Sesion.puedeAcceder(slugModulo);
        boton.setVisible(visible);
        boton.setManaged(visible);
    }
}
