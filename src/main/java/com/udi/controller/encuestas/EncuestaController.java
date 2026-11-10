package com.udi.controller.encuestas;

import com.udi.componentes.SelectBuscable;
import com.udi.controller.viviendas.SelectoresUbicacion;
import com.udi.middleware.Rutas;
import com.udi.model.Departamento;
import com.udi.model.Encuesta;
import com.udi.model.EncuestaResumen;
import com.udi.model.Entidad;
import com.udi.model.Municipio;
import com.udi.model.Region;
import com.udi.model.Rol;
import com.udi.model.Vivienda;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.service.encuestas.EncuestaService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.RangoFechas;
import com.udi.util.Sesion;
import com.udi.util.Tareas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Listado de encuestas. El encuestador ve «Mis Encuestas» (las suyas, con el
 * avance de los 14 días); los demás roles ven todas con su encuestador. Todos
 * los filtros, también los de región, departamento y municipio de la vivienda,
 * se aplican sobre lo ya cargado.
 */
public class EncuestaController {

    private static final String TODOS = "Todos";

    /** Filtro por la fecha de inicio del periodo de 14 días. */
    private enum Periodo {
        TODOS("Todos"),
        EN_CURSO("En curso"),
        ESTE_MES("Iniciadas este mes"),
        MES_ANTERIOR("Iniciadas el mes anterior");

        private final String texto;

        Periodo(String texto) {
            this.texto = texto;
        }

