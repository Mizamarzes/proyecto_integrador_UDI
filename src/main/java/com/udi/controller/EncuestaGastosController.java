package com.udi.controller;

import com.udi.middleware.RutasModulo;
import com.udi.model.ComidaFueraHogar;
import com.udi.model.Encuesta;
import com.udi.model.GastoDiario;
import com.udi.model.LugarCompra;
import com.udi.model.RegistroDiario;
import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.model.enums.TipoGasto;
import com.udi.model.enums.UnidadMedida;
import com.udi.service.CatalogoService;
import com.udi.service.EncuestaService;
import com.udi.service.GastoDiarioService;
import com.udi.util.Alertas;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Pestaña «Línea de 14 días». Muestra los 14 días del periodo; al elegir un
 * día se ven sus renglones de la tabla D (gastos diarios) y D1 (comidas
 * preparadas fuera del hogar). Los renglones se agregan con un modal.
 */
public class EncuestaGastosController implements SeccionEncuesta {

    @FXML
    private HBox linea;
    @FXML
    private HBox boxCodigos;
    @FXML
    private Label lblDia;
    @FXML
    private Label lblRenglones;
    @FXML
    private VBox boxSinRegistro;
    @FXML
    private Label lblSinRegistro;
    @FXML
    private Button btnCrearDia;
    @FXML
    private VBox boxConRegistro;
    @FXML
    private Button btnAgregarGasto;
    @FXML
    private Button btnAgregarComida;
    @FXML
    private TextArea txtObservaciones;
    @FXML
    private Button btnObservaciones;

    @FXML
    private TableView<GastoDiario> tablaD;
    @FXML
    private TableColumn<GastoDiario, String> dOrden;
    @FXML
    private TableColumn<GastoDiario, String> dArticulo;
    @FXML
    private TableColumn<GastoDiario, String> dCantidad;
    @FXML
    private TableColumn<GastoDiario, String> dUnidad;
    @FXML
    private TableColumn<GastoDiario, String> dEquivalencia;
    @FXML
    private TableColumn<GastoDiario, String> dForma;
    @FXML
    private TableColumn<GastoDiario, String> dLugar;
    @FXML
    private TableColumn<GastoDiario, String> dValor;
    @FXML
    private TableColumn<GastoDiario, String> dFrecuencia;
    @FXML
    private TableColumn<GastoDiario, String> dTipo;
    @FXML
    private TableColumn<GastoDiario, GastoDiario> dAcciones;

    @FXML
    private TableView<ComidaFueraHogar> tablaD1;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mOrden;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mDescripcion;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mTipoComida;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mCantidad;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mForma;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mLugar;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mValor;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mFrecuencia;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mTipo;
    @FXML
    private TableColumn<ComidaFueraHogar, String> mDomicilio;
    @FXML
    private TableColumn<ComidaFueraHogar, ComidaFueraHogar> mAcciones;

    private final GastoDiarioService gastoService = new GastoDiarioService();
    private final CatalogoService catalogoService = new CatalogoService();
    private final ToggleGroup grupoDias = new ToggleGroup();

    private EncuestaDetalleController detalle;
    private List<LugarCompra> lugares = new ArrayList<>();
    private final Map<Long, String> nombresLugares = new HashMap<>();
    /** Registro de cada día (por número de día) y sus renglones con información. */
    private final Map<Integer, RegistroDiario> registros = new HashMap<>();
    private final Map<Integer, Long> renglones = new HashMap<>();
    private int dia = 1;
    /** Mientras se vuelve a pintar la línea no se atienden los cambios de selección. */
    private boolean pintando;

