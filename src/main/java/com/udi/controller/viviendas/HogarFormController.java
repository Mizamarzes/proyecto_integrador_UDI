package com.udi.controller.viviendas;

import com.udi.model.Hogar;
import com.udi.model.Vivienda;
import com.udi.service.viviendas.HogarService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modal para agregar o editar un hogar de una vivienda. El número del hogar
 * (A.13) no se pide: al crear lo asigna {@code HogarService} (el siguiente de
 * la vivienda) y al editar no cambia.
 */
public class HogarFormController {

    /** hogar_numero es NUMBER(2). */
    static final int MAXIMO_HOGARES = 99;

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtPersonas;
    @FXML
    private TextField txtCiclo;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    private final HogarService hogarService = new HogarService();

    private Hogar hogar;
    private Consumer<Hogar> alGuardar;

    @FXML
    private void initialize() {
        Formato.soloEnteros(txtPersonas, 2);
        txtCiclo.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().length() <= 10 ? c : null));
        Formato.mostrar(lblError, false);
    }

    /** {@code existente} es null para agregar un hogar nuevo. */
    public void preparar(Vivienda vivienda, List<Hogar> registrados, Hogar existente, Consumer<Hogar> alGuardar) {
        this.alGuardar = alGuardar;
        if (existente == null) {
            hogar = new Hogar();
            hogar.setViviendaId(vivienda.getId());
            // Solo informativo: el número definitivo lo asigna HogarService al guardar
            int siguiente = registrados.stream().mapToInt(Hogar::getHogarNumero).max().orElse(0) + 1;
            lblTitulo.setText("Agregar hogar " + siguiente);
        } else {
            hogar = existente;
            lblTitulo.setText("Editar hogar " + existente.getHogarNumero() + " · " + Formato.codigo("H", existente.getId()));
            txtPersonas.setText(String.valueOf(existente.getTotalPersonas()));
            txtCiclo.setText(existente.getCiclo());
            btnGuardar.setText("Guardar cambios");
        }
        lblSubtitulo.setText("Vivienda " + Formato.codigo("V", vivienda.getId()) + " · " + vivienda.getDireccion()
                + " · " + (registrados.size() == 1 ? "1 hogar registrado" : registrados.size() + " hogares registrados"));
    }

    @FXML
    private void guardar() {
        String personas = Formato.texto(txtPersonas.getText());
        int total = personas == null ? 0 : Integer.parseInt(personas);
        if (total < 1) {
            error("Indique cuántas personas conforman el hogar (1 a 99).");
            return;
        }
        hogar.setTotalPersonas(total);
        hogar.setCiclo(Formato.texto(txtCiclo.getText()));
        boolean esNuevo = hogar.getId() == null;
        Formato.mostrar(lblError, false);
        Tareas.ejecutar(() -> esNuevo ? hogarService.crearHogar(hogar) : hogarService.actualizarHogar(hogar), guardado -> {
            Modal.cerrar(btnGuardar);
            alGuardar.accept(guardado);
        });
    }

    @FXML
    private void cancelar() {
        Modal.cerrar(btnGuardar);
    }

    private void error(String mensaje) {
        lblError.setText(mensaje);
        Formato.mostrar(lblError, true);
    }
}
