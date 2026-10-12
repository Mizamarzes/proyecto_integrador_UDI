package com.udi.controller;

import com.udi.exception.ReglaNegocioException;
import com.udi.model.Encuesta;
import com.udi.model.Opcion;
import com.udi.model.enums.FormaPago;
import com.udi.model.enums.ResultadoEntrevista;
import com.udi.service.EncuestaService;
import com.udi.util.Alertas;
import com.udi.util.Tareas;
import com.udi.util.Formato;
import com.udi.util.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.Node;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Formulario de una encuesta: capítulo A (hogar, encuestador, periodo e
 * informante) y cierre D2. Permite cerrar el caso y, al administrador, reabrirlo (RF-06).
 */
public class EncuestaFormController {

    private static final String SIN_DATO = "Sin dato";
    private static final String SI = "Sí";
    private static final String NO = "No";

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblCierre;
    @FXML
    private Label lblAviso;
    @FXML
    private GridPane formulario;
    @FXML
    private ComboBox<Opcion> cmbHogar;
    @FXML
    private ComboBox<Opcion> cmbEncuestador;
    @FXML
    private DatePicker dtpFechaDesde;
    @FXML
    private Label lblFechaHasta;
    @FXML
    private TextField txtInformante;
    @FXML
    private TextField txtOrden;
    @FXML
    private ComboBox<ResultadoEntrevista> cmbResultado;
    @FXML
    private ComboBox<String> cmbGastoFiestas;
    @FXML
    private ComboBox<FormaPago> cmbFormaPago;
    @FXML
    private TextField txtFormaPagoOtra;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCerrarCaso;
    @FXML
    private Button btnReabrir;
    @FXML
    private Button btnEliminar;

    private final EncuestaService encuestaService = new EncuestaService();

    /** Encuesta que se edita; null si es nueva. */
    private Encuesta encuesta;

