package com.udi.controller;

import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.Rutas;
import com.udi.model.Encuesta;
import com.udi.model.Hogar;
import com.udi.model.Municipio;
import com.udi.model.Vivienda;
import com.udi.service.EncuestaService;
import com.udi.service.HogarService;
import com.udi.service.UbicacionService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detalle de una vivienda: su identificación (capítulo A) y sus hogares. Desde
 * aquí se agregan y editan hogares, y se crea o abre la encuesta de cada uno.
 */
public class ViviendaDetalleController {

    static final String VISTA_HOGAR = "modulos/viviendas/partials/hogar_form";

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblCodigo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private Label lblMunicipio;
    @FXML
    private Label lblSectorSeccion;
    @FXML
    private Label lblManzanaSegmento;
    @FXML
    private Label lblEdificacion;
    @FXML
    private Label lblTelefono;
    @FXML
    private Label lblRegistrados;
    @FXML
    private Label lblAviso;
    @FXML
    private Button btnAgregar;
    @FXML
    private Hyperlink lnkEditarVivienda;
    @FXML
    private Hyperlink lnkEncuestas;
    @FXML
    private Label lblTope;
    @FXML
    private TableView<Fila> tabla;
    @FXML
    private TableColumn<Fila, String> colNumero;
    @FXML
    private TableColumn<Fila, String> colPersonas;
    @FXML
    private TableColumn<Fila, String> colCiclo;
    @FXML
    private TableColumn<Fila, Fila> colEncuesta;
    @FXML
    private TableColumn<Fila, Fila> colAcciones;

    private final HogarService hogarService = new HogarService();
    private final EncuestaService encuestaService = new EncuestaService();
    private final UbicacionService ubicacionService = new UbicacionService();

    private Vivienda vivienda;
    private List<Hogar> hogares = new ArrayList<>();

    /** Abre el detalle de la vivienda {@code id} en el área de contenido. */
    public static void abrir(long id) {
        NavbarController.marcarModulo("viviendas");
        ViviendaDetalleController detalle = NavbarController.abrir("modulos/viviendas/vivienda_detalle",
                "Viviendas y hogares", Formato.codigo("V", id));
        detalle.cargar(id);
    }

    @FXML
    private void initialize() {
        // Cada acción se muestra con los mismos middlewares que protegen la vista que abre
        Rutas.permitir(lnkEditarVivienda, ViviendaController.VISTA_FORMULARIO);
        Rutas.permitir(btnAgregar, VISTA_HOGAR);
        Rutas.permitir(lnkEncuestas, "modulos/encuestas/encuesta");
        Formato.mostrar(lblAviso, false);
        configurarTabla();
    }

    private void cargar(long id) {
        Tareas.ejecutar(() -> {
            Datos datos = new Datos();
            datos.vivienda = hogarService.buscarVivienda(id)
                    .orElseThrow(() -> new ReglaNegocioException("No existe la vivienda " + id + "."));
            datos.municipio = datos.vivienda.getMunicipioId() == null ? null
                    : ubicacionService.buscarMunicipio(datos.vivienda.getMunicipioId()).orElse(null);
            datos.hogares = hogarService.listarHogaresPorVivienda(id);
            for (Hogar h : datos.hogares) {
                encuestaService.buscarPorHogar(h.getId()).ifPresent(e -> datos.encuestas.put(h.getId(), e));
            }
            return datos;
        }, this::pintar);
    }

    private void pintar(Datos datos) {
        vivienda = datos.vivienda;
        hogares = datos.hogares;
        String municipio = datos.municipio == null ? "" : datos.municipio.getNombre();
        lblTitulo.setText(vivienda.getDireccion());
        lblCodigo.setText(Formato.codigo("V", vivienda.getId()));
        lblSubtitulo.setText((vivienda.getBarrio() == null ? "" : vivienda.getBarrio() + " · ") + municipio
                + " · " + ViviendaController.textoClase(vivienda.getClase()).replaceFirst("^\\d\\. ", ""));
        lblMunicipio.setText(municipio + (vivienda.getMunicipioId() == null ? "" : " (" + vivienda.getMunicipioId() + ")"));
        lblSectorSeccion.setText(guion(vivienda.getSector()) + " · " + guion(vivienda.getSeccion()));
        lblManzanaSegmento.setText(guion(vivienda.getManzana()) + " · " + guion(vivienda.getSegmento()));
        lblEdificacion.setText(ViviendaController.tresDigitos(vivienda.getEdificacionNumero()) + " · "
                + ViviendaController.tresDigitos(vivienda.getViviendaNumero()));
        lblTelefono.setText(vivienda.getTelefono() == null ? "—" : vivienda.getTelefono());
        lblRegistrados.setText(hogares.size() + " de " + vivienda.getTotalHogares());

        boolean completa = hogares.size() >= vivienda.getTotalHogares();
        btnAgregar.setDisable(completa);
        lblTope.setText(completa ? "Ya están registrados los " + vivienda.getTotalHogares() + " hogares de la vivienda." : "");
        Formato.mostrar(lblTope, completa);

        List<Fila> filas = new ArrayList<>();
        for (Hogar h : hogares) {
            filas.add(new Fila(h, datos.encuestas.get(h.getId())));
        }
        tabla.getItems().setAll(filas);
    }

    private void avisar(String mensaje) {
        lblAviso.setText(mensaje);
        Formato.mostrar(lblAviso, true);
    }

    @FXML
    private void volver() {
        ViviendaController.abrirListado();
    }

    @FXML
    private void irEncuestas() {
        NavbarController.marcarModulo("encuestas");
        NavbarController.abrir("modulos/encuestas/encuesta", "Encuestas");
    }

