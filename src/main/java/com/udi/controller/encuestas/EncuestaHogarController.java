package com.udi.controller.encuestas;

import com.udi.controller.viviendas.ViviendaController;
import com.udi.controller.viviendas.ViviendaDetalleController;
import com.udi.model.Departamento;
import com.udi.model.Encuesta;
import com.udi.model.Hogar;
import com.udi.model.Municipio;
import com.udi.model.Opcion;
import com.udi.model.Region;
import com.udi.model.Vivienda;
import com.udi.service.catalogos.UbicacionService;
import com.udi.service.encuestas.EncuestaService;
import com.udi.util.Formato;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Pestaña «Identificación y hogar (Cap. A)»: datos de la encuesta (informante
 * y periodo, editables) y datos del hogar y la vivienda (solo lectura; se
 * editan en el módulo Viviendas y hogares).
 */
public class EncuestaHogarController implements SeccionEncuesta {

    @FXML
    private Button btnEditar;
    @FXML
    private GridPane gridDatos;
    @FXML
    private Label lblInformante;
    @FXML
    private Label lblOrden;
    @FXML
    private Label lblEncuestador;
    @FXML
    private Label lblDesde;
    @FXML
    private Label lblHasta;
    @FXML
    private Label lblResultado;
    @FXML
    private VBox boxEditar;
    @FXML
    private TextField txtInformante;
    @FXML
    private TextField txtOrden;
    @FXML
    private DatePicker dtpDesde;
    @FXML
    private Label lblHastaCalculada;
    @FXML
    private Label lblError;
    @FXML
    private Label lblCodigoHogar;
    @FXML
    private Label lblHogarNumero;
    @FXML
    private Label lblPersonas;
    @FXML
    private Label lblCiclo;
    @FXML
    private Label lblCodigoVivienda;
    @FXML
    private Label lblDireccion;
    @FXML
    private Label lblBarrio;
    @FXML
    private Label lblTotalHogares;
    @FXML
    private Label lblEdificacion;
    @FXML
    private Label lblViviendaNumero;
    @FXML
    private Label lblTelefono;
    @FXML
    private Label lblRegion;
    @FXML
    private Label lblDepartamento;
    @FXML
    private Label lblMunicipio;
    @FXML
    private Label lblClase;
    @FXML
    private Label lblSector;
    @FXML
    private Label lblSeccion;
    @FXML
    private Label lblManzana;
    @FXML
    private Label lblSegmento;

    private final EncuestaService encuestaService = new EncuestaService();
    private final UbicacionService ubicacionService = new UbicacionService();

    private EncuestaDetalleController detalle;

    @FXML
    private void initialize() {
        dtpDesde.setConverter(Formato.convertidorFecha());
        dtpDesde.valueProperty().addListener((obs, antes, fecha) -> lblHastaCalculada.setText(fecha == null ? ""
                : Formato.fecha(fecha.plusDays(EncuestaService.DIAS_PERIODO - 1))));
        Formato.soloEnteros(txtOrden, 2);
        modoEdicion(false);
    }

    @Override
    public void mostrar(EncuestaDetalleController detalle) {
        this.detalle = detalle;
        Encuesta e = detalle.getEncuesta();
        Formato.mostrar(btnEditar, detalle.esEditable());
        lblInformante.setText(e.getInformanteNombre() == null ? "Por registrar" : e.getInformanteNombre());
        lblOrden.setText(e.getInformanteOrden() == null ? "—" : String.format("%02d", e.getInformanteOrden()));
        lblDesde.setText(Formato.fecha(e.getFechaDesde()));
        lblHasta.setText(Formato.fecha(e.getFechaHasta()));
        lblResultado.setText(e.getResultadoCuadernillo() == null ? "Se registra al cierre" : e.getResultadoCuadernillo().toString());

        pintarHogar(detalle.getHogar(), detalle.getVivienda());
        Tareas.ejecutar(() -> cargarDatos(e, detalle.getVivienda()), this::pintarUbicacion);
    }

    private Datos cargarDatos(Encuesta e, Vivienda v) {
        Datos datos = new Datos();
        datos.encuestador = encuestaService.opcionesEncuestadores().stream()
                .filter(o -> o.getId() == e.getEncuestadorId()).map(Opcion::getTexto).findFirst().orElse("");
        if (v != null && v.getMunicipioId() != null) {
            datos.municipio = ubicacionService.buscarMunicipio(v.getMunicipioId()).orElse(null);
            if (datos.municipio != null) {
                datos.departamento = ubicacionService.buscarDepartamento(datos.municipio.getDepartamentoId()).orElse(null);
            }
            if (datos.departamento != null) {
                datos.region = ubicacionService.buscarRegion(datos.departamento.getRegionId()).orElse(null);
            }
        }
        return datos;
    }