    @FXML
    private void initialize() {
        dtpFechaDesde.setConverter(Formato.convertidorFecha());
        dtpFechaDesde.valueProperty().addListener((obs, antes, fecha) -> lblFechaHasta.setText(fecha == null ? ""
                : Formato.fecha(fecha.plusDays(EncuestaService.DIAS_PERIODO - 1))));

        txtOrden.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("\\d{0,2}") ? c : null));

        cmbResultado.getItems().setAll(ResultadoEntrevista.values());
        cmbGastoFiestas.getItems().setAll(SIN_DATO, SI, NO);
        cmbFormaPago.getItems().setAll(FormaPago.values());
        cmbFormaPago.valueProperty().addListener((obs, antes, forma) -> {
            boolean otra = forma == FormaPago.OTRA;
            txtFormaPagoOtra.setDisable(!otra);
            if (!otra) {
                txtFormaPagoOtra.clear();
            }
        });
        txtFormaPagoOtra.setDisable(true);
        mostrar(lblAviso, false);
        mostrar(lblCierre, false);
    }

    /** Prepara el formulario para registrar una encuesta nueva. */
    public void nueva() {
        encuesta = null;
        lblTitulo.setText("Nueva encuesta");
        mostrar(lblEstado, false);
        dtpFechaDesde.setValue(LocalDate.now());
        cmbGastoFiestas.setValue(SIN_DATO);
        actualizarBotones();
        Tareas.ejecutar(() -> new Opciones(encuestaService.opcionesHogares(null),
                encuestaService.opcionesEncuestadores(), encuestaService.encuestadorDeSesion()), opciones -> {
            cargarOpciones(opciones);
            if (cmbHogar.getItems().isEmpty()) {
                lblAviso.setText("Todos los hogares registrados ya tienen encuesta. "
                        + "Registre primero un hogar nuevo en el módulo Hogares.");
                mostrar(lblAviso, true);
            }
        });
    }

    /** Carga la encuesta {@code id} para consultarla o editarla. */
    public void editar(long id) {
        Tareas.ejecutar(() -> {
            Encuesta e = encuestaService.buscar(id)
                    .orElseThrow(() -> new ReglaNegocioException("No existe la encuesta " + id + "."));
            Opciones opciones = new Opciones(encuestaService.opcionesHogares(e.getHogarId()),
                    encuestaService.opcionesEncuestadores(), encuestaService.encuestadorDeSesion());
            opciones.encuesta = e;
            return opciones;
        }, opciones -> {
            cargarOpciones(opciones);
            mostrarEncuesta(opciones.encuesta);
        });
    }

    @FXML
    private void guardar() {
        Encuesta datos = leerFormulario();
        boolean esNueva = encuesta == null;
        Tareas.ejecutar(() -> esNueva ? encuestaService.crear(datos) : encuestaService.actualizar(datos), guardada -> {
            Alertas.info("Encuesta guardada", "La encuesta " + Formato.codigo("E", guardada.getId())
                    + (esNueva ? " se registró correctamente." : " se actualizó correctamente."));
            volver();
        });
    }

    @FXML
    private void cerrarCaso() {
        if (Alertas.confirmar("Cerrar caso", "Después de cerrar la encuesta no se podrán registrar visitas, "
                + "gastos ni cambios. Solo un administrador puede reabrirla.\n\n¿Desea cerrar el caso?")) {
            long id = encuesta.getId();
            Tareas.accion(() -> encuestaService.cerrar(id), () -> editar(id));
        }
    }

    @FXML
    private void reabrir() {
        if (Alertas.confirmar("Reabrir caso", "¿Desea reabrir la encuesta para permitir cambios?")) {
            long id = encuesta.getId();
            Tareas.accion(() -> encuestaService.reabrir(id), () -> editar(id));
        }
    }

    @FXML
    private void eliminar() {
        if (Alertas.confirmar("Eliminar encuesta", "Se eliminará la encuesta con su capítulo C y sus registros "
                + "diarios. Esta acción no se puede deshacer.\n\n¿Desea continuar?")) {
            long id = encuesta.getId();
            Tareas.accion(() -> encuestaService.eliminar(id), this::volver);
        }
    }

    @FXML
    private void volver() {
        NavbarController.abrir("modulos/encuestas/encuesta", "Encuestas");
    }

    private void cargarOpciones(Opciones opciones) {
        cmbHogar.getItems().setAll(opciones.hogares);
        cmbEncuestador.getItems().setAll(opciones.encuestadores);
        // El encuestador registra sus propias encuestas
        opciones.encuestadorPropio.ifPresent(id -> seleccionar(cmbEncuestador, id));
        cmbEncuestador.setDisable(opciones.encuestadorPropio.isPresent());
    }

    private void mostrarEncuesta(Encuesta e) {
        encuesta = e;
        lblTitulo.setText("Encuesta " + Formato.codigo("E", e.getId()));
        lblEstado.setText(e.getEstado().getDescripcion());
        lblEstado.getStyleClass().removeIf(c -> c.startsWith("insignia-"));
        lblEstado.getStyleClass().add("insignia-" + e.getEstado().name().toLowerCase());
        mostrar(lblEstado, true);
        if (e.estaCerrada()) {
            lblCierre.setText("Cerrada el " + Formato.fechaHora(e.getFechaCierre())
                    + ". No admite cambios mientras un administrador no la reabra.");
        } else {
            lblCierre.setText("Periodo del " + Formato.fecha(e.getFechaDesde()) + " al " + Formato.fecha(e.getFechaHasta()) + ".");
        }
        mostrar(lblCierre, true);

        seleccionar(cmbHogar, e.getHogarId());
        cmbHogar.setDisable(true);
        seleccionar(cmbEncuestador, e.getEncuestadorId());
        dtpFechaDesde.setValue(e.getFechaDesde());
        txtInformante.setText(e.getInformanteNombre());
        txtOrden.setText(e.getInformanteOrden() == null ? "" : String.valueOf(e.getInformanteOrden()));
        cmbResultado.setValue(e.getResultadoCuadernillo());
        cmbGastoFiestas.setValue(e.getGastoFiestas() == null ? SIN_DATO : e.getGastoFiestas() ? SI : NO);
        cmbFormaPago.setValue(e.getFormaPago());
        txtFormaPagoOtra.setText(e.getFormaPagoOtra());
        txtObservaciones.setText(e.getObservaciones());

        formulario.setDisable(e.estaCerrada());
        txtObservaciones.setDisable(e.estaCerrada());
        actualizarBotones();
    }

    private Encuesta leerFormulario() {
        Encuesta e = new Encuesta();
        if (encuesta != null) {
            e.setId(encuesta.getId());
        }
        e.setHogarId(cmbHogar.getValue() == null ? null : cmbHogar.getValue().getId());
        e.setEncuestadorId(cmbEncuestador.getValue() == null ? null : cmbEncuestador.getValue().getId());
        e.setFechaDesde(dtpFechaDesde.getValue());
        e.setInformanteNombre(texto(txtInformante));
        e.setInformanteOrden(texto(txtOrden) == null ? null : Integer.valueOf(txtOrden.getText().trim()));
        e.setResultadoCuadernillo(cmbResultado.getValue());
        e.setGastoFiestas(SI.equals(cmbGastoFiestas.getValue()) ? Boolean.TRUE
                : NO.equals(cmbGastoFiestas.getValue()) ? Boolean.FALSE : null);
        e.setFormaPago(cmbFormaPago.getValue());
        e.setFormaPagoOtra(texto(txtFormaPagoOtra));
        e.setObservaciones(texto(txtObservaciones.getText()));
        return e;
    }

    private void actualizarBotones() {
        boolean existe = encuesta != null;
        boolean cerrada = existe && encuesta.estaCerrada();
        mostrar(btnGuardar, !cerrada);
        mostrar(btnCerrarCaso, existe && !cerrada);
        mostrar(btnEliminar, existe && !cerrada);
        mostrar(btnReabrir, cerrada && Sesion.esAdmin());
    }

    private static void seleccionar(ComboBox<Opcion> combo, Long id) {
        if (id == null) {
            return;
        }
        combo.getItems().stream().filter(o -> o.getId() == id).findFirst().ifPresent(combo::setValue);
    }

    private static String texto(TextField campo) {
        return texto(campo.getText());
    }

    private static String texto(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    private static void mostrar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    /** Datos que se cargan de la base antes de mostrar el formulario. */
    private static final class Opciones {
        private final List<Opcion> hogares;
        private final List<Opcion> encuestadores;
        private final Optional<Long> encuestadorPropio;
        private Encuesta encuesta;

        private Opciones(List<Opcion> hogares, List<Opcion> encuestadores, Optional<Long> encuestadorPropio) {
            this.hogares = hogares;
            this.encuestadores = encuestadores;
            this.encuestadorPropio = encuestadorPropio;
        }
    }
}