    @FXML
    private void editarVivienda() {
        Modal<ViviendaFormController> modal = Modal.cargar(ViviendaController.VISTA_FORMULARIO, "Editar vivienda");
        modal.controlador().editar(vivienda, guardada -> {
            avisar("Datos de la vivienda actualizados.");
            cargar(guardada.getId());
        });
        modal.mostrar();
    }

    @FXML
    private void agregarHogar() {
        abrirHogar(null);
    }

    private void abrirHogar(Hogar hogar) {
        Modal<HogarFormController> modal = Modal.cargar(VISTA_HOGAR,
                hogar == null ? "Agregar hogar" : "Editar hogar");
        modal.controlador().preparar(vivienda, hogares, hogar, guardado -> {
            avisar(hogar == null ? "Hogar " + guardado.getHogarNumero() + " agregado a la vivienda."
                    : "Hogar " + guardado.getHogarNumero() + " actualizado.");
            cargar(vivienda.getId());
        });
        modal.mostrar();
    }

    /**
     * Atajo «Crear encuesta»: el encuestador crea la encuesta del hogar a su
     * nombre; los demás roles eligen el encuestador en el modal de nueva encuesta.
     */
    private void crearEncuesta(Hogar hogar) {
        Tareas.ejecutar(encuestaService::encuestadorDeSesion, propio -> {
            if (propio.isPresent()) {
                Encuesta nueva = new Encuesta();
                nueva.setHogarId(hogar.getId());
                nueva.setEncuestadorId(propio.get());
                nueva.setFechaDesde(LocalDate.now());
                Tareas.ejecutar(() -> encuestaService.crear(nueva),
                        creada -> EncuestaDetalleController.abrir(creada.getId(), EncuestaDetalleController.Pestana.HOGAR));
            } else {
                Modal<NuevaEncuestaController> modal = Modal.cargar(EncuestaController.VISTA_NUEVA, "Nueva encuesta");
                modal.controlador().preparar(creada ->
                        EncuestaDetalleController.abrir(creada.getId(), EncuestaDetalleController.Pestana.HOGAR));
                modal.controlador().buscar(Formato.codigo("H", hogar.getId()));
                modal.mostrar();
            }
        });
    }

    private void configurarTabla() {
        tabla.setPlaceholder(new Label("La vivienda aún no tiene hogares registrados."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colNumero.setCellValueFactory(d -> new ReadOnlyStringWrapper("Hogar " + d.getValue().hogar.getHogarNumero()));
        colPersonas.setCellValueFactory(d -> {
            int personas = d.getValue().hogar.getTotalPersonas();
            return new ReadOnlyStringWrapper(personas + (personas == 1 ? " persona" : " personas"));
        });
        colCiclo.setCellValueFactory(d -> new ReadOnlyStringWrapper(guion(d.getValue().hogar.getCiclo())));
        colEncuesta.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        colAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));

        colEncuesta.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Fila fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia || fila == null) {
                    setGraphic(null);
                    return;
                }
                if (fila.encuesta == null) {
                    Label sin = new Label("Sin encuesta");
                    sin.getStyleClass().add("texto-suave");
                    setGraphic(sin);
                    return;
                }
                Label codigo = new Label(Formato.codigo("E", fila.encuesta.getId()));
                codigo.getStyleClass().add("dato-codigo");
                HBox caja = new HBox(8, codigo, EncuestaController.insigniaEstado(fila.encuesta.getEstado()));
                caja.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caja);
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink editar = enlace("Editar");
            private final Hyperlink encuesta = enlace("");
            private final HBox caja = new HBox(12, editar, encuesta);

            {
                caja.setAlignment(Pos.CENTER_LEFT);
                editar.setOnAction(e -> abrirHogar(getItem().hogar));
                encuesta.setOnAction(e -> {
                    Fila fila = getItem();
                    if (fila.encuesta == null) {
                        crearEncuesta(fila.hogar);
                    } else {
                        EncuestaDetalleController.abrir(fila.encuesta.getId(), EncuestaDetalleController.Pestana.VISITAS);
                    }
                });
            }

            @Override
            protected void updateItem(Fila fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia || fila == null) {
                    setGraphic(null);
                    return;
                }
                encuesta.setText(fila.encuesta == null ? "Crear encuesta" : "Ver encuesta");
                // Un hogar con encuesta cerrada no se puede editar (RF-06)
                Formato.mostrar(editar, Rutas.puedeAbrir(VISTA_HOGAR)
                        && (fila.encuesta == null || !fila.encuesta.estaCerrada()));
                Formato.mostrar(encuesta, Rutas.puedeAbrir(fila.encuesta == null
                        ? EncuestaController.VISTA_NUEVA : "modulos/encuestas/encuesta_detalle"));
                setGraphic(caja);
            }
        });
    }

    private static Hyperlink enlace(String texto) {
        Hyperlink enlace = new Hyperlink(texto);
        enlace.getStyleClass().add("enlace");
        return enlace;
    }

    private static String guion(String valor) {
        return valor == null || valor.isEmpty() ? "—" : valor;
    }

    private static final class Datos {
        private Vivienda vivienda;
        private Municipio municipio;
        private List<Hogar> hogares = new ArrayList<>();
        private final Map<Long, Encuesta> encuestas = new HashMap<>();
    }

    /** Hogar con su encuesta (null si todavía no tiene). */
    private static final class Fila {
        private final Hogar hogar;
        private final Encuesta encuesta;

        private Fila(Hogar hogar, Encuesta encuesta) {
            this.hogar = hogar;
            this.encuesta = encuesta;
        }
    }
}