    private void pintarHogar(Hogar h, Vivienda v) {
        if (h != null) {
            lblCodigoHogar.setText(Formato.codigo("H", h.getId()));
            String total = v == null ? "" : " de " + v.getTotalHogares();
            lblHogarNumero.setText("Hogar " + h.getHogarNumero() + total);
            lblPersonas.setText(h.getTotalPersonas() + (h.getTotalPersonas() == 1 ? " persona" : " personas"));
            lblCiclo.setText(h.getCiclo() == null ? "—" : h.getCiclo());
        }
        if (v != null) {
            lblCodigoVivienda.setText(Formato.codigo("V", v.getId()));
            lblDireccion.setText(guion(v.getDireccion()));
            lblBarrio.setText(guion(v.getBarrio()));
            lblTelefono.setText(guion(v.getTelefono()));
            lblTotalHogares.setText(String.valueOf(v.getTotalHogares()));
            lblEdificacion.setText(ViviendaController.tresDigitos(v.getEdificacionNumero()));
            lblViviendaNumero.setText(ViviendaController.tresDigitos(v.getViviendaNumero()));
            lblClase.setText(ViviendaController.textoClase(v.getClase()));
            lblSector.setText(guion(v.getSector()));
            lblSeccion.setText(guion(v.getSeccion()));
            lblManzana.setText(guion(v.getManzana()));
            lblSegmento.setText(guion(v.getSegmento()));
        }
    }

    private void pintarUbicacion(Datos datos) {
        lblEncuestador.setText(datos.encuestador);
        lblRegion.setText(datos.region == null ? "—" : datos.region.getNombre());
        lblDepartamento.setText(datos.departamento == null ? "—" : datos.departamento.getNombre());
        lblMunicipio.setText(datos.municipio == null ? "—" : datos.municipio.getNombre());
    }

    @FXML
    private void editar() {
        Encuesta e = detalle.getEncuesta();
        txtInformante.setText(e.getInformanteNombre());
        txtOrden.setText(e.getInformanteOrden() == null ? "" : String.valueOf(e.getInformanteOrden()));
        dtpDesde.setValue(e.getFechaDesde());
        lblError.setText("");
        modoEdicion(true);
    }

    @FXML
    private void cancelar() {
        modoEdicion(false);
    }

    @FXML
    private void guardar() {
        String informante = Formato.texto(txtInformante.getText());
        if (informante == null || dtpDesde.getValue() == null) {
            lblError.setText("Escriba el nombre del informante y la fecha desde.");
            return;
        }
        Encuesta datos = copiar(detalle.getEncuesta());
        datos.setInformanteNombre(informante);
        datos.setInformanteOrden(Formato.texto(txtOrden.getText()) == null ? null : Integer.valueOf(txtOrden.getText().trim()));
        datos.setFechaDesde(dtpDesde.getValue());
        Tareas.ejecutar(() -> encuestaService.actualizar(datos), guardada -> {
            detalle.recargar();
            detalle.avisar("Datos de la encuesta actualizados. Periodo: " + Formato.fecha(guardada.getFechaDesde())
                    + " – " + Formato.fecha(guardada.getFechaDesde().plusDays(EncuestaService.DIAS_PERIODO - 1)) + ".");
        });
    }

    @FXML
    private void irVivienda() {
        if (detalle.getVivienda() != null) {
            ViviendaDetalleController.abrir(detalle.getVivienda().getId());
        }
    }

    private void modoEdicion(boolean editando) {
        Formato.mostrar(gridDatos, !editando);
        Formato.mostrar(boxEditar, editando);
        btnEditar.setDisable(editando);
    }

    /** Copia los campos editables de la encuesta para enviarlos al servicio sin tocar la original. */
    static Encuesta copiar(Encuesta e) {
        Encuesta c = new Encuesta();
        c.setId(e.getId());
        c.setHogarId(e.getHogarId());
        c.setEncuestadorId(e.getEncuestadorId());
        c.setInformanteNombre(e.getInformanteNombre());
        c.setInformanteOrden(e.getInformanteOrden());
        c.setResultadoCuadernillo(e.getResultadoCuadernillo());
        c.setFechaDesde(e.getFechaDesde());
        c.setGastoFiestas(e.getGastoFiestas());
        c.setFormaPago(e.getFormaPago());
        c.setFormaPagoOtra(e.getFormaPagoOtra());
        c.setObservaciones(e.getObservaciones());
        return c;
    }

    private static String guion(String valor) {
        return valor == null || valor.isEmpty() ? "—" : valor;
    }

    private static final class Datos {
        private String encuestador = "";
        private Municipio municipio;
        private Departamento departamento;
        private Region region;
    }
}
