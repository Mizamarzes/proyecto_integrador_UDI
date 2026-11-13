package com.udi.controller.verificacion;

import com.udi.componentes.Paginador;
import com.udi.componentes.SelectBuscable;
import com.udi.controller.encuestas.EncuestaDetalleController;
import com.udi.controller.viviendas.SelectoresUbicacion;
import com.udi.middleware.Rutas;
import com.udi.model.AuditoriaEncuesta;
import com.udi.model.CasoVerificacion;
import com.udi.model.Departamento;
import com.udi.model.Encuesta;
import com.udi.model.Entidad;
import com.udi.model.Municipio;
import com.udi.model.Region;
import com.udi.model.ReglaVerificacion;
import com.udi.model.Vivienda;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.service.verificacion.VerificacionService;
import com.udi.util.Alertas;
import com.udi.util.Formato;
import com.udi.util.Tareas;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Bandeja del crítico-digitador (RF-10). A la izquierda los casos cerrados,
 * rechazados y aprobados; a la derecha el caso elegido con su validación
 * cruzada, las acciones que permite su estado y su historial.
 */
public class VerificacionController {

    private static final String TODOS = "Todos";
    private static final String VISTA_ENCUESTA = "modulos/encuestas/encuesta_detalle";
    /** Ancho de página por debajo del cual el detalle del caso pasa debajo de la bandeja. */
    private static final double ANCHO_LADO_A_LADO = 1000;
    /** Alto del detalle cuando va al lado de la bandeja (luego tiene su propio scroll). */
    private static final double ALTO_DETALLE_LATERAL = 300;
    private static final double ANCHO_MAX_DETALLE = 480;

    @FXML
    private ScrollPane scroll;
    @FXML
    private VBox pagina;
    @FXML
    private HBox boxCasos;
    @FXML
    private ScrollPane scrollDetalle;
    @FXML
    private Label lblAviso;
    @FXML
    private Button btnExportarAprobadas;
    @FXML
    private Label lblPorVerificar;
    @FXML
    private Label lblAprobadas;
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
    private ComboBox<String> cmbEncuestador;
    @FXML
    private Label lblTotal;
    @FXML
    private TableView<CasoVerificacion> tabla;
    @FXML
    private TableColumn<CasoVerificacion, String> colCaso;
    @FXML
    private TableColumn<CasoVerificacion, CasoVerificacion> colHogar;
    @FXML
    private TableColumn<CasoVerificacion, String> colEncuestador;
    @FXML
    private TableColumn<CasoVerificacion, CasoVerificacion> colValidacion;
    @FXML
    private TableColumn<CasoVerificacion, EstadoEncuesta> colEstado;
    @FXML
    private Paginador<CasoVerificacion> paginador;
    @FXML
    private Label lblSinSeleccion;
    @FXML
    private VBox boxDetalle;
    @FXML
    private Label lblCodigo;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblHogar;
    @FXML
    private Label lblRegion;
    @FXML
    private Label lblDepartamento;
    @FXML
    private Label lblMunicipio;
    @FXML
    private Label lblDireccion;
    @FXML
    private Label lblBarrio;
    @FXML
    private Label lblEncuestador;
    @FXML
    private Label lblPeriodo;
    @FXML
    private Label lblCierre;
    @FXML
    private Label lblResumen;
    @FXML
    private VBox listaReglas;
    @FXML
    private Label lblEstadoCaso;
    @FXML
    private VBox boxObservacion;
    @FXML
    private TextArea txtObservacion;
    @FXML
    private Label lblError;
    @FXML
    private Button btnAbrir;
    @FXML
    private Button btnRechazar;
    @FXML
    private Button btnAprobar;
    @FXML
    private Button btnExportar;
    @FXML
    private VBox listaHistorial;

    private final VerificacionService verificacionService = new VerificacionService();
    private final ObservableList<CasoVerificacion> casos = FXCollections.observableArrayList();
    private final FilteredList<CasoVerificacion> filtrados = new FilteredList<>(casos);

    /** Caso que se muestra a la derecha; null si no hay ninguno elegido. */
    private CasoVerificacion seleccionado;
    /** Región, departamento y municipio encadenados (los mismos filtros del listado de encuestas). */
    private SelectoresUbicacion ubicacion;

