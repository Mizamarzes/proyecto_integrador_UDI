package com.udi.controller;

import com.udi.model.Encuesta;
import com.udi.model.RegistroDiario;
import com.udi.model.Visita;
import com.udi.model.enums.FormaPago;
import com.udi.model.enums.ResultadoEntrevista;
import com.udi.service.CaracteristicaService;
import com.udi.service.EncuestaService;
import com.udi.service.GastoDiarioService;
import com.udi.service.UsuarioService;
import com.udi.service.VisitaService;
import com.udi.util.Alertas;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Sesion;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña «Cierre (Cap. D)»: preguntas D2 que se hacen al recoger el
 * cuadernillo, resultado del cuadernillo (A.18), verificación del caso y
 * cierre (RF-06). El botón «Cerrar caso» se habilita cuando se cumplen todos
 * los puntos de la verificación.
 */
public class EncuestaCierreController implements SeccionEncuesta {

    private static final int MAXIMO_OBSERVACIONES = 1000;
    private static final String[] LETRAS = {"a", "b", "c", "d", "e", "f"};

    @FXML
    private VBox boxCerrada;
    @FXML
    private Label lblCerrada;
    @FXML
    private VBox formulario;
    @FXML
    private ToggleGroup grupoFiestas;
    @FXML
    private ToggleGroup grupoPago;
    @FXML
    private RadioButton rbFiestasSi;
    @FXML
    private RadioButton rbFiestasNo;
    @FXML
    private GridPane gridPago;
    @FXML
    private VBox boxOtra;
    @FXML
    private TextField txtOtra;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private Label lblCaracteres;
    @FXML
    private ComboBox<ResultadoEntrevista> cmbResultado;
    @FXML
    private Label lblError;
    @FXML
    private HBox boxBotones;
    @FXML
    private Label lblVerificados;
    @FXML
    private ProgressBar barVerificacion;
    @FXML
    private VBox listaChequeos;
    @FXML
    private VBox boxCerrar;
    @FXML
    private Label lblFaltan;
    @FXML
    private Button btnCerrar;
    @FXML
    private VBox boxCerrado;
    @FXML
    private Label lblFechaCierre;
    @FXML
    private Label lblCerradoPor;
    @FXML
    private Label lblReapertura;
    @FXML
    private Button btnReabrir;

    private final EncuestaService encuestaService = new EncuestaService();
    private final CaracteristicaService caracteristicaService = new CaracteristicaService();
    private final GastoDiarioService gastoService = new GastoDiarioService();
    private final VisitaService visitaService = new VisitaService();
    private final UsuarioService usuarioService = new UsuarioService();

    private EncuestaDetalleController detalle;
    private Datos datos;

