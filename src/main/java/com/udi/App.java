package com.udi;

import com.udi.config.ConexionBD;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Punto de entrada de la aplicación. Mantiene una única escena y cambia la
 * pantalla con {@link #setRoot(String)}.
 *
 * Las vistas están en resources/com/udi/ui, organizadas por módulo
 * (ui/modulos/encuestas/encuesta.fxml) y lo común en ui/shared. Se nombran con
 * su ruta dentro de ui y sin extensión, por ejemplo "modulos/encuestas/encuesta".
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) {
        scene = new Scene(loadFXML("modulos/auth/login"), 1100, 680);
        scene.getStylesheets().add(App.class.getResource("ui/shared/estilos.css").toExternalForm());
        stage.setTitle("ENPH · Cuadernillo 2 urbano");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        ConexionBD.cerrar();
    }

    /** Muestra la pantalla {@code ui/<vista>.fxml} en toda la ventana. */
    public static void setRoot(String vista) {
        scene.setRoot(loadFXML(vista));
    }

    private static Parent loadFXML(String vista) {
        return cargar(vista).getRoot();
    }

    /**
     * Carga {@code ui/<vista>.fxml} y devuelve el loader, del que se obtiene
     * la raíz y el controlador. Lo usa el menú para mostrar los módulos.
     */
    public static FXMLLoader cargar(String vista) {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("ui/" + vista + ".fxml"));
        try {
            loader.load();
            return loader;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + vista, e);
        }
    }

    public static void main(String[] args) {
        launch();
    }

}
