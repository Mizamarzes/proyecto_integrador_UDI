package com.udi.controller.reportes;

import com.udi.componentes.SelectBuscable;
import com.udi.controller.viviendas.SelectoresUbicacion;
import com.udi.model.Departamento;
import com.udi.model.Entidad;
import com.udi.model.FiltroReporte;
import com.udi.model.Municipio;
import com.udi.model.Reporte;
import com.udi.model.ReporteEncuestador;
import com.udi.model.ValorReporte;
import com.udi.service.reportes.ReporteService;
import com.udi.util.Alertas;
import com.udi.util.Formato;
import com.udi.util.LibroExcel;
import com.udi.util.RangoFechas;
import com.udi.util.Tareas;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Tablero del módulo «Reportes»: indicadores del operativo de campo, estado de
 * las encuestas, gasto por tipo, forma de adquisición y lugar de compra,
 * grupos de alimento del capítulo C y rendimiento por encuestador. Los filtros
 * (ubicación y fechas de inicio de la encuesta) se aplican juntos con «Filtrar».
 */
public class ReporteController {

    private static final String TODOS = "Todos";
    private static final int MAX_GRUPOS = 8;
    private static final int MAX_LUGARES = 6;
    private static final double ALTO_FILA = 40;
    /** Encuestadores por página en «Rendimiento por encuestador». */
    private static final int FILAS_POR_PAGINA = 10;

    @FXML
    private ScrollPane scroll;
    @FXML
    private Label lblAviso;
    @FXML
    private Button btnExportar;
    @FXML
    private SelectBuscable<com.udi.model.Region> cmbRegion;
    @FXML
    private SelectBuscable<Departamento> cmbDepartamento;
    @FXML
    private SelectBuscable<Municipio> cmbMunicipio;
    @FXML
    private DatePicker dtpDesde;
    @FXML
    private DatePicker dtpHasta;
    @FXML
    private Button btnFiltrar;
    @FXML
    private Label lblPendiente;
    @FXML
    private Label lblActualizado;
    @FXML
    private VBox boxVacio;
    @FXML
    private VBox boxDatos;
    @FXML
    private Label lblEncuestas;
    @FXML
    private Label lblEncuestasNota;
    @FXML
    private Label lblEnCurso;
    @FXML
    private Label lblEnCursoNota;
    @FXML
    private Label lblCerradas;
    @FXML
    private Label lblCerradasNota;
    @FXML
    private Label lblVisitas;
    @FXML
    private Label lblVisitasNota;
    @FXML
    private Label lblGasto;
    @FXML
    private Label lblGastoNota;
    @FXML
    private Label lblEstadoTotal;
    @FXML
    private HBox barraEstado;
    @FXML
    private VBox leyendaEstado;
    @FXML
    private HBox boxTipos;
    @FXML
    private HBox barraTipos;
    @FXML
    private Label lblArticulos;
    @FXML
    private Label lblComidas;
    @FXML
    private Label lblDomicilios;
    @FXML
    private VBox barrasGrupos;
    @FXML
    private VBox barrasFormas;
    @FXML
    private VBox barrasLugares;
    @FXML
    private TableView<ReporteEncuestador> tabla;
    @FXML
    private TableColumn<ReporteEncuestador, String> colNombre;
    @FXML
    private TableColumn<ReporteEncuestador, String> colTotal;
    @FXML
    private TableColumn<ReporteEncuestador, String> colAbiertas;
    @FXML
    private TableColumn<ReporteEncuestador, String> colCerradas;
    @FXML
    private TableColumn<ReporteEncuestador, String> colReabiertas;
    @FXML
    private TableColumn<ReporteEncuestador, String> colVisitas;
    @FXML
    private TableColumn<ReporteEncuestador, String> colGasto;
    @FXML
    private Label lblEncuestadores;
    @FXML
    private Label lblPagina;
    @FXML
    private Button btnPrimera;
    @FXML
    private Button btnAnterior;
    @FXML
    private Button btnSiguiente;
    @FXML
    private Button btnUltima;

    private final ReporteService reporteService = new ReporteService();

    private SelectoresUbicacion ubicacion;
    /** Inicio de la encuesta (A.21 fecha desde) entre estas fechas. */
    private RangoFechas fechas;
    private Reporte reporte;
    /** Página visible de la tabla de encuestadores, desde 0. */
    private int paginaActual;

