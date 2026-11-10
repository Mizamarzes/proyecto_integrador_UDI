package com.udi.controller.encuestas;

import com.udi.controller.viviendas.ViviendaController;
import com.udi.model.Encuesta;
import com.udi.model.Hogar;
import com.udi.model.Opcion;
import com.udi.model.Vivienda;
import com.udi.service.encuestas.EncuestaService;
import com.udi.service.viviendas.HogarService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Modal «Nueva encuesta». Muestra los hogares que todavía no tienen encuesta
 * y crea la encuesta del hogar elegido. Al encuestador se le asigna a sí
 * mismo; los demás roles eligen el encuestador.
 *
 * La fecha de inicio (A.21) queda en la fecha de hoy y se ajusta después en
 * la pestaña «Identificación y hogar» junto con el informante.
 */
public class NuevaEncuestaController {

    @FXML
    private Label lblSubtitulo;
    @FXML
    private TextField txtBuscar;
    @FXML
    private ScrollPane scrollHogares;
    @FXML
    private VBox listaHogares;
    @FXML
    private Label lblSinHogares;
    @FXML
    private VBox boxEncuestador;
    @FXML
    private ComboBox<Opcion> cmbEncuestador;
    @FXML
    private Button btnCrear;

    private final EncuestaService encuestaService = new EncuestaService();
    private final HogarService hogarService = new HogarService();
    private final ToggleGroup grupo = new ToggleGroup();

    private List<HogarLibre> libres = new ArrayList<>();
    private Optional<Long> encuestadorPropio = Optional.empty();
    private Consumer<Encuesta> alCrear;

    @FXML
    private void initialize() {
        Formato.mostrar(lblSinHogares, false);
        Formato.mostrar(boxEncuestador, false);
        btnCrear.setDisable(true);
        grupo.selectedToggleProperty().addListener((obs, antes, ahora) -> actualizarBoton());
        cmbEncuestador.valueProperty().addListener((obs, antes, ahora) -> actualizarBoton());
        txtBuscar.textProperty().addListener((obs, antes, ahora) -> pintarLista());
    }

    /** Carga los hogares libres; {@code alCrear} recibe la encuesta creada. */
    public void preparar(Consumer<Encuesta> alCrear) {
        this.alCrear = alCrear;
        Tareas.ejecutar(this::cargarDatos, datos -> {
            libres = datos.libres;
            encuestadorPropio = datos.encuestadorPropio;
            if (!encuestadorPropio.isPresent()) {
                lblSubtitulo.setText("Selecciona el hogar y el encuestador que diligenciará el cuadernillo.");
                cmbEncuestador.getItems().setAll(datos.encuestadores);
                Formato.mostrar(boxEncuestador, true);
            }
            pintarLista();
        });
    }

    /** Escribe un texto en la búsqueda (por ejemplo, el código del hogar). */
    public void buscar(String texto) {
        txtBuscar.setText(texto);
    }

    private Datos cargarDatos() {
        Datos datos = new Datos();
        datos.encuestadorPropio = encuestaService.encuestadorDeSesion();
        if (!datos.encuestadorPropio.isPresent()) {
            datos.encuestadores = encuestaService.opcionesEncuestadores();
        }
        Set<Long> conEncuesta = new HashSet<>();
        for (Encuesta e : encuestaService.listar()) {
            conEncuesta.add(e.getHogarId());
        }
        Map<Long, Vivienda> viviendas = new HashMap<>();
        for (Vivienda v : hogarService.listarViviendas()) {
            viviendas.put(v.getId(), v);
        }
        for (Hogar h : hogarService.listarHogares()) {
            if (!conEncuesta.contains(h.getId())) {
                datos.libres.add(new HogarLibre(h, viviendas.get(h.getViviendaId())));
            }
        }
        return datos;
    }

