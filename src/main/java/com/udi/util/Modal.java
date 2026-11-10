package com.udi.util;

import com.udi.App;
import com.udi.controller.encuestas.VisitaFormController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Ventana modal con una vista FXML (los formularios de visita, gasto, vivienda…).
 * Se carga, se prepara el controlador y luego se muestra:
 *
 * <pre>
 * Modal&lt;VisitaFormController&gt; modal = Modal.cargar("modulos/encuestas/partials/visita_form", "Registrar visita");
 * modal.controlador().nueva(hogarId, this::recargar);
 * modal.mostrar();
 * </pre>
 *
 * El controlador cierra la ventana con {@link #cerrar(Node)}.
 */
public final class Modal<T> {

    private final Stage ventana;
    private final T controlador;

    private Modal(Stage ventana, T controlador) {
        this.ventana = ventana;
        this.controlador = controlador;
    }

    public static <T> Modal<T> cargar(String vista, String titulo) {
        FXMLLoader loader = App.cargar(vista);
        Scene escena = new Scene((Parent) loader.getRoot());
        escena.getStylesheets().add(App.estilos());
        Stage ventana = new Stage();
        ventana.initOwner(App.ventana());
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.setTitle(titulo);
        ventana.setScene(escena);
        return new Modal<>(ventana, loader.getController());
    }

    public T controlador() {
        return controlador;
    }

    /** Muestra la ventana y espera a que se cierre. */
    public void mostrar() {
        ventana.sizeToScene();
        ventana.showAndWait();
    }

    /** Cierra la ventana modal que contiene {@code nodo}. */
    public static void cerrar(Node nodo) {
        ((Stage) nodo.getScene().getWindow()).close();
    }
}