    @FXML
    private void initialize() {
        cmbEstado.getItems().addAll(TODOS, texto(EstadoEncuesta.PENDIENTE_APROBACION),
                texto(EstadoEncuesta.RECHAZADA), texto(EstadoEncuesta.APROBADA));
        cmbEncuestador.getItems().add(TODOS);
        cmbRegion.setOpcionVacia("Todas");
        cmbDepartamento.setOpcionVacia("Todos");
        cmbMunicipio.setOpcionVacia("Todos");
        ubicacion = new SelectoresUbicacion(cmbRegion, cmbDepartamento, cmbMunicipio, false);
        Rutas.permitir(btnAbrir, VISTA_ENCUESTA);
        configurarTabla();
        configurarDisposicion();

        txtBuscar.textProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbEstado.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbEncuestador.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbRegion.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbDepartamento.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbMunicipio.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, antes, ahora) -> {
            if (ahora != null) {
                mostrar(ahora);
            }
        });
        limpiar();
        avisar(null);
        mostrar(null);
        cargar(null);
    }

    /** Carga la bandeja y vuelve a elegir el caso {@code encuestaId} (puede ser null). */
    private void cargar(Long encuestaId) {
        Tareas.ejecutar(verificacionService::listarCasos, lista -> {
            casos.setAll(lista);
            lblPorVerificar.setText(String.valueOf(contar(EstadoEncuesta.PENDIENTE_APROBACION)));
            lblAprobadas.setText(String.valueOf(contar(EstadoEncuesta.APROBADA)));
            btnExportarAprobadas.setDisable(contar(EstadoEncuesta.APROBADA) == 0);
            actualizarEncuestadores();
            CasoVerificacion caso = encuestaId == null ? null : casos.stream()
                    .filter(c -> c.getEncuesta().getId().equals(encuestaId)).findFirst().orElse(null);
            if (caso != null && tabla.getItems().contains(caso)) {
                tabla.getSelectionModel().select(caso);
            }
            mostrar(caso);
        });
    }

    @FXML
    private void limpiar() {
        txtBuscar.clear();
        cmbEstado.setValue(TODOS);
        cmbEncuestador.setValue(TODOS);
        ubicacion.limpiar();
    }

    // ------------------------------------------------------------------
    // Detalle del caso
    // ------------------------------------------------------------------

    private void mostrar(CasoVerificacion caso) {
        seleccionado = caso;
        Formato.mostrar(lblSinSeleccion, caso == null);
        Formato.mostrar(boxDetalle, caso != null);
        if (caso == null) {
            return;
        }
        Encuesta e = caso.getEncuesta();
        EstadoEncuesta estado = e.getEstado();
        lblCodigo.setText(Formato.codigo("E", e.getId()));
        pintarEstado(lblEstado, estado);
        Vivienda v = caso.getResumen().getVivienda();
        lblHogar.setText("Hogar " + Formato.codigo("H", e.getHogarId()));
        lblRegion.setText(v == null ? "Sin vivienda registrada" : "Región " + valor(v.getRegionNombre()));
        lblDepartamento.setText(v == null ? "—" : valor(v.getDepartamentoNombre()));
        lblMunicipio.setText(v == null ? "—" : valor(v.getMunicipioNombre()));
        lblDireccion.setText(v == null ? "—" : valor(v.getDireccion()));
        lblBarrio.setText(v == null ? "—" : valor(v.getBarrio()));
        lblEncuestador.setText(caso.getResumen().getEncuestador());
        lblPeriodo.setText(Formato.fecha(e.getFechaDesde()) + " – " + Formato.fecha(e.getFechaHasta()));
        lblCierre.setText(e.getFechaCierre() == null ? "Sin cerrar" : Formato.fechaHora(e.getFechaCierre()));

        lblResumen.setText(plural(caso.getErrores(), "error", "errores") + " · "
                + plural(caso.getAvisos(), "aviso", "avisos") + " · " + caso.getCorrectas() + " correctas");
        listaReglas.getChildren().clear();
        for (ReglaVerificacion r : caso.getReglas()) {
            listaReglas.getChildren().add(fila(r));
        }

        boolean porVerificar = estado == EstadoEncuesta.PENDIENTE_APROBACION;
        boolean bloqueado = porVerificar && caso.getErrores() > 0;
        lblEstadoCaso.getStyleClass().setAll("label");
        if (bloqueado) {
            lblEstadoCaso.setText("No se puede aprobar: " + plural(caso.getErrores(), "error", "errores")
                    + " por resolver. Rechaza el caso con una observación. Los avisos no bloquean.");
            lblEstadoCaso.getStyleClass().add("banner-error");
        } else if (porVerificar) {
            lblEstadoCaso.setText(caso.getAvisos() > 0
                    ? "Sin errores. Revisa los avisos contra el cuadernillo físico antes de aprobar."
                    : "Sin errores ni avisos: el caso se puede aprobar.");
            lblEstadoCaso.getStyleClass().add("banner-ok");
        } else if (estado == EstadoEncuesta.RECHAZADA) {
            lblEstadoCaso.setText("El caso fue rechazado y se puede editar desde la encuesta. Cuando se cierre"
                    + " de nuevo vuelve a esta bandeja como «Pendiente de aprobación».");
            lblEstadoCaso.getStyleClass().add("banner-alerta");
        } else {
            lblEstadoCaso.setText("Caso aprobado. Ya no admite cambios; puedes exportarlo con «Exportar a Excel».");
            lblEstadoCaso.getStyleClass().add("banner-ok");
        }
        Formato.mostrar(boxObservacion, porVerificar);
        txtObservacion.clear();
        Formato.mostrar(lblError, false);
        Formato.mostrar(btnRechazar, porVerificar);
        Formato.mostrar(btnAprobar, porVerificar);
        btnAprobar.setDisable(bloqueado);
        Formato.mostrar(btnExportar, estado == EstadoEncuesta.APROBADA);
        btnAbrir.setText(estado == EstadoEncuesta.RECHAZADA ? "Abrir encuesta para corregir" : "Ver encuesta");

        listaHistorial.getChildren().clear();
        long id = e.getId();
        Tareas.ejecutar(() -> verificacionService.historial(id), historial -> {
            if (seleccionado != caso) {
                return;
            }
            listaHistorial.getChildren().clear();
            if (historial.isEmpty()) {
                Label vacio = new Label("Sin registros.");
                vacio.getStyleClass().add("texto-suave");
                listaHistorial.getChildren().add(vacio);
            }
            for (AuditoriaEncuesta a : historial) {
                listaHistorial.getChildren().add(fila(a));
            }
        });
    }

    private static HBox fila(ReglaVerificacion r) {
        Label icono = new Label(r.getNivel() == ReglaVerificacion.Nivel.CORRECTA ? "✓"
                : r.getNivel() == ReglaVerificacion.Nivel.AVISO ? "!" : "✕");
        icono.getStyleClass().add(r.getNivel() == ReglaVerificacion.Nivel.CORRECTA ? "check-ok"
                : r.getNivel() == ReglaVerificacion.Nivel.AVISO ? "check-pendiente" : "check-error");
        Label titulo = new Label(r.getTitulo());
        titulo.getStyleClass().add("dato-valor");
        titulo.setWrapText(true);
        Label detalle = new Label(r.getDetalle());
        detalle.setWrapText(true);
        detalle.getStyleClass().add(r.getNivel() == ReglaVerificacion.Nivel.ERROR ? "texto-alerta" : "texto-suave");
        VBox textos = new VBox(2, titulo, detalle);
        HBox.setHgrow(textos, Priority.ALWAYS);
        Label nivel = new Label(r.getNivel().getDescripcion().toUpperCase());
        nivel.getStyleClass().add(claseValidacion(r.getNivel()));
        nivel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox fila = new HBox(10, icono, textos, nivel);
        fila.setAlignment(Pos.TOP_LEFT);
        fila.getStyleClass().add("check-fila");
        return fila;
    }

    private static VBox fila(AuditoriaEncuesta a) {
        String texto = a.getAccion().getDescripcion();
        if (a.getObservacion() != null) {
            texto += ": «" + a.getObservacion() + "»";
        }
        Label accion = new Label(texto + ".");
        accion.setWrapText(true);
        Label quien = new Label((a.getUsuarioNombre() == null ? "" : a.getUsuarioNombre() + " · ")
                + Formato.fechaHora(a.getCreatedAt()));
        quien.getStyleClass().add("texto-suave");
        return new VBox(2, accion, quien);
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    @FXML
    private void abrirEncuesta() {
        if (seleccionado != null) {
            EncuestaDetalleController.abrir(seleccionado.getEncuesta().getId(), EncuestaDetalleController.Pestana.VISITAS);
        }
    }

    @FXML
    private void rechazar() {
        if (seleccionado == null) {
            return;
        }
        String observacion = Formato.texto(txtObservacion.getText());
        if (observacion == null) {
            lblError.setText("Escribe la observación para el encuestador antes de rechazar el caso.");
            Formato.mostrar(lblError, true);
            txtObservacion.requestFocus();
            return;
        }
        Encuesta e = seleccionado.getEncuesta();
        String codigo = Formato.codigo("E", e.getId());
        if (!Alertas.confirmar("Rechazar caso", "La encuesta " + codigo + " pasará a «Rechazada» y "
                + seleccionado.getResumen().getEncuestador() + " podrá corregirla. ¿Desea rechazarla?")) {
            return;
        }
        long id = e.getId();
        Tareas.accion(() -> verificacionService.rechazar(id, observacion), () -> {
            avisar(codigo + " pasó a «Rechazada». Cuando el encuestador la cierre de nuevo, vuelve a esta bandeja.");
            cargar(id);
        });
    }

    /** Aprueba el caso. El Excel es aparte: «Exportar a Excel», disponible cuando el caso ya está aprobado. */
    @FXML
    private void aprobar() {
        if (seleccionado == null) {
            return;
        }
        long id = seleccionado.getEncuesta().getId();
        String codigo = Formato.codigo("E", id);
        if (!Alertas.confirmar("Aprobar caso", "La encuesta " + codigo + " quedará «Aprobada» y ya no admitirá cambios."
                + " ¿Desea aprobarla?")) {
            return;
        }
        Tareas.accion(() -> verificacionService.aprobar(id), () -> {
            avisar(codigo + " quedó «Aprobada». Puedes exportarla con «Exportar a Excel».");
            cargar(id);
        });
    }

    /** Guarda el Excel de un caso aprobado (solo ese caso). */
    @FXML
    private void exportar() {
        if (seleccionado == null) {
            return;
        }
        long id = seleccionado.getEncuesta().getId();
        Path archivo = elegirArchivo(id);
        if (archivo != null) {
            Tareas.accion(() -> verificacionService.exportar(id, archivo),
                    () -> avisar("Se guardó " + archivo + " con los capítulos A, B, C, D, D1 y D2."));
        }
    }

    /**
     * Exporta en un solo libro todas las encuestas aprobadas de la región,
     * departamento y municipio elegidos (los demás filtros no se aplican). La
     * lista la arma el procedimiento pr_exportar_aprobadas; no cambia estados.
     */
    @FXML
    private void exportarAprobadas() {
        Long regionId = id(cmbRegion.getValue());
        Long departamentoId = id(cmbDepartamento.getValue());
        Long municipioId = id(cmbMunicipio.getValue());
        boolean filtrado = regionId != null || departamentoId != null || municipioId != null;
        long aprobadas = casos.stream().filter(c -> c.getEncuesta().getEstado() == EstadoEncuesta.APROBADA
                && coincideUbicacion(c, regionId, departamentoId, municipioId)).count();
        if (aprobadas == 0) {
            Alertas.info("Exportar aprobadas", "No hay encuestas aprobadas"
                    + (filtrado ? " en la ubicación elegida" : "") + " para exportar.");
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar encuestas aprobadas a Excel");
        selector.setInitialFileName(VerificacionService.nombreArchivoAprobadas());
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel", "*.xlsx"));
        File elegido = selector.showSaveDialog(scroll.getScene().getWindow());
        if (elegido == null) {
            return;
        }
        Path archivo = elegido.toPath();
        Tareas.ejecutar(() -> verificacionService.exportarAprobadas(regionId, departamentoId, municipioId, archivo),
                total -> avisar("Se guardó " + archivo + " con " + plural(total, "encuesta aprobada", "encuestas aprobadas")
                        + (filtrado ? " de la ubicación elegida" : "")
                        + ": resumen y capítulos B, C, D y D1."));
    }

    private Path elegirArchivo(long id) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar caso a Excel");
        selector.setInitialFileName(VerificacionService.nombreArchivo(id));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel", "*.xlsx"));
        File archivo = selector.showSaveDialog(scroll.getScene().getWindow());
        return archivo == null ? null : archivo.toPath();
    }

    private void avisar(String mensaje) {
        lblAviso.setText(mensaje);
        Formato.mostrar(lblAviso, mensaje != null);
    }

    // ------------------------------------------------------------------
    // Tabla y filtros
    // ------------------------------------------------------------------

    /**
     * La página ocupa al menos el alto visible, así la bandeja crece en pantalla
     * completa; si la ventana es más baja que el contenido, la página hace scroll
     * en lugar de aplastar la tabla. En ventanas angostas el detalle del caso va
     * debajo de la bandeja para que la tabla use todo el ancho.
     */
    private void configurarDisposicion() {
        pagina.minHeightProperty().bind(Bindings.createDoubleBinding(
                () -> scroll.getViewportBounds().getHeight(), scroll.viewportBoundsProperty()));
        scroll.viewportBoundsProperty().addListener((obs, antes, ahora) -> acomodarDetalle(ahora.getWidth()));
    }

    private void acomodarDetalle(double ancho) {
        boolean ladoALado = ancho >= ANCHO_LADO_A_LADO;
        if (ladoALado == boxCasos.getChildren().contains(scrollDetalle)) {
            return;
        }
        if (ladoALado) {
            pagina.getChildren().remove(scrollDetalle);
            scrollDetalle.setMaxWidth(ANCHO_MAX_DETALLE);
            scrollDetalle.setPrefViewportHeight(ALTO_DETALLE_LATERAL);
            boxCasos.getChildren().add(scrollDetalle);
        } else {
            // Debajo de la tabla: ancho completo y alto de su contenido (scroll de la página)
            boxCasos.getChildren().remove(scrollDetalle);
            scrollDetalle.setMaxWidth(Double.MAX_VALUE);
            scrollDetalle.setPrefViewportHeight(0);
            pagina.getChildren().add(scrollDetalle);
        }
    }

    private void configurarTabla() {
        paginador.vincular(tabla, filtrados, "caso", "casos");
        tabla.setPlaceholder(new Label("Ningún caso coincide con los filtros."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCaso.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.codigo("E", d.getValue().getEncuesta().getId())));
        colHogar.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        colEncuestador.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getResumen().getEncuestador()));
        colValidacion.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        colEstado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getEncuesta().getEstado()));

        colHogar.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CasoVerificacion caso, boolean vacia) {
                super.updateItem(caso, vacia);
                setText(vacia || caso == null ? null : Formato.codigo("H", caso.getEncuesta().getHogarId())
                        + (caso.getResumen().getDireccion().isEmpty() ? "" : " · " + caso.getResumen().getDireccion()));
            }
        });

        colValidacion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CasoVerificacion caso, boolean vacia) {
                super.updateItem(caso, vacia);
                if (vacia || caso == null) {
                    setGraphic(null);
                    return;
                }
                ReglaVerificacion.Nivel nivel = caso.getErrores() > 0 ? ReglaVerificacion.Nivel.ERROR
                        : caso.getAvisos() > 0 ? ReglaVerificacion.Nivel.AVISO : ReglaVerificacion.Nivel.CORRECTA;
                Label texto = new Label(caso.getErrores() > 0 ? plural(caso.getErrores(), "error", "errores")
                        : caso.getAvisos() > 0 ? plural(caso.getAvisos(), "aviso", "avisos") : "Sin errores");
                texto.getStyleClass().add(claseValidacion(nivel));
                setGraphic(texto);
            }
        });

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoEncuesta estado, boolean vacia) {
                super.updateItem(estado, vacia);
                if (vacia || estado == null) {
                    setGraphic(null);
                    return;
                }
                Label insignia = new Label();
                pintarEstado(insignia, estado);
                setGraphic(insignia);
            }
        });

        filtrados.addListener((ListChangeListener<CasoVerificacion>) c -> actualizarTotal());
    }

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String estado = cmbEstado.getValue();
        String encuestador = cmbEncuestador.getValue();
        Long regionId = id(cmbRegion.getValue());
        Long departamentoId = id(cmbDepartamento.getValue());
        Long municipioId = id(cmbMunicipio.getValue());
        filtrados.setPredicate(caso -> {
            Encuesta e = caso.getEncuesta();
            Vivienda v = caso.getResumen().getVivienda();
            boolean coincideTexto = texto.isEmpty()
                    || Formato.codigo("E", e.getId()).toLowerCase().contains(texto)
                    || Formato.codigo("H", e.getHogarId()).toLowerCase().contains(texto)
                    || caso.getResumen().getDireccion().toLowerCase().contains(texto)
                    || caso.getResumen().getMunicipio().toLowerCase().contains(texto)
                    || (v != null && v.getBarrio() != null && v.getBarrio().toLowerCase().contains(texto));
            boolean coincideUbicacion = coincideUbicacion(caso, regionId, departamentoId, municipioId);
            boolean coincideEstado = estado == null || TODOS.equals(estado) || texto(e.getEstado()).equals(estado);
            boolean coincideEncuestador = encuestador == null || TODOS.equals(encuestador)
                    || caso.getResumen().getEncuestador().equals(encuestador);
            return coincideTexto && coincideUbicacion && coincideEstado && coincideEncuestador;
        });
        actualizarTotal();
    }

    private static boolean coincideUbicacion(CasoVerificacion caso, Long regionId, Long departamentoId, Long municipioId) {
        Vivienda v = caso.getResumen().getVivienda();
        return (regionId == null || (v != null && regionId.equals(v.getRegionId())))
                && (departamentoId == null || (v != null && departamentoId.equals(v.getDepartamentoId())))
                && (municipioId == null || (v != null && municipioId.equals(v.getMunicipioId())));
    }

    private void actualizarEncuestadores() {
        String actual = cmbEncuestador.getValue();
        TreeSet<String> nombres = new TreeSet<>();
        for (CasoVerificacion c : casos) {
            nombres.add(c.getResumen().getEncuestador());
        }
        List<String> opciones = new ArrayList<>();
        opciones.add(TODOS);
        opciones.addAll(nombres);
        cmbEncuestador.getItems().setAll(opciones);
        cmbEncuestador.setValue(opciones.contains(actual) ? actual : TODOS);
    }

    private void actualizarTotal() {
        lblTotal.setText(filtrados.size() + " de " + casos.size() + " casos");
    }

    private long contar(EstadoEncuesta estado) {
        return casos.stream().filter(c -> c.getEncuesta().getEstado() == estado).count();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static String texto(EstadoEncuesta estado) {
        return estado.getDescripcion();
    }

    private static void pintarEstado(Label etiqueta, EstadoEncuesta estado) {
        etiqueta.setText(texto(estado));
        etiqueta.getStyleClass().setAll("label", "insignia",
                "insignia-" + estado.name().toLowerCase());
    }

    private static String claseValidacion(ReglaVerificacion.Nivel nivel) {
        return nivel == ReglaVerificacion.Nivel.ERROR ? "validacion-error"
                : nivel == ReglaVerificacion.Nivel.AVISO ? "validacion-aviso" : "validacion-ok";
    }

    private static Long id(Entidad entidad) {
        return entidad == null ? null : entidad.getId();
    }

    /** El texto, o «Sin registrar» si está vacío. */
    private static String valor(String texto) {
        return Formato.texto(texto) == null ? "Sin registrar" : texto.trim();
    }

    private static String plural(long n, String uno, String varios) {
        return n + " " + (n == 1 ? uno : varios);
    }
}
