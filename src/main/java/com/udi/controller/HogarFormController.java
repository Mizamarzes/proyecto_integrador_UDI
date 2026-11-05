package com.udi.controller;

import com.udi.model.Hogar;
import com.udi.model.Vivienda;
import com.udi.service.HogarService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Modal para agregar o editar un hogar de una vivienda. El número del hogar
 * (A.13) solo ofrece los números libres, del 1 al total de hogares de la vivienda.
 */
public class HogarFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private ComboBox<Integer> cmbNumero;
    @FXML
    private Label lblNotaNumero;
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
        Set<Integer> ocupados = new HashSet<>();
        for (Hogar h : registrados) {
            ocupados.add(h.getHogarNumero());
        }
        if (existente == null) {
            hogar = new Hogar();
            hogar.setViviendaId(vivienda.getId());
            lblTitulo.setText("Agregar hogar");
        } else {
            hogar = existente;
            ocupados.remove(existente.getHogarNumero());
            lblTitulo.setText("Editar hogar " + Formato.codigo("H", existente.getId()));
            txtPersonas.setText(String.valueOf(existente.getTotalPersonas()));
            txtCiclo.setText(existente.getCiclo());
            btnGuardar.setText("Guardar cambios");
        }
        for (int n = 1; n <= vivienda.getTotalHogares(); n++) {
            if (!ocupados.contains(n)) {
                cmbNumero.getItems().add(n);
            }
        }
        cmbNumero.setValue(existente == null
                ? (cmbNumero.getItems().isEmpty() ? null : cmbNumero.getItems().get(0))
                : existente.getHogarNumero());
        lblSubtitulo.setText("Vivienda " + Formato.codigo("V", vivienda.getId()) + " · " + vivienda.getDireccion()
                + " · " + registrados.size() + " de " + vivienda.getTotalHogares() + " hogares registrados");
        lblNotaNumero.setText("Solo números libres, del 1 al total de hogares de la vivienda (" + vivienda.getTotalHogares() + ").");
    }

    @FXML
    private void guardar() {
        String personas = Formato.texto(txtPersonas.getText());
        int total = personas == null ? 0 : Integer.parseInt(personas);
        if (cmbNumero.getValue() == null) {
            error("Seleccione el número del hogar.");
            return;
        }
        if (total < 1) {
            error("Indique cuántas personas conforman el hogar (1 a 99).");
            return;
        }
        hogar.setHogarNumero(cmbNumero.getValue());
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