    @FXML
    private void initialize() {
        configurarTablaD();
        configurarTablaD1();
        grupoDias.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (pintando) {
                return;
            }
            if (ahora == null) {
                grupoDias.selectToggle(antes);
            } else if ((int) ahora.getUserData() != dia) {
                dia = (int) ahora.getUserData();
                mostrarDia();
            }
        });
        Formato.mostrar(boxSinRegistro, false);
        Formato.mostrar(boxConRegistro, false);
    }

    @Override
    public void mostrar(EncuestaDetalleController detalle) {
        this.detalle = detalle;
        boolean editable = detalle.esEditable();
        // Cada acción exige el módulo de su captura: D (gastos-diarios) y D1 (comidas-fuera-hogar)
        boolean editaDia = detalle.puedeEditar(RutasModulo.LINEA_14_DIAS);
        Formato.mostrar(btnCrearDia, editaDia);
        Formato.mostrar(btnAgregarGasto, detalle.puedeEditar(RutasModulo.GASTOS_DIARIOS));
        Formato.mostrar(btnAgregarComida, detalle.puedeEditar(RutasModulo.COMIDAS));
        Formato.mostrar(btnObservaciones, editaDia);
        txtObservaciones.setEditable(editaDia);
        if (!editable) {
            lblSinRegistro.setText("La encuesta está cerrada: no se pueden crear registros nuevos.");
        }
        dia = diaInicial(detalle.getEncuesta());
        cargar();
    }

    /** Día que se muestra al entrar: el de hoy dentro del periodo, o el último si ya terminó. */
    private static int diaInicial(Encuesta e) {
        long transcurridos = ChronoUnit.DAYS.between(e.getFechaDesde(), LocalDate.now()) + 1;
        return (int) Math.max(1, Math.min(EncuestaService.DIAS_PERIODO, transcurridos));
    }

    /** Lee los registros del periodo y el número de renglones de cada día. */
    private void cargar() {
        long encuestaId = detalle.getEncuesta().getId();
        boolean conLugares = !lugares.isEmpty();
        Tareas.ejecutar(() -> {
            Datos datos = new Datos();
            datos.registros = gastoService.listarRegistros(encuestaId);
            for (RegistroDiario r : datos.registros) {
                datos.renglones.put(r.getNumeroDia(), gastoService.contarRenglones(r.getId()));
            }
            if (!conLugares) {
                datos.lugares = catalogoService.listarLugaresCompra();
            }
            return datos;
        }, datos -> {
            registros.clear();
            datos.registros.forEach(r -> registros.put(r.getNumeroDia(), r));
            renglones.clear();
            renglones.putAll(datos.renglones);
            if (datos.lugares != null) {
                lugares = datos.lugares;
                lugares.forEach(l -> nombresLugares.put(l.getId(), l.getNombre()));
                pintarCodigos();
            }
            pintarLinea();
            mostrarDia();
        });
    }

    private LocalDate fechaDel(int numero) {
        return detalle.getEncuesta().getFechaDesde().plusDays(numero - 1);
    }

    private void pintarLinea() {
        pintando = true;
        linea.getChildren().clear();
        grupoDias.getToggles().clear();
        LocalDate hoy = LocalDate.now();
        for (int n = 1; n <= EncuestaService.DIAS_PERIODO; n++) {
            LocalDate fecha = fechaDel(n);
            boolean futuro = fecha.isAfter(hoy);
            RegistroDiario registro = registros.get(n);
            String estado;
            if (registro != null) {
                long cantidad = renglones.getOrDefault(n, 0L);
                estado = cantidad + (cantidad == 1 ? " renglón" : " reng.");
            } else if (fecha.equals(hoy)) {
                estado = "Hoy";
            } else {
                estado = futuro ? "—" : "Sin registro";
            }
            Label numero = new Label("Día " + n);
            numero.getStyleClass().add("dia-numero");
            Label corta = new Label(Formato.fechaCorta(fecha));
            corta.getStyleClass().add("dia-fecha");
            Label texto = new Label(estado);
            texto.getStyleClass().add("dia-estado");
            VBox contenido = new VBox(2, numero, corta, texto);
            contenido.setAlignment(Pos.CENTER);

            ToggleButton boton = new ToggleButton();
            boton.setGraphic(contenido);
            boton.setUserData(n);
            boton.getStyleClass().add("dia");
            if (registro != null) {
                boton.getStyleClass().add("dia-registrado");
            }
            if (fecha.equals(hoy)) {
                boton.getStyleClass().add("dia-hoy");
            }
            boton.setDisable(futuro);
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setMinWidth(54);
            HBox.setHgrow(boton, Priority.ALWAYS);
            boton.setToggleGroup(grupoDias);
            boton.setSelected(n == dia);
            linea.getChildren().add(boton);
        }
        pintando = false;
    }

    private void pintarCodigos() {
        List<String> unidades = new ArrayList<>();
        for (UnidadMedida u : UnidadMedida.values()) {
            unidades.add(u.getCodigo() + "  " + u.getDescripcion() + (u == UnidadMedida.OTRA ? ", ¿cuál?" : ""));
        }
        List<String> formas = new ArrayList<>();
        for (FormaAdquisicion f : FormaAdquisicion.values()) {
            formas.add(f.getCodigo() + "  " + f.getDescripcion() + (f == FormaAdquisicion.OTRA ? ", ¿cuál?" : ""));
        }
        List<String> lugaresTexto = new ArrayList<>();
        for (LugarCompra l : lugares) {
            lugaresTexto.add(l.getId() + "  " + l.getNombre());
        }
        List<String> frecuencias = new ArrayList<>();
        for (FrecuenciaCompra f : FrecuenciaCompra.values()) {
            if (f != FrecuenciaCompra.NUNCA) {
                frecuencias.add(f.getCodigo() + "  " + f.getDescripcion());
            }
        }
        frecuencias.add("(1 Nunca: solo en el capítulo C)");
        boxCodigos.getChildren().setAll(
                columnaCodigos("(5.2) Unidad de medida", unidades),
                columnaCodigos("(6) Forma de adquisición", formas),
                columnaCodigos("(7) Lugar de compra", lugaresTexto),
                columnaCodigos("(9) Frecuencia de compra", frecuencias));
    }

    private static VBox columnaCodigos(String titulo, List<String> lineas) {
        Label encabezado = new Label(titulo);
        encabezado.getStyleClass().add("dato-valor");
        VBox columna = new VBox(3, encabezado);
        for (String texto : lineas) {
            columna.getChildren().add(new Label(texto));
        }
        HBox.setHgrow(columna, Priority.ALWAYS);
        return columna;
    }

    /** Muestra el encabezado del día elegido y sus renglones. */
    private void mostrarDia() {
        LocalDate fecha = fechaDel(dia);
        String larga = Formato.fechaLarga(fecha);
        lblDia.setText("Día " + dia + " · " + Character.toUpperCase(larga.charAt(0)) + larga.substring(1));
        lblRenglones.setText(String.valueOf(renglones.getOrDefault(dia, 0L)));
        btnCrearDia.setText("+ Crear registro del día " + dia);
        RegistroDiario registro = registros.get(dia);
        Formato.mostrar(boxSinRegistro, registro == null);
        Formato.mostrar(boxConRegistro, registro != null);
        if (registro == null) {
            return;
        }
        txtObservaciones.setText(registro.getObservaciones());
        long registroId = registro.getId();
        Tareas.ejecutar(() -> {
            Renglones r = new Renglones();
            r.gastos = gastoService.listarGastos(registroId);
            r.comidas = gastoService.listarComidas(registroId);
            return r;
        }, r -> {
            tablaD.getItems().setAll(r.gastos);
            tablaD1.getItems().setAll(r.comidas);
        });
    }

    @FXML
    private void crearDia() {
        RegistroDiario nuevo = new RegistroDiario();
        nuevo.setEncuestaId(detalle.getEncuesta().getId());
        nuevo.setNumeroDia(dia);
        nuevo.setFecha(fechaDel(dia));
        Tareas.ejecutar(() -> gastoService.crearRegistro(nuevo), creado -> {
            detalle.avisar("Registro del día " + dia + " creado. Ya puede agregar sus gastos y comidas.");
            cargar();
        });
    }

    @FXML
    private void agregarGasto() {
        abrirRenglon(true);
    }

    @FXML
    private void agregarComida() {
        abrirRenglon(false);
    }

    private void abrirRenglon(boolean esGasto) {
        RegistroDiario registro = registros.get(dia);
        int siguiente = esGasto
                ? tablaD.getItems().stream().mapToInt(GastoDiario::getNumeroOrden).max().orElse(0) + 1
                : tablaD1.getItems().stream().mapToInt(ComidaFueraHogar::getNumeroOrden).max().orElse(0) + 1;
        Modal<RenglonFormController> modal = Modal.cargar("modulos/encuestas/partials/renglon_form",
                esGasto ? "Agregar gasto" : "Agregar comida");
        Consumer<String> alGuardar = mensaje -> {
            detalle.avisar(mensaje);
            cargar();
        };
        if (esGasto) {
            modal.controlador().gasto(registro, siguiente, lugares, alGuardar);
        } else {
            modal.controlador().comida(registro, siguiente, lugares, alGuardar);
        }
        modal.mostrar();
    }

    @FXML
    private void guardarObservaciones() {
        RegistroDiario registro = registros.get(dia);
        registro.setObservaciones(Formato.texto(txtObservaciones.getText()));
        Tareas.ejecutar(() -> gastoService.actualizarRegistro(registro),
                guardado -> detalle.avisar("Observaciones del día " + dia + " guardadas."));
    }

    private void eliminarGasto(GastoDiario gasto) {
        if (Alertas.confirmar("Eliminar gasto", "¿Eliminar el renglón " + gasto.getNumeroOrden() + " («"
                + gasto.getNombreArticulo() + "») de la tabla D?")) {
            Tareas.accion(() -> gastoService.eliminarGasto(gasto.getId()), () -> {
                detalle.avisar("Renglón " + gasto.getNumeroOrden() + " de la tabla D eliminado.");
                cargar();
            });
        }
    }

    private void eliminarComida(ComidaFueraHogar comida) {
        if (Alertas.confirmar("Eliminar comida", "¿Eliminar el renglón " + comida.getNumeroOrden() + " («"
                + comida.getDescripcion() + "») de la tabla D1?")) {
            Tareas.accion(() -> gastoService.eliminarComida(comida.getId()), () -> {
                detalle.avisar("Renglón " + comida.getNumeroOrden() + " de la tabla D1 eliminado.");
                cargar();
            });
        }
    }

    // ------------------------------------------------------------------
    // Tablas
    // ------------------------------------------------------------------

    private void configurarTablaD() {
        tablaD.setPlaceholder(new Label("Sin gastos registrados en la tabla D para este día."));
        tablaD.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        texto(dOrden, g -> String.valueOf(g.getNumeroOrden()));
        texto(dArticulo, g -> g.getNombreArticulo() + (g.getBeneficiarioMesada() == null ? "" : " (mesada: " + g.getBeneficiarioMesada() + ")"));
        texto(dCantidad, g -> Formato.numero(g.getCantidad()));
        texto(dUnidad, g -> g.getUnidadMedida() == UnidadMedida.OTRA ? g.getUnidadMedidaOtra() : g.getUnidadMedida().getDescripcion());
        texto(dEquivalencia, g -> g.getEquivalenciaGr() != null ? Formato.numero(g.getEquivalenciaGr()) + " g"
                : g.getEquivalenciaCm3() != null ? Formato.numero(g.getEquivalenciaCm3()) + " cm³" : "—");
        texto(dForma, g -> forma(g.getFormaAdquisicion(), g.getFormaAdquisicionOtra()));
        texto(dLugar, g -> lugar(g.getLugarCompraId()));
        texto(dValor, g -> valor(g.getValor(), g.getFormaAdquisicion()));
        texto(dFrecuencia, g -> g.getFrecuenciaCompra() == null ? "—" : g.getFrecuenciaCompra().toString());
        texto(dTipo, g -> tipo(g.getTipoGasto()));
        dAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        dAcciones.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink enlace = enlaceEliminar();

            {
                enlace.setOnAction(e -> eliminarGasto(getItem()));
            }

            @Override
            protected void updateItem(GastoDiario gasto, boolean vacia) {
                super.updateItem(gasto, vacia);
                setGraphic(vacia || gasto == null || !detalle.puedeEditar(RutasModulo.GASTOS_DIARIOS) ? null : enlace);
            }
        });
    }

    private void configurarTablaD1() {
        tablaD1.setPlaceholder(new Label("Sin comidas fuera del hogar registradas para este día."));
        tablaD1.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        texto(mOrden, m -> String.valueOf(m.getNumeroOrden()));
        texto(mDescripcion, ComidaFueraHogar::getDescripcion);
        texto(mTipoComida, m -> m.getTipoComida().getDescripcion());
        texto(mCantidad, m -> Formato.numero(m.getCantidad()));
        texto(mForma, m -> forma(m.getFormaAdquisicion(), m.getFormaAdquisicionOtra()));
        texto(mLugar, m -> lugar(m.getLugarCompraId()));
        texto(mValor, m -> valor(m.getValor(), m.getFormaAdquisicion()));
        texto(mFrecuencia, m -> m.getFrecuenciaCompra() == null ? "—" : m.getFrecuenciaCompra().toString());
        texto(mTipo, m -> tipo(m.getTipoGasto()));
        texto(mDomicilio, m -> m.isADomicilio() ? "Sí" : "No");
        mAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        mAcciones.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink enlace = enlaceEliminar();

            {
                enlace.setOnAction(e -> eliminarComida(getItem()));
            }

            @Override
            protected void updateItem(ComidaFueraHogar comida, boolean vacia) {
                super.updateItem(comida, vacia);
                setGraphic(vacia || comida == null || !detalle.puedeEditar(RutasModulo.COMIDAS) ? null : enlace);
            }
        });
    }

    private static <T> void texto(TableColumn<T, String> columna, Function<T, String> valor) {
        columna.setCellValueFactory(d -> new ReadOnlyStringWrapper(valor.apply(d.getValue())));
    }

    private static Hyperlink enlaceEliminar() {
        Hyperlink enlace = new Hyperlink("Eliminar");
        enlace.getStyleClass().add("enlace");
        return enlace;
    }

    private String lugar(Long id) {
        return id == null ? "—" : id + " · " + nombresLugares.getOrDefault(id, "");
    }

    private static String forma(FormaAdquisicion forma, String otra) {
        return forma == FormaAdquisicion.OTRA && otra != null ? "7. " + otra : forma.toString();
    }

    private static String valor(BigDecimal valor, FormaAdquisicion forma) {
        return Formato.pesos(valor) + (forma.esCompra() ? "" : " (estimado)");
    }

    private static String tipo(TipoGasto tipo) {
        return tipo == TipoGasto.PERSONAL ? "Personal" : "Hogar";
    }

    private static final class Datos {
        private List<RegistroDiario> registros = new ArrayList<>();
        private final Map<Integer, Long> renglones = new HashMap<>();
        private List<LugarCompra> lugares;
    }

    private static final class Renglones {
        private List<GastoDiario> gastos = new ArrayList<>();
        private List<ComidaFueraHogar> comidas = new ArrayList<>();
    }
}
