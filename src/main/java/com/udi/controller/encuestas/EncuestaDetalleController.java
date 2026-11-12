package com.udi.controller.encuestas;

import com.udi.App;
import com.udi.controller.shared.NavbarController;
import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.Rutas;
import com.udi.model.AuditoriaEncuesta;
import com.udi.model.Encuesta;
import com.udi.model.Hogar;
import com.udi.model.Visita;
import com.udi.model.Vivienda;
import com.udi.service.encuestas.EncuestaService;
import com.udi.service.encuestas.VisitaService;
import com.udi.service.viviendas.HogarService;
import com.udi.util.Alertas;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Detalle de una encuesta. Muestra el encabezado (código, estado, hogar,
 * informante y periodo) y una pestaña por sección del cuadernillo. Cada
 * pestaña es una vista de {@code modulos/encuestas/partials} con su controlador.
 */
public class EncuestaDetalleController {

    /** Pestañas del detalle, con su vista y el texto de la ruta del encabezado. */
    public enum Pestana {
        VISITAS("encuesta_visitas", "Control de visitas"),
        HOGAR("encuesta_hogar", "Cap. A"),
        CAPITULO_C("encuesta_capitulo_c", "Cap. C"),
        GASTOS("encuesta_gastos", "Línea de 14 días"),
        CIERRE("encuesta_cierre", "Cierre");

        private final String vista;
        private final String miga;

        Pestana(String vista, String miga) {
            this.vista = "modulos/encuestas/partials/" + vista;
            this.miga = miga;
        }
    }

    @FXML
    private ScrollPane scroll;
    @FXML
    private ToggleGroup grupoPestanas;
    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private ToggleButton tabVisitas;
    @FXML
    private ToggleButton tabHogar;
    @FXML
    private ToggleButton tabCapituloC;
    @FXML
    private ToggleButton tabGastos;
    @FXML
    private ToggleButton tabCierre;
    @FXML
    private Label lblAviso;
    @FXML
    private Label lblCorreccion;
    @FXML
    private StackPane contenido;

    /** Formulario de visitas (módulo «encuestas», middleware/RutasModulo). */
    static final String VISTA_VISITA = "modulos/encuestas/partials/visita_form";

    private final EncuestaService encuestaService = new EncuestaService();
    private final HogarService hogarService = new HogarService();
    private final Map<Pestana, ToggleButton> botones = new EnumMap<>(Pestana.class);

    private Encuesta encuesta;
    private Hogar hogar;
    private Vivienda vivienda;
    /** Pestaña que se está mostrando; null hasta que se carga la encuesta. */
    private Pestana actual;

    /** Abre el detalle de la encuesta {@code id} en el área de contenido, en la pestaña indicada. */
    public static void abrir(long id, Pestana pestana) {
        NavbarController.marcarModulo("encuestas");
        EncuestaDetalleController detalle = NavbarController.abrir("modulos/encuestas/encuesta_detalle",
                "Encuestas", Formato.codigo("E", id));
        detalle.cargar(id, pestana);
    }