        boolean incluye(Encuesta e) {
            LocalDate hoy = LocalDate.now();
            switch (this) {
                case EN_CURSO:
                    return !hoy.isBefore(e.getFechaDesde()) && !hoy.isAfter(e.getFechaHasta());
                case ESTE_MES:
                    return YearMonth.from(e.getFechaDesde()).equals(YearMonth.from(hoy));
                case MES_ANTERIOR:
                    return YearMonth.from(e.getFechaDesde()).equals(YearMonth.from(hoy).minusMonths(1));
                default:
                    return true;
            }
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtBuscar;
    @FXML
    private SelectBuscable<Region> cmbRegion;
    @FXML
    private SelectBuscable<Departamento> cmbDepartamento;
    @FXML
    private SelectBuscable<Municipio> cmbMunicipio;
    @FXML
    private ComboBox<String> cmbEstado;
    @FXML
    private VBox boxPeriodo;
    @FXML
    private ComboBox<Periodo> cmbPeriodo;
    @FXML
    private DatePicker dtpDesde;
    @FXML
    private DatePicker dtpHasta;
    @FXML
    private TableView<EncuestaResumen> tabla;
    @FXML
    private TableColumn<EncuestaResumen, String> colId;
    @FXML
    private TableColumn<EncuestaResumen, EncuestaResumen> colHogar;
    @FXML
    private TableColumn<EncuestaResumen, String> colRegion;
    @FXML
    private TableColumn<EncuestaResumen, String> colDepartamento;
    @FXML
    private TableColumn<EncuestaResumen, String> colMunicipio;
    @FXML
    private TableColumn<EncuestaResumen, String> colEncuestador;
    @FXML
    private TableColumn<EncuestaResumen, EstadoEncuesta> colEstado;
    @FXML
    private TableColumn<EncuestaResumen, String> colFecha;
    @FXML
    private TableColumn<EncuestaResumen, LocalDateTime> colCreacion;
    @FXML
    private TableColumn<EncuestaResumen, EncuestaResumen> colAvance;
    @FXML
    private TableColumn<EncuestaResumen, EncuestaResumen> colAcciones;
    @FXML
    private Label lblTotal;
    @FXML
    private Button btnNueva;

    public static final String VISTA_NUEVA = "modulos/encuestas/partials/nueva_encuesta";

    private final EncuestaService encuestaService = new EncuestaService();
    private final ObservableList<EncuestaResumen> encuestas = FXCollections.observableArrayList();
    private final FilteredList<EncuestaResumen> filtradas = new FilteredList<>(encuestas);

    /** El encuestador ve sus encuestas con el avance; los demás roles, la vista global. */
    private final boolean esEncuestador = Sesion.tieneRol(Rol.ENCUESTADOR);

    private SelectoresUbicacion ubicacion;
    /** Filtro por la fecha de creación de la encuesta. */
    private RangoFechas creacion;

    @FXML
    private void initialize() {
        Rutas.permitir(btnNueva, VISTA_NUEVA);
        if (esEncuestador) {
            lblTitulo.setText("Mis Encuestas");
            lblSubtitulo.setText("Seguimiento y captura de los cuadernillos que tienes asignados.");
            Formato.mostrar(boxPeriodo, false);
        }
        cmbEstado.getItems().add(TODOS);
        for (EstadoEncuesta estado : EstadoEncuesta.values()) {
            cmbEstado.getItems().add(estado.getDescripcion());
        }
        cmbPeriodo.getItems().setAll(Periodo.values());
        cmbRegion.setOpcionVacia("Todas");
        cmbDepartamento.setOpcionVacia("Todos");
        cmbMunicipio.setOpcionVacia("Todos");
        ubicacion = new SelectoresUbicacion(cmbRegion, cmbDepartamento, cmbMunicipio, false);
        creacion = new RangoFechas(dtpDesde, dtpHasta, this::filtrar);
        configurarTabla();

        txtBuscar.textProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbEstado.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbPeriodo.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbRegion.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbDepartamento.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbMunicipio.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        limpiar();

        Tareas.ejecutar(encuestaService::listarResumen, encuestas::setAll);
    }

    @FXML
    private void limpiar() {
        txtBuscar.clear();
        cmbEstado.setValue(TODOS);
        cmbPeriodo.setValue(Periodo.TODOS);
        ubicacion.limpiar();
        creacion.limpiar();
    }

    /** Nueva encuesta = seleccionar un hogar que todavía no tenga encuesta. */
    @FXML
    private void nuevaEncuesta() {
        Modal<NuevaEncuestaController> modal = Modal.cargar(VISTA_NUEVA, "Nueva encuesta");
        modal.controlador().preparar(creada ->
                EncuestaDetalleController.abrir(creada.getId(), EncuestaDetalleController.Pestana.HOGAR));
        modal.mostrar();
    }

    private void abrir(EncuestaResumen fila) {
        EncuestaDetalleController.abrir(fila.getEncuesta().getId(), EncuestaDetalleController.Pestana.VISITAS);
    }

    /** Día del periodo de 14 días en el que va la encuesta (0 si todavía no empieza). */
    static int diaActual(Encuesta e) {
        if (e.estaCerrada()) {
            return EncuestaService.DIAS_PERIODO;
        }
        if (e.getFechaDesde() == null) {
            return 0;
        }
        long dia = ChronoUnit.DAYS.between(e.getFechaDesde(), LocalDate.now()) + 1;
        return (int) Math.max(0, Math.min(EncuestaService.DIAS_PERIODO, dia));
    }

    private String textoAccion(Encuesta e) {
        if (e.estaCerrada()) {
            return "Ver";
        }
        if (!esEncuestador) {
            return "Ver / editar";
        }
        int dia = diaActual(e);
        return dia == 0 ? "Iniciar" : "Capturar día " + dia + " / Ver";
    }

    private void configurarTabla() {
        tabla.setItems(filtradas);
        tabla.setPlaceholder(new Label("Ninguna encuesta coincide con los filtros."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Columnas según el rol (maquetas «Mis Encuestas» y «Administrador · Encuestas»)
        colAvance.setVisible(esEncuestador);
        colEncuestador.setVisible(!esEncuestador);
        colFecha.setVisible(!esEncuestador);
        colHogar.setText(esEncuestador ? "Hogar / Dirección" : "Hogar");

        colId.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.codigo("E", d.getValue().getEncuesta().getId())));
        colHogar.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        colRegion.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRegion()));
        colDepartamento.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getDepartamento()));
        colMunicipio.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getMunicipio()));
        colEncuestador.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEncuestador()));
        colEstado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getEncuesta().getEstado()));
        colFecha.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.fecha(d.getValue().getEncuesta().getFechaDesde())));
        colCreacion.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getEncuesta().getCreatedAt()));
        colCreacion.setCellFactory(col -> RangoFechas.celdaFecha());
        colAvance.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        colAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));

        colHogar.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EncuestaResumen fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia || fila == null) {
                    setText(null);
                    return;
                }
                String codigo = Formato.codigo("H", fila.getEncuesta().getHogarId());
                setText(esEncuestador && !fila.getDireccion().isEmpty() ? codigo + " · " + fila.getDireccion() : codigo);
            }
        });

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoEncuesta estado, boolean vacia) {
                super.updateItem(estado, vacia);
                setGraphic(vacia || estado == null ? null : insigniaEstado(estado));
            }
        });

        colAvance.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar barra = new ProgressBar();
            private final Label texto = new Label();
            private final HBox caja = new HBox(8, barra, texto);

            {
                barra.getStyleClass().add("barra");
                barra.setPrefWidth(90);
                texto.getStyleClass().add("texto-suave");
                caja.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(EncuestaResumen fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia || fila == null) {
                    setGraphic(null);
                    return;
                }
                Encuesta e = fila.getEncuesta();
                int dia = diaActual(e);
                barra.setProgress(dia / (double) EncuestaService.DIAS_PERIODO);
                barra.getStyleClass().remove("barra-abierta");
                if (!e.estaCerrada()) {
                    barra.getStyleClass().add("barra-abierta");
                }
                texto.setText(e.estaCerrada() ? "Completo" : "Día " + dia + "/14");
                setGraphic(caja);
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink enlace = new Hyperlink();

            {
                enlace.getStyleClass().add("enlace");
                enlace.setOnAction(e -> abrir(getItem()));
            }

            @Override
            protected void updateItem(EncuestaResumen fila, boolean vacia) {
                super.updateItem(fila, vacia);
                if (vacia || fila == null) {
                    setGraphic(null);
                    return;
                }
                enlace.setText(textoAccion(fila.getEncuesta()));
                setGraphic(enlace);
            }
        });

        tabla.setRowFactory(t -> {
            TableRow<EncuestaResumen> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    abrir(fila.getItem());
                }
            });
            return fila;
        });

        filtradas.addListener((ListChangeListener<EncuestaResumen>) c -> actualizarTotal());
    }

    /** Insignia de color con el estado de la encuesta (Abierta, Cerrada, Reabierta). */
    public static Label insigniaEstado(EstadoEncuesta estado) {
        Label etiqueta = new Label(estado.getDescripcion());
        etiqueta.getStyleClass().addAll("insignia", "insignia-" + estado.name().toLowerCase());
        return etiqueta;
    }

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String estado = cmbEstado.getValue();
        Periodo periodo = cmbPeriodo.getValue() == null ? Periodo.TODOS : cmbPeriodo.getValue();
        Long regionId = id(cmbRegion.getValue());
        Long departamentoId = id(cmbDepartamento.getValue());
        Long municipioId = id(cmbMunicipio.getValue());
        filtradas.setPredicate(fila -> {
            Encuesta e = fila.getEncuesta();
            boolean coincideTexto = texto.isEmpty()
                    || Formato.codigo("E", e.getId()).toLowerCase().contains(texto)
                    || Formato.codigo("H", e.getHogarId()).toLowerCase().contains(texto)
                    || fila.getEncuestador().toLowerCase().contains(texto)
                    || fila.getDireccion().toLowerCase().contains(texto)
                    || (e.getInformanteNombre() != null && e.getInformanteNombre().toLowerCase().contains(texto));
            boolean coincideEstado = estado == null || TODOS.equals(estado)
                    || e.getEstado().getDescripcion().equals(estado);
            Vivienda v = fila.getVivienda();
            boolean coincideUbicacion = (regionId == null || (v != null && regionId.equals(v.getRegionId())))
                    && (departamentoId == null || (v != null && departamentoId.equals(v.getDepartamentoId())))
                    && (municipioId == null || (v != null && municipioId.equals(v.getMunicipioId())));
            return coincideTexto && coincideEstado && coincideUbicacion && periodo.incluye(e)
                    && creacion.incluye(e.getCreatedAt());
        });
        actualizarTotal();
    }

    private static Long id(Entidad entidad) {
        return entidad == null ? null : entidad.getId();
    }

    private void actualizarTotal() {
        lblTotal.setText(filtradas.size() + " de " + encuestas.size() + (esEncuestador ? " encuestas asignadas" : " encuestas"));
    }
}
