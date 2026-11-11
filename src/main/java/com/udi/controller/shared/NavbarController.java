package com.udi.controller.shared;

import com.udi.App;
import com.udi.middleware.Rutas;
import com.udi.service.auth.AuthService;
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
    private static final String VISTA_ENCUESTAS = "modulos/encuestas/encuesta";
    private static final String VISTA_VIVIENDAS = "modulos/viviendas/vivienda";
    private static final String VISTA_USUARIOS = "modulos/usuarios/usuario";
    private static final String VISTA_ROLES = "modulos/roles/rol";
    private static final String VISTA_REPORTES = "modulos/reportes/reporte";

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
    private Button btnViviendas;
    @FXML
    private Button btnUsuarios;
    @FXML
    private Button btnRoles;
    @FXML
    private Button btnReportes;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        actual = this;
        lblUsuario.setText(Sesion.getUsuario().getNombreCompleto());
        lblRol.setText(Formato.nombreRol(Sesion.getRol().getNombre()));
        // Cada botón se muestra con los mismos middlewares que protegen su vista (middleware/Rutas)
        Rutas.permitir(btnEncuestas, VISTA_ENCUESTAS);
        Rutas.permitir(btnViviendas, VISTA_VIVIENDAS);
        Rutas.permitir(btnUsuarios, VISTA_USUARIOS);
        Rutas.permitir(btnRoles, VISTA_ROLES);
        Rutas.permitir(btnReportes, VISTA_REPORTES);
        if (btnEncuestas.isVisible()) {
            irEncuestas();
        } else if (btnViviendas.isVisible()) {
            irViviendas();
        } else if (btnUsuarios.isVisible()) {
            irUsuarios();
        } else if (btnRoles.isVisible()) {
            irRoles();
        } else if (btnReportes.isVisible()) {
            irReportes();
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
        migas(migas);
        return loader.getController();
    }

    /** Cambia el título y la ruta del encabezado sin cambiar la pantalla (pestañas de la encuesta). */
    public static void migas(String... migas) {
        actual.lblTitulo.setText(migas[migas.length - 1]);
        actual.lblMigas.setText("ENPH / " + String.join(" / ", migas));
    }

    @FXML
    private void irEncuestas() {
        navegar(btnEncuestas, VISTA_ENCUESTAS, "Encuestas");
    }

    @FXML
    private void irViviendas() {
        navegar(btnViviendas, VISTA_VIVIENDAS, "Viviendas y hogares");
    }

    @FXML
    private void irUsuarios() {
        navegar(btnUsuarios, VISTA_USUARIOS, "Usuarios");
    }

    @FXML
    private void irRoles() {
        navegar(btnRoles, VISTA_ROLES, "Roles y permisos");
    }

    @FXML
    private void irReportes() {
        navegar(btnReportes, VISTA_REPORTES, "Reportes");
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
        marcar(boton);
        abrir(vista, titulo);
    }

    /** Marca en el menú el módulo de la pantalla que se abre desde otra (por ejemplo, la vivienda desde una encuesta). */
    public static void marcarModulo(String slug) {
        Button boton = "viviendas".equals(slug) ? actual.btnViviendas
                : "usuarios".equals(slug) ? actual.btnUsuarios
                : "roles".equals(slug) ? actual.btnRoles
                : "reportes".equals(slug) ? actual.btnReportes : actual.btnEncuestas;
        actual.marcar(boton);
    }

    private void marcar(Button boton) {
        for (Button b : List.of(btnEncuestas, btnViviendas, btnUsuarios, btnRoles, btnReportes)) {
            b.getStyleClass().remove(ACTIVO);
        }
        boton.getStyleClass().add(ACTIVO);
    }

    /** El rol no tiene ninguno de los módulos del menú. */
    private void sinModulos() {
        Label aviso = new Label("Su rol no tiene módulos asignados. Comuníquese con el administrador.");
        aviso.getStyleClass().add("subtitulo-pagina");
        contenido.getChildren().setAll(aviso);
        lblTitulo.setText("Sin módulos");
        lblMigas.setText("ENPH");
    }
}