    @FXML
    private void initialize() {
        botones.put(Pestana.VISITAS, tabVisitas);
        botones.put(Pestana.HOGAR, tabHogar);
        botones.put(Pestana.CAPITULO_C, tabCapituloC);
        botones.put(Pestana.GASTOS, tabGastos);
        botones.put(Pestana.CIERRE, tabCierre);
        botones.forEach((pestana, boton) -> {
            boton.setUserData(pestana);
            // Cada pestaña se muestra con los mismos middlewares que protegen su vista
            Rutas.permitir(boton, pestana.vista);
        });
        grupoPestanas.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                // Una pestaña siempre queda elegida
                grupoPestanas.selectToggle(antes);
            } else if (encuesta != null && ahora.getUserData() != actual) {
                mostrarPestana((Pestana) ahora.getUserData());
            }
        });
        Formato.mostrar(lblAviso, false);
        Formato.mostrar(lblEstado, false);
        Formato.mostrar(lblCorreccion, false);
    }

    /** Carga la encuesta con su hogar y vivienda y muestra la pestaña indicada. */
    public void cargar(long id, Pestana pestana) {
        Tareas.ejecutar(() -> {
            Datos datos = new Datos();
            datos.encuesta = encuestaService.buscar(id)
                    .orElseThrow(() -> new ReglaNegocioException("No existe la encuesta " + id + "."));
            datos.hogar = hogarService.buscarHogar(datos.encuesta.getHogarId()).orElse(null);
            datos.vivienda = datos.hogar == null ? null : hogarService.buscarVivienda(datos.hogar.getViviendaId()).orElse(null);
            datos.devolucion = encuestaService.ultimaDevolucion(id).orElse(null);
            return datos;
        }, datos -> {
            encuesta = datos.encuesta;
            hogar = datos.hogar;
            vivienda = datos.vivienda;
            pintarEncabezado();
            pintarCorreccion(datos.devolucion);
            ir(pestana);
        });
    }

    /** Vuelve a leer la encuesta (por ejemplo, después de cerrarla) y la pestaña actual. */
    public void recargar() {
        cargar(encuesta.getId(), actual);
    }

    /** Cambia de pestaña. Si el rol no puede abrirla, va a la primera que sí pueda. */
    public void ir(Pestana pestana) {
        ToggleButton boton = botones.get(pestana);
        if (!boton.isVisible()) {
            boton = botones.values().stream().filter(ToggleButton::isVisible).findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("Su rol no tiene permiso para ver ninguna sección de la encuesta."));
            pestana = (Pestana) boton.getUserData();
        }
        if (boton.isSelected()) {
            mostrarPestana(pestana);
        } else {
            // El listener del grupo muestra la pestaña
            boton.setSelected(true);
        }
    }

    /** Vuelve a mostrar la pestaña actual con los datos de la base. */
    public void recargarPestana() {
        mostrarPestana(actual);
    }

    /** Mensaje de confirmación debajo de las pestañas; null lo oculta. */
    public void avisar(String mensaje) {
        lblAviso.setText(mensaje);
        Formato.mostrar(lblAviso, mensaje != null);
    }

    public Encuesta getEncuesta() {
        return encuesta;
    }

    public Hogar getHogar() {
        return hogar;
    }

    public Vivienda getVivienda() {
        return vivienda;
    }

    /** false si la encuesta está pendiente de aprobación o aprobada (RF-06): las pestañas quedan en solo lectura. */
    public boolean esEditable() {
        return encuesta != null && !encuesta.estaCerrada();
    }

    /** Hogar H-000001 · Carrera 27 # 45-12, para los subtítulos de los modales. */
    public String descripcionHogar() {
        String codigo = "Hogar " + Formato.codigo("H", encuesta.getHogarId());
        return vivienda == null ? codigo : codigo + " · " + vivienda.getDireccion();
    }

    private void mostrarPestana(Pestana pestana) {
        actual = pestana;
        FXMLLoader loader = App.cargar(pestana.vista);
        contenido.getChildren().setAll((Parent) loader.getRoot());
        NavbarController.migas("Encuestas", Formato.codigo("E", encuesta.getId()), pestana.miga);
        SeccionEncuesta seccion = loader.getController();
        seccion.mostrar(this);
        Platform.runLater(() -> scroll.setVvalue(0));
    }

    private void pintarEncabezado() {
        lblTitulo.setText("Encuesta " + Formato.codigo("E", encuesta.getId()));
        lblEstado.setText(encuesta.getEstado().getDescripcion());
        lblEstado.getStyleClass().removeIf(c -> c.startsWith("insignia-"));
        lblEstado.getStyleClass().add("insignia-" + encuesta.getEstado().name().toLowerCase());
        Formato.mostrar(lblEstado, true);

        StringBuilder texto = new StringBuilder(Formato.codigo("H", encuesta.getHogarId()));
        if (vivienda != null) {
            texto.append(" · ").append(vivienda.getDireccion());
        }
        texto.append(" · Informante: ").append(encuesta.getInformanteNombre() == null ? "por registrar" : encuesta.getInformanteNombre());
        texto.append(" · Periodo ").append(Formato.fecha(encuesta.getFechaDesde()))
                .append(" – ").append(Formato.fecha(encuesta.getFechaHasta()));
        lblSubtitulo.setText(texto.toString());
    }

    /** Qué pidió corregir el crítico-digitador; se oculta si la encuesta no está rechazada. */
    private void pintarCorreccion(AuditoriaEncuesta devolucion) {
        if (devolucion != null) {
            lblCorreccion.setText("Rechazada por " + devolucion.getUsuarioNombre() + " el "
                    + Formato.fechaHora(devolucion.getCreatedAt()) + ": «" + devolucion.getObservacion()
                    + "». Corrija y vuelva a cerrar el caso en la pestaña Cierre.");
        }
        Formato.mostrar(lblCorreccion, devolucion != null);
    }

    @FXML
    private void volver() {
        NavbarController.abrir("modulos/encuestas/encuesta", "Encuestas");
    }

    /** Abre el modal de la visita: nueva si {@code visita} es null, o para editarla. */
    void abrirVisita(List<Visita> visitas, Visita visita) {
        if (visita == null && visitas.size() >= VisitaService.MAXIMO_VISITAS) {
            Alertas.error("Control de visitas", "El hogar ya tiene las " + VisitaService.MAXIMO_VISITAS + " visitas permitidas.");
            return;
        }
        Modal<VisitaFormController> modal = Modal.cargar(VISTA_VISITA,
                visita == null ? "Registrar visita" : "Editar visita");
        if (visita == null) {
            int siguiente = visitas.stream().mapToInt(Visita::getNumeroVisita).max().orElse(0) + 1;
            modal.controlador().nueva(encuesta.getHogarId(), siguiente, descripcionHogar(), guardada -> {
                avisar("Visita " + guardada.getNumeroVisita() + " registrada (" + guardada.getResultadoVisita() + ").");
                if (actual == Pestana.VISITAS || actual == Pestana.CIERRE) {
                    recargarPestana();
                }
            });
        } else {
            modal.controlador().editar(visita, descripcionHogar(), guardada -> {
                avisar("Visita " + guardada.getNumeroVisita() + " actualizada.");
                recargarPestana();
            });
        }
        modal.mostrar();
    }

    private static final class Datos {
        private Encuesta encuesta;
        private Hogar hogar;
        private Vivienda vivienda;
        private AuditoriaEncuesta devolucion;
    }
}