    @FXML
    private void initialize() {
        cmbRegion.setOpcionVacia("Todas");
        cmbDepartamento.setOpcionVacia(TODOS);
        cmbMunicipio.setOpcionVacia(TODOS);
        ubicacion = new SelectoresUbicacion(cmbRegion, cmbDepartamento, cmbMunicipio, false);
        // Los filtros no consultan al cambiar: se aplican todos juntos con «Filtrar»
        fechas = new RangoFechas(dtpDesde, dtpHasta, this::marcarPendiente);
        cmbRegion.valueProperty().addListener((obs, antes, ahora) -> marcarPendiente());
        cmbDepartamento.valueProperty().addListener((obs, antes, ahora) -> marcarPendiente());
        cmbMunicipio.valueProperty().addListener((obs, antes, ahora) -> marcarPendiente());
        configurarTabla();
        Formato.mostrar(boxVacio, false);
        Formato.mostrar(boxDatos, false);
        btnExportar.setDisable(true);
        cargar();
    }

    /** Aplica todos los filtros y vuelve a cargar los indicadores, las gráficas y la tabla. */
    @FXML
    private void filtrar() {
        cargar();
    }

    /** Quita todos los filtros y carga el reporte completo. */
    @FXML
    private void limpiar() {
        ubicacion.limpiar();
        fechas.limpiar();
        cargar();
    }

    /** Avisa que hay filtros elegidos que todavía no se han aplicado. */
    private void marcarPendiente() {
        Formato.mostrar(lblPendiente, true);
    }

    private void cargar() {
        FiltroReporte filtro = new FiltroReporte(dtpDesde.getValue(), dtpHasta.getValue(),
                id(cmbRegion.getValue()), id(cmbDepartamento.getValue()), id(cmbMunicipio.getValue()));
        btnFiltrar.setDisable(true);
        Tareas.ejecutar(() -> reporteService.generar(filtro), r -> {
            btnFiltrar.setDisable(false);
            Formato.mostrar(lblPendiente, false);
            mostrar(r);
        });
    }

    private void mostrar(Reporte r) {
        reporte = r;
        lblAviso.setText("");
        lblActualizado.setText("Datos al " + Formato.fechaHora(LocalDateTime.now()));
        boolean vacio = r.estaVacio();
        Formato.mostrar(boxVacio, vacio);
        Formato.mostrar(boxDatos, !vacio);
        btnExportar.setDisable(vacio);
        if (vacio) {
            return;
        }
        mostrarIndicadores(r);
        mostrarEstado(r);
        mostrarTipos(r);
        mostrarGrupos(r);
        barras(barrasFormas, r.getFormas(), r.getGasto(), Formato::pesos, "Aún no hay gastos registrados.");
        barras(barrasLugares, lugares(r.getLugares()), suma(r.getLugares()), Formato::pesos,
                "Aún no hay compras registradas.");
        paginaActual = 0;
        mostrarPagina();
    }

    // ------------------------------------------------------------------
    // Paginación de «Rendimiento por encuestador» (en memoria: la consulta ya
    // trae una fila por encuestador). La fila Total suma a todos y se repite
    // al final de cada página.
    // ------------------------------------------------------------------

    private void mostrarPagina() {
        List<ReporteEncuestador> todos = reporte.getEncuestadores();
        int paginas = totalPaginas();
        paginaActual = Math.max(0, Math.min(paginaActual, paginas - 1));
        int desde = paginaActual * FILAS_POR_PAGINA;
        int hasta = Math.min(desde + FILAS_POR_PAGINA, todos.size());

        List<ReporteEncuestador> filas = new ArrayList<>(todos.subList(desde, hasta));
        filas.add(totalEncuestadores(reporte));
        tabla.getItems().setAll(filas);
        // Con varias páginas la tabla mantiene el alto de una página llena para que los botones no se muevan
        int visibles = paginas > 1 ? FILAS_POR_PAGINA : todos.size();
        tabla.setPrefHeight(ALTO_FILA * (visibles + 2) + 4);

        String sufijo = todos.size() == 1 ? " encuestador" : " encuestadores";
        lblEncuestadores.setText(paginas > 1
                ? "Mostrando " + (desde + 1) + "–" + hasta + " de " + todos.size() + sufijo
                : todos.size() + sufijo);
        lblPagina.setText("Página " + (paginaActual + 1) + " de " + paginas);
        btnPrimera.setDisable(paginaActual == 0);
        btnAnterior.setDisable(paginaActual == 0);
        btnSiguiente.setDisable(paginaActual >= paginas - 1);
        btnUltima.setDisable(paginaActual >= paginas - 1);
    }

