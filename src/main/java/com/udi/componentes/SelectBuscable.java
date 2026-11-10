package com.udi.componentes;

import com.udi.model.Pagina;
import com.udi.util.Tareas;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.function.Function;

/**
 * Select con buscador y resultados paginados. Al abrirlo muestra un campo de
 * búsqueda y la primera página de opciones; al final de la lista aparece el
 * botón «Ver más» mientras queden resultados. Las opciones se piden a una
 * {@link Fuente} (normalmente un servicio) en segundo plano.
 *
 * Se usa en FXML como {@code <SelectBuscable fx:id="..." promptText="..." />};
 * el controlador le asigna la fuente con {@link #setFuente}.
 */
public class SelectBuscable<T> extends HBox {

    /** Busca una página de opciones que contienen {@code texto}. */
    @FunctionalInterface
    public interface Fuente<T> {
        Pagina<T> buscar(String texto, int desde, int limite) throws Exception;
    }

    /** Marcadores de la lista que no son opciones. */
    private static final Object VER_MAS = new Object();
    private static final Object VACIA = new Object();

    private static final int TAMANO_PAGINA = 20;

    private final ObjectProperty<T> value = new SimpleObjectProperty<>(this, "value");
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "Seleccione");
    private final StringProperty opcionVacia = new SimpleStringProperty(this, "opcionVacia");

    private final Label texto = new Label();
    private final Popup popup = new Popup();
    private final TextField buscador = new TextField();
    private final ListView<Object> lista = new ListView<>();
    private final Label estado = new Label();
    private final PauseTransition espera = new PauseTransition(Duration.millis(300));

    private Fuente<T> fuente;
    private Function<T, String> convertidor = String::valueOf;
    /** Cada búsqueda nueva invalida las respuestas de las anteriores. */
    private int consulta;
    private int cargados;

    public SelectBuscable() {
        getStyleClass().addAll("combo-linea", "select-buscable");
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);
        setMinWidth(80);

        texto.getStyleClass().add("select-texto");
        texto.setMaxWidth(Double.MAX_VALUE);
        // Puede encogerse: los nombres largos se recortan con «…»
        texto.setMinWidth(0);
        texto.setTextOverrun(OverrunStyle.ELLIPSIS);
        HBox.setHgrow(texto, Priority.ALWAYS);
        Region flecha = new Region();
        flecha.getStyleClass().add("arrow");
        // Sin esto la flecha se estira a todo el alto del select y se deforma
        flecha.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane boton = new StackPane(flecha);
        boton.setMinWidth(Region.USE_PREF_SIZE);
        boton.getStyleClass().add("arrow-button");
        getChildren().addAll(texto, boton);

        // Invalidación y no cambio: dos entidades con el mismo id son «iguales»,
        // pero la nueva puede traer otro nombre y hay que repintarla
        value.addListener((Observable obs) -> pintar());
        promptText.addListener((obs, antes, ahora) -> pintar());
        pintar();

        construirPopup();

        // Al presionar (no al hacer clic): el popup consume la pulsación que lo
        // cierra por autoHide, así un clic sobre el select abierto solo lo cierra
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                requestFocus();
                alternar();
            }
        });
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.DOWN) {
                abrir();
                e.consume();
            } else if ((e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) && getOpcionVacia() != null) {
                setValue(null);
                e.consume();
            }
        });
        disabledProperty().addListener((obs, antes, deshabilitado) -> {
            if (deshabilitado) {
                popup.hide();
            }
        });
    }

    private void construirPopup() {
        buscador.setPromptText("Buscar…");
        buscador.getStyleClass().add("select-buscador");
        buscador.textProperty().addListener((obs, antes, ahora) -> espera.playFromStart());
        espera.setOnFinished(e -> buscar());
        buscador.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DOWN) {
                lista.requestFocus();
                if (lista.getSelectionModel().isEmpty() && !lista.getItems().isEmpty()) {
                    lista.getSelectionModel().select(0);
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                if (espera.getStatus() == PauseTransition.Status.RUNNING) {
                    return;
                }
                Object primero = lista.getSelectionModel().isEmpty()
                        ? (lista.getItems().isEmpty() ? null : lista.getItems().get(0))
                        : lista.getSelectionModel().getSelectedItem();
                elegir(primero);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                e.consume();
            }
        });

        lista.getStyleClass().add("select-lista");
        lista.setPrefHeight(260);
        lista.setFixedCellSize(32);
        lista.setPlaceholder(estado);
        lista.setCellFactory(l -> new Celda());
        lista.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                elegir(lista.getSelectionModel().getSelectedItem());
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
                e.consume();
            }
        });

        VBox contenido = new VBox(6, buscador, lista);
        contenido.getStyleClass().add("select-popup");
        popup.getContent().add(contenido);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setOnHidden(e -> requestFocus());
    }

    private void alternar() {
        if (popup.isShowing()) {
            popup.hide();
        } else {
            abrir();
        }
    }

    /** Abre la lista con la primera página de resultados. */
    public void abrir() {
        if (isDisabled() || fuente == null || getScene() == null || popup.isShowing()) {
            return;
        }
        VBox contenido = (VBox) popup.getContent().get(0);
        // El popup es otra ventana: no hereda las hojas de estilo de la escena
        contenido.getStylesheets().setAll(getScene().getStylesheets());
        contenido.setPrefWidth(Math.max(getWidth(), 240));
        Bounds limites = localToScreen(getBoundsInLocal());
        popup.show(this, limites.getMinX(), limites.getMaxY() + 2);
        buscador.setText("");
        espera.stop();
        buscar();
        buscador.requestFocus();
    }

    /** Busca desde la primera página con el texto actual del buscador. */
    private void buscar() {
        lista.getItems().clear();
        cargados = 0;
        cargar(false);
    }

    private void cargar(boolean agregar) {
        int numero = ++consulta;
        String filtro = buscador.getText();
        int desde = cargados;
        Fuente<T> origen = fuente;
        estado.setText("Cargando…");
        // Primera fila visible antes de cargar más: se vuelve a ella para que la
        // lista no salte al final y haya que hacer scroll hasta el nuevo «Ver más»
        int primeraVisible = agregar ? primeraVisible() : -1;
        if (agregar) {
            lista.getItems().remove(VER_MAS);
        }
        Tareas.ejecutar(() -> origen.buscar(filtro, desde, TAMANO_PAGINA), pagina -> {
            if (numero != consulta) {
                return;
            }
            if (!agregar && getOpcionVacia() != null && (filtro == null || filtro.trim().isEmpty())) {
                lista.getItems().add(VACIA);
            }
            lista.getItems().addAll(pagina.getRegistros());
            cargados += pagina.getRegistros().size();
            if (pagina.isHayMas()) {
                lista.getItems().add(VER_MAS);
            }
            estado.setText("Sin resultados");
            if (primeraVisible >= 0) {
                lista.scrollTo(primeraVisible);
            }
            if (!agregar) {
                int actual = lista.getItems().indexOf(getValue());
                if (actual >= 0) {
                    lista.getSelectionModel().select(actual);
                    lista.scrollTo(actual);
                }
            }
        });
    }

    /** Índice de la primera fila visible de la lista, o -1 si no se puede saber. */
    private int primeraVisible() {
        if (lista.getSkin() instanceof ListViewSkin) {
            for (javafx.scene.Node hijo : ((ListViewSkin<?>) lista.getSkin()).getChildren()) {
                if (hijo instanceof VirtualFlow) {
                    IndexedCell<?> celda = ((VirtualFlow<?>) hijo).getFirstVisibleCell();
                    return celda == null ? -1 : celda.getIndex();
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private void elegir(Object item) {
        if (item == null) {
            return;
        }
        if (item == VER_MAS) {
            cargar(true);
            return;
        }
        setValue(item == VACIA ? null : (T) item);
        popup.hide();
    }

    private void pintar() {
        T valor = getValue();
        boolean vacio = valor == null;
        texto.setText(vacio ? (getOpcionVacia() != null ? getOpcionVacia() : getPromptText()) : convertidor.apply(valor));
        texto.getStyleClass().remove("select-prompt");
        if (vacio && getOpcionVacia() == null) {
            texto.getStyleClass().add("select-prompt");
        }
    }

    private final class Celda extends ListCell<Object> {
        private final Button verMas = new Button("Ver más");

        private Celda() {
            // Ancho preferido 0: la celda toma el ancho de la lista en vez de
            // ensancharla, así no hay scroll horizontal y el texto largo se recorta con «…»
            setPrefWidth(0);
            setTextOverrun(OverrunStyle.ELLIPSIS);
            verMas.getStyleClass().add("select-ver-mas");
            verMas.setMaxWidth(Double.MAX_VALUE);
            verMas.setOnAction(e -> elegir(VER_MAS));
            setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && !isEmpty() && getItem() != VER_MAS) {
                    elegir(getItem());
                }
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void updateItem(Object item, boolean vacia) {
            super.updateItem(item, vacia);
            getStyleClass().remove("select-opcion-vacia");
            if (vacia || item == null) {
                setText(null);
                setGraphic(null);
            } else if (item == VER_MAS) {
                setText(null);
                setGraphic(verMas);
            } else if (item == VACIA) {
                setText(getOpcionVacia());
                setGraphic(null);
                getStyleClass().add("select-opcion-vacia");
            } else {
                setText(convertidor.apply((T) item));
                setGraphic(null);
            }
            // El nombre completo queda en el tooltip por si se recortó
            String completo = getText();
            setTooltip(completo == null || completo.isEmpty() ? null : tooltip(completo));
        }
    }

    private static Tooltip tooltip(String texto) {
        Tooltip t = new Tooltip(texto);
        t.setShowDelay(Duration.millis(400));
        return t;
    }

    // ------------------------------------------------------------------
    // Propiedades
    // ------------------------------------------------------------------

    /** Origen de las opciones; se consulta cada vez que se abre o se busca. */
    public void setFuente(Fuente<T> fuente) {
        this.fuente = fuente;
    }

    /** Texto de cada opción; por defecto su toString. */
    public void setConvertidor(Function<T, String> convertidor) {
        this.convertidor = convertidor;
        pintar();
    }

    public ObjectProperty<T> valueProperty() {
        return value;
    }

    public T getValue() {
        return value.get();
    }

    public void setValue(T valor) {
        value.set(valor);
    }

    public StringProperty promptTextProperty() {
        return promptText;
    }

    public String getPromptText() {
        return promptText.get();
    }

    public void setPromptText(String texto) {
        promptText.set(texto);
    }

    /** Si se define (p. ej. «Todas»), es la primera opción y deja el valor en null. */
    public StringProperty opcionVaciaProperty() {
        return opcionVacia;
    }

    public String getOpcionVacia() {
        return opcionVacia.get();
    }

    public void setOpcionVacia(String texto) {
        opcionVacia.set(texto);
        pintar();
    }
}