    private void pintarLista() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        listaHogares.getChildren().clear();
        Long seleccionado = grupo.getSelectedToggle() == null ? null : (Long) grupo.getSelectedToggle().getUserData();
        grupo.getToggles().clear();
        for (HogarLibre libre : libres) {
            if (!texto.isEmpty() && !libre.busqueda().contains(texto)) {
                continue;
            }
            RadioButton opcion = tarjeta(libre);
            opcion.setToggleGroup(grupo);
            opcion.setSelected(libre.hogar.getId().equals(seleccionado));
            listaHogares.getChildren().add(opcion);
        }
        boolean vacia = listaHogares.getChildren().isEmpty();
        lblSinHogares.setText(libres.isEmpty()
                ? "Todos los hogares registrados ya tienen encuesta. Registra un hogar nuevo en Viviendas y hogares."
                : "Ningún hogar coincide con la búsqueda.");
        Formato.mostrar(lblSinHogares, vacia);
        // Sin hogares libres no hay lista que mostrar; al filtrar se conserva el alto del modal
        Formato.mostrar(scrollHogares, !libres.isEmpty());
        actualizarBoton();
    }

    private RadioButton tarjeta(HogarLibre libre) {
        Hogar h = libre.hogar;
        Vivienda v = libre.vivienda;
        Label direccion = new Label((v == null ? "" : v.getDireccion()) + " · Hogar " + h.getHogarNumero());
        direccion.getStyleClass().add("dato-valor");
        Label detalle = new Label((v == null || v.getBarrio() == null ? "" : v.getBarrio() + " · ") + h.getTotalPersonas() + " personas");
        detalle.getStyleClass().add("texto-suave");
        Label codigo = new Label(Formato.codigo("H", h.getId()));
        codigo.getStyleClass().add("chip");
        Label vivienda = new Label(Formato.codigo("V", h.getViviendaId()));
        vivienda.getStyleClass().add("texto-suave");
        VBox codigos = new VBox(2, codigo, vivienda);
        codigos.setAlignment(Pos.CENTER_RIGHT);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox contenido = new HBox(10, new VBox(2, direccion, detalle), espacio, codigos);
        contenido.setAlignment(Pos.CENTER_LEFT);
        contenido.setPrefWidth(470);

        RadioButton opcion = new RadioButton();
        opcion.setGraphic(contenido);
        opcion.setUserData(h.getId());
        opcion.getStyleClass().add("radio-tarjeta");
        opcion.setMaxWidth(Double.MAX_VALUE);
        return opcion;
    }

    private void actualizarBoton() {
        boolean conHogar = grupo.getSelectedToggle() != null;
        boolean conEncuestador = encuestadorPropio.isPresent() || cmbEncuestador.getValue() != null;
        btnCrear.setDisable(!(conHogar && conEncuestador));
    }

    @FXML
    private void crear() {
        Encuesta nueva = new Encuesta();
        nueva.setHogarId((Long) grupo.getSelectedToggle().getUserData());
        nueva.setEncuestadorId(encuestadorPropio.orElseGet(() -> cmbEncuestador.getValue().getId()));
        nueva.setFechaDesde(LocalDate.now());
        Tareas.ejecutar(() -> encuestaService.crear(nueva), creada -> {
            Modal.cerrar(btnCrear);
            alCrear.accept(creada);
        });
    }

    @FXML
    private void cancelar() {
        Modal.cerrar(btnCrear);
    }

    @FXML
    private void irViviendas() {
        Modal.cerrar(btnCrear);
        ViviendaController.abrirListado();
    }

    /** Hogar sin encuesta con los datos de su vivienda. */
    private static final class HogarLibre {
        private final Hogar hogar;
        private final Vivienda vivienda;

        private HogarLibre(Hogar hogar, Vivienda vivienda) {
            this.hogar = hogar;
            this.vivienda = vivienda;
        }

        private String busqueda() {
            String texto = Formato.codigo("H", hogar.getId()) + " " + Formato.codigo("V", hogar.getViviendaId());
            if (vivienda != null) {
                texto += " " + vivienda.getDireccion() + " " + (vivienda.getBarrio() == null ? "" : vivienda.getBarrio());
            }
            return texto.toLowerCase();
        }
    }

    private static final class Datos {
        private final List<HogarLibre> libres = new ArrayList<>();
        private List<Opcion> encuestadores = new ArrayList<>();
        private Optional<Long> encuestadorPropio = Optional.empty();
    }
}
