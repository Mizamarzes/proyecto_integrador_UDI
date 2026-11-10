package com.udi.controller.viviendas;

import com.udi.componentes.SelectBuscable;
import com.udi.controller.shared.NavbarController;
import com.udi.middleware.Rutas;
import com.udi.model.Departamento;
import com.udi.model.Entidad;
import com.udi.model.Encuesta;
import com.udi.model.Hogar;
import com.udi.model.Municipio;
import com.udi.model.Region;
import com.udi.model.Vivienda;
import com.udi.service.encuestas.EncuestaService;
import com.udi.service.viviendas.HogarService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listado de viviendas del módulo «Viviendas y hogares». Cada vivienda
 * muestra su ubicación, cuántos hogares tiene registrados y cuántos no tienen
 * encuesta. Los filtros de región, departamento y municipio se aplican en la
 * consulta; la búsqueda por texto y el de hogares, sobre lo ya cargado.
 */
public class ViviendaController {

    static final String VISTA_FORMULARIO = "modulos/viviendas/partials/vivienda_form";

    private static final String TODAS = "Todas";
    private static final String SIN_HOGARES = "Sin hogares";
    private static final String SIN_ENCUESTA = "Con hogares sin encuesta";

    @FXML
    private Button btnNueva;
    @FXML
    private HBox boxAviso;
    @FXML
    private Label lblAviso;
    @FXML
    private TextField txtBuscar;
    @FXML
    private SelectBuscable<Region> cmbRegion;
    @FXML
    private SelectBuscable<Departamento> cmbDepartamento;
    @FXML
    private SelectBuscable<Municipio> cmbMunicipio;
    @FXML
    private ComboBox<String> cmbFiltro;
    @FXML
    private Label lblTotal;
    @FXML
    private TableView<Fila> tabla;
    @FXML
    private TableColumn<Fila, String> colCodigo;
    @FXML
    private TableColumn<Fila, Fila> colDireccion;
    @FXML
    private TableColumn<Fila, String> colRegion;
    @FXML
    private TableColumn<Fila, String> colDepartamento;
    @FXML
    private TableColumn<Fila, String> colMunicipio;
    @FXML
    private TableColumn<Fila, String> colCartografia;
    @FXML
    private TableColumn<Fila, String> colHogares;
    @FXML
    private TableColumn<Fila, String> colEncuestas;
    @FXML
    private TableColumn<Fila, Fila> colAcciones;

    private final HogarService hogarService = new HogarService();
    private final EncuestaService encuestaService = new EncuestaService();
    private final ObservableList<Fila> viviendas = FXCollections.observableArrayList();
    private final FilteredList<Fila> filtradas = new FilteredList<>(viviendas);

    private SelectoresUbicacion ubicacion;
    /** Junta en una sola consulta los cambios encadenados de los filtros de ubicación. */
    private boolean recargaPendiente;
    /** Vivienda recién creada, para el enlace «Agregar sus hogares». */
    private Long nueva;

    /** Abre el listado de viviendas desde otra pantalla. */
    public static void abrirListado() {
        NavbarController.marcarModulo("viviendas");
        NavbarController.abrir("modulos/viviendas/vivienda", "Viviendas y hogares");
    }

    @FXML
    private void initialize() {
        Rutas.permitir(btnNueva, VISTA_FORMULARIO);
        Formato.mostrar(boxAviso, false);
        cmbFiltro.getItems().setAll(TODAS, SIN_HOGARES, SIN_ENCUESTA);
        cmbRegion.setOpcionVacia("Todas");
        cmbDepartamento.setOpcionVacia("Todos");
        cmbMunicipio.setOpcionVacia("Todos");
        ubicacion = new SelectoresUbicacion(cmbRegion, cmbDepartamento, cmbMunicipio, false);
        configurarTabla();
        txtBuscar.textProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbFiltro.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbRegion.valueProperty().addListener((obs, antes, ahora) -> programarCarga());
        cmbDepartamento.valueProperty().addListener((obs, antes, ahora) -> programarCarga());
        cmbMunicipio.valueProperty().addListener((obs, antes, ahora) -> programarCarga());
        limpiar();
        cargar();
    }

    private void programarCarga() {
        if (!recargaPendiente) {
            recargaPendiente = true;
            Platform.runLater(() -> {
                recargaPendiente = false;
                cargar();
            });
        }
    }

    private void cargar() {
        Long regionId = id(cmbRegion.getValue());
        Long departamentoId = id(cmbDepartamento.getValue());
        Long municipioId = id(cmbMunicipio.getValue());
        Tareas.ejecutar(() -> {
            Set<Long> conEncuesta = new HashSet<>();
            for (Encuesta e : encuestaService.listar()) {
                conEncuesta.add(e.getHogarId());
            }
            Map<Long, List<Hogar>> hogares = new HashMap<>();
            for (Hogar h : hogarService.listarHogares()) {
                hogares.computeIfAbsent(h.getViviendaId(), k -> new ArrayList<>()).add(h);
            }
            List<Fila> filas = new ArrayList<>();
            for (Vivienda v : hogarService.listarViviendas(regionId, departamentoId, municipioId)) {
                List<Hogar> propios = hogares.getOrDefault(v.getId(), List.of());
                int sinEncuesta = (int) propios.stream().filter(h -> !conEncuesta.contains(h.getId())).count();
                filas.add(new Fila(v, propios.size(), sinEncuesta));
            }
            return filas;
        }, viviendas::setAll);
    }

    @FXML
    private void limpiar() {
        txtBuscar.clear();
        cmbFiltro.setValue(TODAS);
        ubicacion.limpiar();
    }