    @FXML
    private void initialize() {
        rbFiestasSi.setUserData(Boolean.TRUE);
        rbFiestasNo.setUserData(Boolean.FALSE);
        FormaPago[] formas = FormaPago.values();
        for (int i = 0; i < formas.length; i++) {
            RadioButton opcion = new RadioButton(LETRAS[i] + ". " + formas[i].getDescripcion() + " (" + (i + 1) + ")");
            opcion.setUserData(formas[i]);
            opcion.setToggleGroup(grupoPago);
            opcion.getStyleClass().add("radio-tarjeta");
            opcion.setMaxWidth(Double.MAX_VALUE);
            gridPago.add(opcion, i % 3, i / 3);
        }
        grupoPago.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            boolean otra = ahora != null && ahora.getUserData() == FormaPago.OTRA;
            Formato.mostrar(boxOtra, otra);
            if (!otra) {
                txtOtra.clear();
            }
        });
        txtOtra.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().length() <= 100 ? c : null));
        txtObservaciones.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().length() <= MAXIMO_OBSERVACIONES ? c : null));
        txtObservaciones.textProperty().addListener((obs, antes, ahora) ->
                lblCaracteres.setText(ahora.length() + " / " + MAXIMO_OBSERVACIONES));
        lblCaracteres.setText("0 / " + MAXIMO_OBSERVACIONES);
        cmbResultado.getItems().setAll(ResultadoEntrevista.values());
        Formato.mostrar(boxOtra, false);
        Formato.mostrar(lblError, false);
        // Se habilita cuando termine la verificación
        btnCerrar.setDisable(true);
    }

    @Override
    public void mostrar(EncuestaDetalleController detalle) {
        this.detalle = detalle;
        Encuesta e = detalle.getEncuesta();
        boolean editable = detalle.esEditable();

        // Respuestas guardadas
        if (e.getGastoFiestas() != null) {
            (e.getGastoFiestas() ? rbFiestasSi : rbFiestasNo).setSelected(true);
        }
        grupoPago.getToggles().stream().filter(t -> t.getUserData() == e.getFormaPago()).findFirst()
                .ifPresent(t -> t.setSelected(true));
        txtOtra.setText(e.getFormaPagoOtra());
        txtObservaciones.setText(e.getObservaciones() == null ? "" : e.getObservaciones());
        cmbResultado.setValue(e.getResultadoCuadernillo());

        formulario.setDisable(!editable);
        Formato.mostrar(boxBotones, editable);
        Formato.mostrar(boxCerrada, !editable);
        Formato.mostrar(boxCerrar, editable);
        Formato.mostrar(boxCerrado, !editable);
        if (!editable) {
            lblCerrada.setText("Caso cerrado el " + Formato.fechaHora(e.getFechaCierre()));
            lblFechaCierre.setText(Formato.fechaHora(e.getFechaCierre()));
        }
        Formato.mostrar(btnReabrir, !editable && Sesion.esAdmin());
        Formato.mostrar(lblReapertura, !Sesion.esAdmin());

        Tareas.ejecutar(() -> cargarDatos(e), cargados -> {
            datos = cargados;
            lblCerradoPor.setText(cargados.cerradaPor);
            if (!editable) {
                lblCerrada.setText("Caso cerrado el " + Formato.fechaHora(e.getFechaCierre()) + " por " + cargados.cerradaPor);
            }
            pintarVerificacion();
        });
    }

    private Datos cargarDatos(Encuesta e) {
        Datos d = new Datos();
        d.gruposPendientes = caracteristicaService.gruposPendientes(e.getId()).size();
        for (RegistroDiario r : gastoService.listarRegistros(e.getId())) {
            if (gastoService.contarRenglones(r.getId()) > 0) {
                d.diasConInformacion++;
            }
        }
        d.visitas = visitaService.listarPorHogar(e.getHogarId());
        if (e.getCerradaPor() != null) {
            d.cerradaPor = usuarioService.nombreUsuario(e.getCerradaPor());
        }
        return d;
    }

    /** Los seis puntos que deben cumplirse para cerrar el caso. */
    private List<Chequeo> chequeos() {
        Encuesta e = detalle.getEncuesta();
        LocalDate hoy = LocalDate.now();
        List<Chequeo> lista = new ArrayList<>();

        lista.add(new Chequeo("Periodo de 14 días terminado",
                "Del " + Formato.fecha(e.getFechaDesde()) + " al " + Formato.fecha(e.getFechaHasta()) + " · hoy es " + Formato.fecha(hoy),
                hoy.isAfter(e.getFechaHasta()), null));

        lista.add(new Chequeo("Capítulo C completo",
                datos.gruposPendientes == 0 ? "Todos los grupos diligenciados" : "Faltan " + datos.gruposPendientes + " grupos por diligenciar",
                datos.gruposPendientes == 0, EncuestaDetalleController.Pestana.CAPITULO_C));

        lista.add(new Chequeo("Línea de 14 días",
                datos.diasConInformacion + " de " + EncuestaService.DIAS_PERIODO + " días con información (D y D1)",
                datos.diasConInformacion >= EncuestaService.DIAS_PERIODO, EncuestaDetalleController.Pestana.GASTOS));

        Visita recoleccion = datos.visitas.stream()
                .filter(v -> !v.getFecha().isBefore(e.getFechaHasta()))
                .reduce((primera, segunda) -> segunda).orElse(null);
        lista.add(new Chequeo("Visita de recolección",
                recoleccion == null ? "Registre la visita en que recoge el cuadernillo (desde el " + Formato.fecha(e.getFechaHasta()) + ")"
                        : "Visita " + recoleccion.getNumeroVisita() + " · " + Formato.fecha(recoleccion.getFecha()) + " · " + recoleccion.getResultadoVisita(),
                recoleccion != null, EncuestaDetalleController.Pestana.VISITAS));

        boolean d2 = e.getGastoFiestas() != null && e.getFormaPago() != null
                && (e.getFormaPago() != FormaPago.OTRA || e.getFormaPagoOtra() != null);
        lista.add(new Chequeo("Preguntas D2 respondidas",
                d2 ? "Fiestas: " + (e.getGastoFiestas() ? "Sí" : "No") + " · Pago: " + e.getFormaPago().getDescripcion()
                        : "Responda la 1 y la 2 y guarde",
                d2, null));

        lista.add(new Chequeo("Resultado del cuadernillo (A.18)",
                e.getResultadoCuadernillo() == null ? "Sin registrar" : e.getResultadoCuadernillo().toString(),
                e.getResultadoCuadernillo() != null, null));
        return lista;
    }

    private void pintarVerificacion() {
        List<Chequeo> lista = chequeos();
        long listos = lista.stream().filter(c -> c.ok).count();
        long faltan = lista.size() - listos;
        lblVerificados.setText(listos + " de " + lista.size());
        barVerificacion.setProgress(listos / (double) lista.size());
        barVerificacion.getStyleClass().remove("barra-alerta");
        if (faltan > 0) {
            barVerificacion.getStyleClass().add("barra-alerta");
        }
        listaChequeos.getChildren().clear();
        for (Chequeo c : lista) {
            listaChequeos.getChildren().add(fila(c));
        }
        lblFaltan.setText(faltan == 1 ? "Falta 1 punto de la verificación." : "Faltan " + faltan + " puntos de la verificación.");
        Formato.mostrar(lblFaltan, faltan > 0);
        btnCerrar.setDisable(faltan > 0);
    }

    private HBox fila(Chequeo c) {
        Label icono = new Label(c.ok ? "✓" : "!");
        icono.getStyleClass().add(c.ok ? "check-ok" : "check-pendiente");
        Label titulo = new Label(c.titulo);
        titulo.getStyleClass().add("dato-valor");
        Label texto = new Label(c.detalle);
        texto.setWrapText(true);
        texto.getStyleClass().add(c.ok ? "texto-suave" : "texto-alerta");
        VBox textos = new VBox(2, titulo, texto);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox fila = new HBox(10, icono, textos);
        fila.setAlignment(Pos.TOP_LEFT);
        fila.getStyleClass().add("check-fila");
        if (c.pestana != null) {
            Hyperlink ver = new Hyperlink("Ver");
            ver.getStyleClass().add("enlace");
            ver.setMinWidth(Region.USE_PREF_SIZE);
            ver.setOnAction(ev -> detalle.ir(c.pestana));
            fila.getChildren().add(ver);
        }
        return fila;
    }

    @FXML
    private void descartar() {
        detalle.avisar(null);
        detalle.recargarPestana();
    }

    @FXML
    private void guardar() {
        List<String> faltas = new ArrayList<>();
        FormaPago forma = grupoPago.getSelectedToggle() == null ? null : (FormaPago) grupoPago.getSelectedToggle().getUserData();
        if (grupoFiestas.getSelectedToggle() == null) {
            faltas.add("la pregunta 1");
        }
        if (forma == null) {
            faltas.add("la pregunta 2");
        } else if (forma == FormaPago.OTRA && Formato.texto(txtOtra.getText()) == null) {
            faltas.add("cuál es la otra forma de pago");
        }
        if (cmbResultado.getValue() == null) {
            faltas.add("el resultado del cuadernillo (A.18)");
        }
        if (!faltas.isEmpty()) {
            lblError.setText("Falta responder " + String.join(", ", faltas) + ".");
            Formato.mostrar(lblError, true);
            return;
        }
        Formato.mostrar(lblError, false);
        Encuesta respuestas = EncuestaHogarController.copiar(detalle.getEncuesta());
        respuestas.setGastoFiestas((Boolean) grupoFiestas.getSelectedToggle().getUserData());
        respuestas.setFormaPago(forma);
        respuestas.setFormaPagoOtra(forma == FormaPago.OTRA ? Formato.texto(txtOtra.getText()) : null);
        respuestas.setObservaciones(Formato.texto(txtObservaciones.getText()));
        respuestas.setResultadoCuadernillo(cmbResultado.getValue());
        Tareas.ejecutar(() -> encuestaService.actualizar(respuestas), guardada -> {
            detalle.recargar();
            detalle.avisar("Respuestas del capítulo D guardadas.");
        });
    }

    @FXML
    private void cerrarCaso() {
        Modal<CerrarCasoController> modal = Modal.cargar("modulos/encuestas/partials/cerrar_caso", "Cerrar caso");
        modal.controlador().preparar(detalle.getEncuesta(), detalle.descripcionHogar(), datos.diasConInformacion,
                datos.visitas.size(), () -> {
                    detalle.recargar();
                    detalle.avisar("Caso cerrado. La encuesta quedó en solo lectura.");
                });
        modal.mostrar();
    }

    @FXML
    private void reabrir() {
        if (Alertas.confirmar("Reabrir caso", "La encuesta pasará a «Reabierta» y se podrán hacer cambios. ¿Desea reabrirla?")) {
            long id = detalle.getEncuesta().getId();
            Tareas.accion(() -> encuestaService.reabrir(id), () -> {
                detalle.recargar();
                detalle.avisar("Caso reabierto. La encuesta admite cambios de nuevo.");
            });
        }
    }

    private static final class Datos {
        private int gruposPendientes;
        private int diasConInformacion;
        private List<Visita> visitas = new ArrayList<>();
        private String cerradaPor = "";
    }

    private static final class Chequeo {
        private final String titulo;
        private final String detalle;
        private final boolean ok;
        private final EncuestaDetalleController.Pestana pestana;

        private Chequeo(String titulo, String detalle, boolean ok, EncuestaDetalleController.Pestana pestana) {
            this.titulo = titulo;
            this.detalle = detalle;
            this.ok = ok;
            this.pestana = pestana;
        }
    }
}
