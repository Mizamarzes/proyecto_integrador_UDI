package com.udi.controller.viviendas;

import com.udi.componentes.SelectBuscable;
import com.udi.model.Departamento;
import com.udi.model.Municipio;
import com.udi.model.Region;
import com.udi.model.Vivienda;
import com.udi.service.viviendas.HogarService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Modal para registrar o editar una vivienda (capítulo A). */
public class ViviendaFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private SelectBuscable<Region> cmbRegion;
    @FXML
    private SelectBuscable<Departamento> cmbDepartamento;
    @FXML
    private SelectBuscable<Municipio> cmbMunicipio;
    @FXML
    private ComboBox<String> cmbClase;
    @FXML
    private TextField txtSector;
    @FXML
    private TextField txtSeccion;
    @FXML
    private TextField txtManzana;
    @FXML
    private TextField txtSegmento;
    @FXML
    private TextField txtEdificacion;
    @FXML
    private TextField txtViviendaNumero;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextField txtBarrio;
    @FXML
    private TextField txtDireccion;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    private final HogarService hogarService = new HogarService();

    private SelectoresUbicacion ubicacion;
    private Vivienda vivienda;
    private Consumer<Vivienda> alGuardar;

    @FXML
    private void initialize() {
        cmbClase.getItems().setAll(ViviendaController.textoClase(1), ViviendaController.textoClase(2), ViviendaController.textoClase(3));
        Formato.soloEnteros(txtSector, 4);
        Formato.soloEnteros(txtSeccion, 2);
        Formato.soloEnteros(txtManzana, 2);
        Formato.soloEnteros(txtSegmento, 4);
        Formato.soloEnteros(txtEdificacion, 4);
        Formato.soloEnteros(txtViviendaNumero, 4);
        txtTelefono.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("[0-9 +]{0,20}") ? c : null));

        ubicacion = new SelectoresUbicacion(cmbRegion, cmbDepartamento, cmbMunicipio, true);
        Formato.mostrar(lblError, false);
    }

    public void nueva(Consumer<Vivienda> alGuardar) {
        this.alGuardar = alGuardar;
        vivienda = new Vivienda();
        lblTitulo.setText("Nueva vivienda");
        cmbClase.setValue(ViviendaController.textoClase(1));
    }

    public void editar(Vivienda existente, Consumer<Vivienda> alGuardar) {
        this.alGuardar = alGuardar;
        vivienda = existente;
        lblTitulo.setText("Editar vivienda " + Formato.codigo("V", existente.getId()));
        cmbClase.setValue(ViviendaController.textoClase(existente.getClase()));
        txtSector.setText(existente.getSector());
        txtSeccion.setText(existente.getSeccion());
        txtManzana.setText(existente.getManzana());
        txtSegmento.setText(existente.getSegmento());
        txtEdificacion.setText(texto(existente.getEdificacionNumero()));
        txtViviendaNumero.setText(texto(existente.getViviendaNumero()));
        txtTelefono.setText(existente.getTelefono());
        txtBarrio.setText(existente.getBarrio());
        txtDireccion.setText(existente.getDireccion());
        btnGuardar.setText("Guardar cambios");
        ubicacion.seleccionar(existente);
    }

    @FXML
    private void guardar() {
        List<String> errores = new ArrayList<>();
        if (cmbMunicipio.getValue() == null) {
            errores.add("Seleccione la región, el departamento y el municipio.");
        }
        if (cmbClase.getValue() == null) {
            errores.add("Seleccione la clase.");
        }
        if (entero(txtEdificacion) == null || entero(txtViviendaNumero) == null) {
            errores.add("Escriba el número de la edificación y de la vivienda.");
        }
        if (Formato.texto(txtBarrio.getText()) == null || Formato.texto(txtDireccion.getText()) == null) {
            errores.add("Escriba el barrio y la dirección de la vivienda.");
        }
        if (!errores.isEmpty()) {
            lblError.setText(String.join("\n", errores));
            Formato.mostrar(lblError, true);
            return;
        }
        Formato.mostrar(lblError, false);

        vivienda.setMunicipioId(cmbMunicipio.getValue().getId());
        vivienda.setClase(cmbClase.getSelectionModel().getSelectedIndex() + 1);
        vivienda.setSector(Formato.texto(txtSector.getText()));
        vivienda.setSeccion(Formato.texto(txtSeccion.getText()));
        vivienda.setManzana(Formato.texto(txtManzana.getText()));
        vivienda.setSegmento(Formato.texto(txtSegmento.getText()));
        vivienda.setEdificacionNumero(entero(txtEdificacion));
        vivienda.setViviendaNumero(entero(txtViviendaNumero));
        vivienda.setTelefono(Formato.texto(txtTelefono.getText()));
        vivienda.setBarrio(Formato.texto(txtBarrio.getText()));
        vivienda.setDireccion(Formato.texto(txtDireccion.getText()));

        boolean esNueva = vivienda.getId() == null;
        Tareas.ejecutar(() -> esNueva ? hogarService.crearVivienda(vivienda) : hogarService.actualizarVivienda(vivienda), guardada -> {
            Modal.cerrar(btnGuardar);
            alGuardar.accept(guardada);
        });
    }

    @FXML
    private void cancelar() {
        Modal.cerrar(btnGuardar);
    }

    private static Integer entero(TextField campo) {
        String texto = Formato.texto(campo.getText());
        return texto == null ? null : Integer.valueOf(texto);
    }

    private static String texto(Integer numero) {
        return numero == null ? "" : String.valueOf(numero);
    }
}
