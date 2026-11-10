package com.udi.controller.encuestas;

import com.udi.model.Visita;
import com.udi.model.enums.ResultadoEntrevista;
import com.udi.service.encuestas.VisitaService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Consumer;

/** Modal para registrar o editar una visita del control de visitas (capítulo B). */
public class VisitaFormController {

    private static final int MAXIMO_PENDIENTES = 200;

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblNumero;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private DatePicker dtpFecha;
    @FXML
    private TextField txtInicio;
    @FXML
    private TextField txtFin;
    @FXML
    private ComboBox<ResultadoEntrevista> cmbResultado;
    @FXML
    private TextArea txtPendientes;
    @FXML
    private DatePicker dtpProxima;
    @FXML
    private TextField txtHoraProxima;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    private final VisitaService visitaService = new VisitaService();

    private Visita visita;
    private Consumer<Visita> alGuardar;

    @FXML
    private void initialize() {
        dtpFecha.setConverter(Formato.convertidorFecha());
        dtpProxima.setConverter(Formato.convertidorFecha());
        Formato.soloHora(txtInicio);
        Formato.soloHora(txtFin);
        Formato.soloHora(txtHoraProxima);
        txtPendientes.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().length() <= MAXIMO_PENDIENTES ? c : null));
        cmbResultado.getItems().setAll(ResultadoEntrevista.values());
        Formato.mostrar(lblError, false);
    }

    /** Visita nueva del hogar con el número {@code numero} (1 a 14). */
    public void nueva(long hogarId, int numero, String hogar, Consumer<Visita> alGuardar) {
        this.alGuardar = alGuardar;
        visita = new Visita();
        visita.setHogarId(hogarId);
        visita.setNumeroVisita(numero);
        lblTitulo.setText("Registrar visita");
        lblNumero.setText("Visita " + numero + " de " + VisitaService.MAXIMO_VISITAS);
        lblSubtitulo.setText("Control de visitas · " + hogar);
        dtpFecha.setValue(LocalDate.now());
        txtInicio.setText(LocalTime.now().withSecond(0).withNano(0).toString());
    }

    public void editar(Visita existente, String hogar, Consumer<Visita> alGuardar) {
        this.alGuardar = alGuardar;
        visita = existente;
        lblTitulo.setText("Editar visita");
        lblNumero.setText("Visita " + existente.getNumeroVisita() + " de " + VisitaService.MAXIMO_VISITAS);
        lblSubtitulo.setText("Control de visitas · " + hogar);
        dtpFecha.setValue(existente.getFecha());
        txtInicio.setText(Formato.hora(existente.getHoraInicio()));
        txtFin.setText(Formato.hora(existente.getHoraFin()));
        cmbResultado.setValue(existente.getResultadoVisita());
        txtPendientes.setText(existente.getPartesPendientes());
        dtpProxima.setValue(existente.getFechaProxima());
        txtHoraProxima.setText(Formato.hora(existente.getHoraProxima()));
    }

    @FXML
    private void guardar() {
        LocalDate fecha = dtpFecha.getValue();
        LocalTime inicio = Formato.leerHora(txtInicio.getText());
        LocalTime fin = Formato.leerHora(txtFin.getText());
        LocalDate proxima = dtpProxima.getValue();
        LocalTime horaProxima = Formato.leerHora(txtHoraProxima.getText());

        if (fecha == null || inicio == null) {
            error("La fecha y la hora de inicio son obligatorias (hora en formato HH:mm).");
            return;
        }
        if (cmbResultado.getValue() == null) {
            error("Seleccione el resultado de la visita.");
            return;
        }
        if (!txtFin.getText().trim().isEmpty() && fin == null) {
            error("Escriba la hora de fin en formato HH:mm.");
            return;
        }
        if (fin != null && !fin.isAfter(inicio)) {
            error("La hora de fin debe ser posterior a la hora de inicio.");
            return;
        }
        if (horaProxima != null && proxima == null) {
            error("Indique la fecha de la próxima cita.");
            return;
        }

        visita.setFecha(fecha);
        visita.setHoraInicio(fecha.atTime(inicio));
        visita.setHoraFin(fin == null ? null : fecha.atTime(fin));
        visita.setResultadoVisita(cmbResultado.getValue());
        visita.setPartesPendientes(Formato.texto(txtPendientes.getText()));
        visita.setFechaProxima(proxima);
        visita.setHoraProxima(proxima == null || horaProxima == null ? null : proxima.atTime(horaProxima));

        boolean esNueva = visita.getId() == null;
        Formato.mostrar(lblError, false);
        Tareas.ejecutar(() -> esNueva ? visitaService.crear(visita) : visitaService.actualizar(visita), guardada -> {
            Modal.cerrar(btnGuardar);
            alGuardar.accept(guardada);
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