    @FXML
    private void nuevaVivienda() {
        Modal<ViviendaFormController> modal = Modal.cargar(VISTA_FORMULARIO, "Nueva vivienda");
        modal.controlador().nueva(creada -> {
            nueva = creada.getId();
            lblAviso.setText("Vivienda " + Formato.codigo("V", creada.getId()) + " registrada: " + creada.getDireccion() + ".");
            Formato.mostrar(boxAviso, true);
            cargar();
        });
        modal.mostrar();
    }

    @FXML
    private void abrirNueva() {
        if (nueva != null) {
            ViviendaDetalleController.abrir(nueva);
        }
    }

    private void configurarTabla() {
        tabla.setItems(filtradas);
        tabla.setPlaceholder(new Label("Ninguna vivienda coincide con los filtros. Puedes registrar una nueva vivienda."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCodigo.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.codigo("V", d.getValue().vivienda.getId())));
        colDireccion.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        colRegion.setCellValueFactory(d -> new ReadOnlyStringWrapper(guion(d.getValue().vivienda.getRegionNombre())));
        colDepartamento.setCellValueFactory(d -> new ReadOnlyStringWrapper(guion(d.getValue().vivienda.getDepartamentoNombre())));
        colMunicipio.setCellValueFactory(d -> new ReadOnlyStringWrapper(guion(d.getValue().vivienda.getMunicipioNombre())));
        colCartografia.setCellValueFactory(d -> new ReadOnlyStringWrapper(cartografia(d.getValue().vivienda)));
        colHogares.setCellValueFactory(d -> new ReadOnlyStringWrapper(textoHogares(d.getValue().registrados)));
        colEncuestas.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().textoEncuestas()));
        colAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));

        colDireccion.setCellFactory(col -> new TableCell<>() {
            private final Label direccion = new Label();
            private final Label barrio = new Label();
            private final VBox caja = new VBox(1, direccion, barrio);

            {
                direccion.getStyleClass().add("dato-valor");
                barrio.getStyleClass().add("texto-suave");
            }

            @Override
            protected void updateItem(Fila fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia || fila == null) {
                    setGraphic(null);
                    return;
                }
                direccion.setText(fila.vivienda.getDireccion());
                String b = fila.vivienda.getBarrio();
                barrio.setText(b == null ? "" : b);
                Formato.mostrar(barrio, b != null);
                setGraphic(caja);
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink enlace = new Hyperlink();

            {
                enlace.getStyleClass().add("enlace");
                enlace.setOnAction(e -> ViviendaDetalleController.abrir(getItem().vivienda.getId()));
            }

            @Override
            protected void updateItem(Fila fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia || fila == null) {
                    setGraphic(null);
                    return;
                }
                enlace.setText(fila.registrados > 0 ? "Ver hogares" : "Agregar hogares");
                setGraphic(enlace);
            }
        });

        tabla.setRowFactory(t -> {
            TableRow<Fila> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    ViviendaDetalleController.abrir(fila.getItem().vivienda.getId());
                }
            });
            return fila;
        });

        filtradas.addListener((ListChangeListener<Fila>) c -> actualizarTotal());
    }

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String filtro = cmbFiltro.getValue();
        filtradas.setPredicate(fila -> {
            Vivienda v = fila.vivienda;
            boolean coincideTexto = texto.isEmpty()
                    || Formato.codigo("V", v.getId()).toLowerCase().contains(texto)
                    || v.getDireccion().toLowerCase().contains(texto)
                    || (v.getBarrio() != null && v.getBarrio().toLowerCase().contains(texto));
            boolean coincideFiltro = filtro == null || TODAS.equals(filtro)
                    || (SIN_HOGARES.equals(filtro) && fila.registrados == 0)
                    || (SIN_ENCUESTA.equals(filtro) && fila.sinEncuesta > 0);
            return coincideTexto && coincideFiltro;
        });
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(filtradas.size() + " de " + viviendas.size() + " viviendas");
    }

    /** Sector · sección · manzana · segmento. */
    static String cartografia(Vivienda v) {
        return String.join(" · ", guion(v.getSector()), guion(v.getSeccion()), guion(v.getManzana()), guion(v.getSegmento()));
    }

    /** A.4 Clase: 1 cabecera municipal, 2 centro poblado, 3 rural disperso. */
    public static String textoClase(Integer clase) {
        if (clase == null) {
            return "—";
        }
        switch (clase) {
            case 1:
                return "1. Cabecera municipal";
            case 2:
                return "2. Centro poblado";
            case 3:
                return "3. Rural disperso";
            default:
                return String.valueOf(clase);
        }
    }

    /** 4 → 004 */
    public static String tresDigitos(Integer numero) {
        return numero == null ? "—" : String.format("%03d", numero);
    }

    private static String textoHogares(int registrados) {
        return registrados == 1 ? "1 hogar" : registrados + " hogares";
    }

    private static Long id(Entidad entidad) {
        return entidad == null ? null : entidad.getId();
    }

    private static String guion(String valor) {
        return valor == null || valor.isEmpty() ? "—" : valor;
    }

    /** Vivienda del listado con el conteo de sus hogares. */
    private static final class Fila {
        private final Vivienda vivienda;
        private final int registrados;
        private final int sinEncuesta;

        private Fila(Vivienda vivienda, int registrados, int sinEncuesta) {
            this.vivienda = vivienda;
            this.registrados = registrados;
            this.sinEncuesta = sinEncuesta;
        }

        private String textoEncuestas() {
            if (registrados == 0) {
                return "Sin hogares aún";
            }
            if (sinEncuesta == 0) {
                return "Todos con encuesta";
            }
            return sinEncuesta == 1 ? "1 hogar sin encuesta" : sinEncuesta + " hogares sin encuesta";
        }
    }
}