    private int totalPaginas() {
        int n = reporte.getEncuestadores().size();
        return Math.max(1, (n + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
    }

    private static ReporteEncuestador totalEncuestadores(Reporte r) {
        return new ReporteEncuestador(null, "Total", r.getTotal(), r.getAbiertas(), r.getCerradas(),
                r.getReabiertas(), r.getVisitas(), r.getGasto());
    }

    @FXML
    private void primeraPagina() {
        irAPagina(0);
    }

    @FXML
    private void paginaAnterior() {
        irAPagina(paginaActual - 1);
    }

    @FXML
    private void paginaSiguiente() {
        irAPagina(paginaActual + 1);
    }

    @FXML
    private void ultimaPagina() {
        irAPagina(totalPaginas() - 1);
    }

    private void irAPagina(int nueva) {
        if (reporte != null) {
            paginaActual = nueva;
            mostrarPagina();
        }
    }

    private void mostrarIndicadores(Reporte r) {
        long total = r.getTotal();
        int encuestadores = r.getEncuestadores().size();
        lblEncuestas.setText(String.valueOf(total));
        lblEncuestasNota.setText(encuestadores == 1 ? "De 1 encuestador" : "De " + encuestadores + " encuestadores");
        lblEnCurso.setText(String.valueOf(r.getAbiertas() + r.getReabiertas()));
        lblEnCursoNota.setText(r.getAbiertas() + " abiertas · " + r.getReabiertas() + " reabiertas");
        lblCerradas.setText(String.valueOf(r.getCerradas()));
        lblCerradasNota.setText(porcentaje(r.getCerradas(), total) + " del total");
        lblVisitas.setText(String.valueOf(r.getVisitas()));
        lblVisitasNota.setText(String.format(Locale.forLanguageTag("es-CO"), "%.1f por encuesta",
                (double) r.getVisitas() / total));
        lblGasto.setText(Formato.pesos(r.getGasto()));
        lblGastoNota.setText("Promedio " + Formato.pesos(r.getGasto().divide(BigDecimal.valueOf(total),
                0, RoundingMode.HALF_UP)) + " por hogar");
    }

    private void mostrarEstado(Reporte r) {
        long total = r.getTotal();
        lblEstadoTotal.setText(total == 1 ? "1 encuesta en el filtro" : total + " encuestas en el filtro");
        List<Segmento> segmentos = List.of(
                new Segmento("Abierta", r.getAbiertas(), "reporte-azul"),
                new Segmento("Cerrada", r.getCerradas(), "reporte-verde"),
                new Segmento("Reabierta", r.getReabiertas(), "reporte-naranja"));
        segmentos(barraEstado, segmentos);
        leyendaEstado.getChildren().clear();
        for (Segmento s : segmentos) {
            Label nombre = new Label(s.nombre);
            HBox.setHgrow(nombre, Priority.ALWAYS);
            nombre.setMaxWidth(Double.MAX_VALUE);
            Label cantidad = new Label(String.valueOf(s.valor));
            cantidad.getStyleClass().add("dato-valor");
            Label pct = new Label(porcentaje(s.valor, total));
            pct.getStyleClass().add("texto-suave");
            pct.setMinWidth(48);
            pct.setStyle("-fx-alignment: CENTER-RIGHT;");
            HBox fila = new HBox(10, muestra(s.estilo), nombre, cantidad, pct);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("reporte-leyenda-fila");
            leyendaEstado.getChildren().add(fila);
        }
    }

    private void mostrarTipos(Reporte r) {
        BigDecimal total = r.getGasto();
        List<Segmento> segmentos = new ArrayList<>();
        boxTipos.getChildren().clear();
        String[] estilos = {"reporte-azul", "reporte-naranja"};
        String[] notas = {"Compras para todo el hogar", "Solo días 1 a 7 del periodo"};
        for (int i = 0; i < r.getTipos().size(); i++) {
            ValorReporte tipo = r.getTipos().get(i);
            String estilo = estilos[Math.min(i, estilos.length - 1)];
            segmentos.add(new Segmento(tipo.getNombre(), tipo.getValor().longValue(), estilo));
            Label nombre = new Label(tipo.getNombre());
            nombre.getStyleClass().add("etiqueta-campo");
            HBox titulo = new HBox(8, muestra(estilo), nombre);
            titulo.setAlignment(Pos.CENTER_LEFT);
            Label pct = new Label(porcentaje(tipo.getValor(), total));
            pct.getStyleClass().add("metrica-valor");
            Label valor = new Label(Formato.pesos(tipo.getValor()));
            valor.getStyleClass().add("dato-valor");
            Label nota = new Label(notas[Math.min(i, notas.length - 1)]);
            nota.getStyleClass().add("texto-suave");
            VBox caja = new VBox(3, titulo, pct, valor, nota);
            HBox.setHgrow(caja, Priority.ALWAYS);
            caja.setMaxWidth(Double.MAX_VALUE);
            caja.setPrefWidth(100);
            boxTipos.getChildren().add(caja);
        }
        segmentos(barraTipos, segmentos);
        lblArticulos.setText(r.getArticulos() + " gastos diarios");
        lblComidas.setText(r.getComidas() + " comidas fuera del hogar");
        lblDomicilios.setText(r.getDomicilios() + " a domicilio");
    }

    private void mostrarGrupos(Reporte r) {
        List<ValorReporte> grupos = r.getGrupos().subList(0, Math.min(MAX_GRUPOS, r.getGrupos().size()));
        barras(barrasGrupos, grupos, BigDecimal.valueOf(r.getTotal()),
                v -> v.longValue() == 1 ? "1 hogar" : v.longValue() + " hogares",
                "Ninguna encuesta tiene el capítulo C diligenciado.");
    }

    /** Los primeros lugares y el resto sumado como «Otros lugares». */
    private static List<ValorReporte> lugares(List<ValorReporte> todos) {
        if (todos.size() <= MAX_LUGARES + 1) {
            return todos;
        }
        List<ValorReporte> resultado = new ArrayList<>(todos.subList(0, MAX_LUGARES));
        resultado.add(new ValorReporte(null, "Otros lugares", suma(todos.subList(MAX_LUGARES, todos.size()))));
        return resultado;
    }

    // ------------------------------------------------------------------
    // Gráficas
    // ------------------------------------------------------------------

    /**
     * Barras horizontales de una sola serie: nombre, barra proporcional al
     * mayor valor y el valor con su porcentaje sobre {@code total}.
     */
    private static void barras(VBox caja, List<ValorReporte> valores, BigDecimal total,
                               Function<BigDecimal, String> formato, String vacio) {
        caja.getChildren().clear();
        BigDecimal maximo = valores.stream().map(ValorReporte::getValor).reduce(BigDecimal.ZERO, BigDecimal::max);
        if (maximo.signum() == 0) {
            Label aviso = new Label(vacio);
            aviso.getStyleClass().add("texto-suave");
            caja.getChildren().add(aviso);
            return;
        }
        for (ValorReporte v : valores) {
            Label codigo = new Label(v.getCodigo() == null ? "" : v.getCodigo());
            codigo.getStyleClass().addAll("texto-suave", "reporte-codigo");
            codigo.setMinWidth(v.getCodigo() == null ? 0 : Region.USE_PREF_SIZE);
            Label nombre = new Label(v.getNombre());
            nombre.setMinWidth(0);
            nombre.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nombre, Priority.ALWAYS);
            Label valor = new Label(formato.apply(v.getValor()) + "  " + porcentaje(v.getValor(), total));
            valor.getStyleClass().add("reporte-valor");
            valor.setMinWidth(Region.USE_PREF_SIZE);
            HBox encabezado = new HBox(6, codigo, nombre, valor);
            encabezado.setAlignment(Pos.CENTER_LEFT);

            // Pista a todo el ancho y relleno proporcional al mayor valor
            Pane pista = new Pane();
            pista.getStyleClass().add("reporte-pista");
            pista.setMinHeight(10);
            pista.setPrefHeight(10);
            pista.setMaxHeight(10);
            Region relleno = new Region();
            relleno.getStyleClass().addAll("reporte-barra", v.getCodigo() == null ? "reporte-gris" : "reporte-azul");
            relleno.setLayoutX(1);
            relleno.setPrefHeight(10);
            double fraccion = v.getValor().doubleValue() / maximo.doubleValue();
            relleno.prefWidthProperty().bind(pista.widthProperty().subtract(1).multiply(fraccion));
            pista.getChildren().add(relleno);

            VBox fila = new VBox(4, encabezado, pista);
            Tooltip.install(fila, new Tooltip(textoCodigo(v) + v.getNombre() + ": " + formato.apply(v.getValor())));
            caja.getChildren().add(fila);
        }
    }

    /** Barra apilada al 100 %: un segmento por categoría con valor, separados por 2 px. */
    private static void segmentos(HBox barra, List<Segmento> segmentos) {
        barra.getChildren().clear();
        long total = segmentos.stream().mapToLong(s -> s.valor).sum();
        List<Segmento> visibles = new ArrayList<>();
        for (Segmento s : segmentos) {
            if (s.valor > 0) {
                visibles.add(s);
            }
        }
        double espacios = barra.getSpacing() * Math.max(0, visibles.size() - 1);
        for (Segmento s : visibles) {
            Region parte = new Region();
            parte.getStyleClass().addAll("reporte-segmento", s.estilo);
            parte.setMinWidth(0);
            parte.prefWidthProperty().bind(barra.widthProperty().subtract(espacios).multiply((double) s.valor / total));
            Tooltip.install(parte, new Tooltip(s.nombre + ": " + porcentaje(s.valor, total)));
            barra.getChildren().add(parte);
        }
    }

    /** Cuadro de color de la leyenda. */
    private static Region muestra(String estilo) {
        Region cuadro = new Region();
        cuadro.getStyleClass().addAll("reporte-muestra", estilo);
        cuadro.setMinSize(12, 12);
        cuadro.setMaxSize(12, 12);
        return cuadro;
    }

    // ------------------------------------------------------------------
    // Tabla
    // ------------------------------------------------------------------

    private void configurarTabla() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setPlaceholder(new Label("Sin encuestas."));
        colNombre.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNombre()));
        numero(colTotal, ReporteEncuestador::getTotal);
        numero(colAbiertas, ReporteEncuestador::getAbiertas);
        numero(colCerradas, ReporteEncuestador::getCerradas);
        numero(colReabiertas, ReporteEncuestador::getReabiertas);
        numero(colVisitas, ReporteEncuestador::getVisitas);
        colGasto.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.pesos(d.getValue().getGasto())));
        colGasto.setStyle("-fx-alignment: CENTER-RIGHT;");
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(ReporteEncuestador fila, boolean vacia) {
                super.updateItem(fila, vacia);
                boolean esTotal = !vacia && fila != null && fila.getEncuestadorId() == null;
                getStyleClass().remove("reporte-fila-total");
                if (esTotal) {
                    getStyleClass().add("reporte-fila-total");
                }
            }
        });
    }

    private static void numero(TableColumn<ReporteEncuestador, String> columna,
                               Function<ReporteEncuestador, Long> valor) {
        columna.setCellValueFactory(d -> new ReadOnlyStringWrapper(String.valueOf(valor.apply(d.getValue()))));
        columna.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    // ------------------------------------------------------------------
    // Exportar a Excel
    // ------------------------------------------------------------------

    @FXML
    private void exportar() {
        if (reporte == null) {
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar reporte a Excel");
        selector.setInitialFileName("reporte_enph_" + LocalDate.now() + ".xlsx");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel", "*.xlsx"));
        File archivo = selector.showSaveDialog(scroll.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        if (!archivo.getName().toLowerCase().endsWith(".xlsx")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".xlsx");
        }
        try {
            libroExcel(reporte).guardar(archivo.toPath());
            lblAviso.setText("Exportado " + archivo.getName());
        } catch (IOException e) {
            Alertas.error("No se pudo exportar", "No se pudo escribir el archivo: " + e.getMessage());
        }
    }

    /** Una hoja de resumen con los filtros y una hoja por cada tabla o gráfica del tablero. */
    private LibroExcel libroExcel(Reporte r) {
        LibroExcel libro = new LibroExcel();
        long total = r.getTotal();

        libro.hoja("Resumen").anchos(32, 22)
                .filaNegrita("Reporte ENPH Urbana · Cuadernillo 2")
                .fila("Generado", Formato.fechaHora(LocalDateTime.now()))
                .vacia()
                .encabezado("Filtro", "Valor")
                .fila("Inicio desde", dtpDesde.getValue() == null ? "Sin límite" : Formato.fecha(dtpDesde.getValue()))
                .fila("Inicio hasta", dtpHasta.getValue() == null ? "Sin límite" : Formato.fecha(dtpHasta.getValue()))
                .fila("Región", nombre(cmbRegion.getValue(), "Todas"))
                .fila("Departamento", nombre(cmbDepartamento.getValue(), TODOS))
                .fila("Municipio", nombre(cmbMunicipio.getValue(), TODOS))
                .vacia()
                .encabezado("Indicador", "Valor")
                .fila("Encuestas", total)
                .fila("Abiertas", r.getAbiertas())
                .fila("Cerradas", r.getCerradas())
                .fila("Reabiertas", r.getReabiertas())
                .fila("Visitas realizadas", r.getVisitas())
                .fila("Gasto registrado", LibroExcel.pesos(r.getGasto()))
                .fila("Promedio por hogar", LibroExcel.pesos(
                        r.getGasto().divide(BigDecimal.valueOf(Math.max(1, total)), 0, RoundingMode.HALF_UP)))
                .vacia()
                .encabezado("Renglones registrados", "Cantidad")
                .fila("Gastos diarios (D)", r.getArticulos())
                .fila("Comidas fuera del hogar (D1)", r.getComidas())
                .fila("Comidas a domicilio", r.getDomicilios());

        LibroExcel.Hoja encuestadores = libro.hoja("Encuestadores").anchos(30, 12, 12, 12, 12, 12, 18)
                .encabezado("Encuestador", "Encuestas", "Abiertas", "Cerradas", "Reabiertas", "Visitas", "Gasto registrado");
        for (ReporteEncuestador e : r.getEncuestadores()) {
            encuestadores.fila(e.getNombre(), e.getTotal(), e.getAbiertas(), e.getCerradas(), e.getReabiertas(),
                    e.getVisitas(), LibroExcel.pesos(e.getGasto()));
        }
        encuestadores.filaNegrita("Total", total, r.getAbiertas(), r.getCerradas(), r.getReabiertas(),
                r.getVisitas(), LibroExcel.pesos(r.getGasto()));

        hojaValores(libro, "Gasto por tipo", "Tipo de gasto", "Valor", r.getTipos(), r.getGasto(), true);
        hojaValores(libro, "Formas de adquisición", "Forma de adquisición", "Valor", r.getFormas(), r.getGasto(), true);
        hojaValores(libro, "Lugares de compra", "Lugar de compra", "Valor", r.getLugares(), suma(r.getLugares()), true);
        hojaValores(libro, "Grupos de alimento", "Grupo de alimento (Cap. C)", "Hogares", r.getGrupos(),
                BigDecimal.valueOf(total), false);
        return libro;
    }

    /** Hoja de código, nombre, valor y porcentaje sobre {@code total}, con una fila de total si son pesos. */
    private static void hojaValores(LibroExcel libro, String hoja, String columna, String titulo,
                                    List<ValorReporte> valores, BigDecimal total, boolean pesos) {
        LibroExcel.Hoja h = libro.hoja(hoja).anchos(9, 60, 16, 12)
                .encabezado("Código", columna, titulo, pesos ? "% del total" : "% de hogares");
        for (ValorReporte v : valores) {
            h.fila(v.getCodigo(), v.getNombre(),
                    pesos ? LibroExcel.pesos(v.getValor()) : v.getValor().longValue(),
                    LibroExcel.porcentaje(fraccion(v.getValor(), total)));
        }
        if (pesos) {
            h.filaNegrita(null, "Total", LibroExcel.pesos(suma(valores)), LibroExcel.porcentaje(fraccion(suma(valores), total)));
        }
    }

    private static double fraccion(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? 0 : parte.doubleValue() / total.doubleValue();
    }

    // ------------------------------------------------------------------

    private static String porcentaje(long parte, long total) {
        return total == 0 ? "0 %" : Math.round(parte * 100.0 / total) + " %";
    }

    private static String porcentaje(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? "0 %" : Math.round(parte.doubleValue() * 100 / total.doubleValue()) + " %";
    }

    private static BigDecimal suma(List<ValorReporte> valores) {
        return valores.stream().map(ValorReporte::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String textoCodigo(ValorReporte v) {
        return v.getCodigo() == null ? "" : v.getCodigo() + ". ";
    }

    private static String nombre(com.udi.model.Region region, String vacio) {
        return region == null ? vacio : region.getNombre();
    }

    private static String nombre(Departamento departamento, String vacio) {
        return departamento == null ? vacio : departamento.getNombre();
    }

    private static String nombre(Municipio municipio, String vacio) {
        return municipio == null ? vacio : municipio.getNombre();
    }

    private static Long id(Entidad entidad) {
        return entidad == null ? null : entidad.getId();
    }

    /** Parte de una barra apilada. */
    private static final class Segmento {
        private final String nombre;
        private final long valor;
        private final String estilo;

        private Segmento(String nombre, long valor, String estilo) {
            this.nombre = nombre;
            this.valor = valor;
            this.estilo = estilo;
        }
    }
}
