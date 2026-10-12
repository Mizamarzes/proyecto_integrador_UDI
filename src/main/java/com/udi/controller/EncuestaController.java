package com.udi.controller;

import com.udi.model.Encuesta;
import com.udi.model.EncuestaResumen;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.service.EncuestaService;
import com.udi.util.Tareas;
import com.udi.util.Formato;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.YearMonth;

/** Listado de encuestas con búsqueda y filtros por estado y periodo. */
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
    private TextField txtBuscar;
    @FXML
    private ComboBox<String> cmbEstado;
    @FXML
    private ComboBox<Periodo> cmbPeriodo;
    @FXML
    private TableView<EncuestaResumen> tabla;
    @FXML
    private TableColumn<EncuestaResumen, String> colId;
    @FXML
    private TableColumn<EncuestaResumen, String> colHogar;
    @FXML
    private TableColumn<EncuestaResumen, String> colEncuestador;
    @FXML
    private TableColumn<EncuestaResumen, EstadoEncuesta> colEstado;
    @FXML
    private TableColumn<EncuestaResumen, String> colFecha;
    @FXML
    private TableColumn<EncuestaResumen, EncuestaResumen> colAcciones;
    @FXML
    private Label lblTotal;

    private final EncuestaService encuestaService = new EncuestaService();
    private final ObservableList<EncuestaResumen> encuestas = FXCollections.observableArrayList();
    private final FilteredList<EncuestaResumen> filtradas = new FilteredList<>(encuestas);

    @FXML
    private void initialize() {
        cmbEstado.getItems().add(TODOS);
        for (EstadoEncuesta estado : EstadoEncuesta.values()) {
            cmbEstado.getItems().add(estado.getDescripcion());
        }
        cmbPeriodo.getItems().setAll(Periodo.values());
        configurarTabla();

        txtBuscar.textProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbEstado.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        cmbPeriodo.valueProperty().addListener((obs, antes, ahora) -> filtrar());
        limpiar();

        Tareas.ejecutar(() -> encuestaService.listarResumen(), lista -> encuestas.setAll(lista));
    }

    @FXML
    private void limpiar() {
        txtBuscar.clear();
        cmbEstado.setValue(TODOS);
        cmbPeriodo.setValue(Periodo.TODOS);
    }

    @FXML
    private void nuevaEncuesta() {
        EncuestaFormController formulario = MenuController.abrir("modulos/encuestas/encuesta_form", "Encuestas", "Nueva encuesta");
        formulario.nueva();
    }

    private void abrir(EncuestaResumen fila) {
        long id = fila.getEncuesta().getId();
        EncuestaFormController formulario = MenuController.abrir("modulos/encuestas/encuesta_form", "Encuestas", Formato.codigo("E", id));
        formulario.editar(id);
    }

    private void configurarTabla() {
        tabla.setItems(filtradas);
        tabla.setPlaceholder(new Label("No hay encuestas que coincidan con la búsqueda."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colId.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.codigo("E", d.getValue().getEncuesta().getId())));
        colHogar.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.codigo("H", d.getValue().getEncuesta().getHogarId())));
        colEncuestador.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEncuestador()));
        colEstado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getEncuesta().getEstado()));
        colFecha.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formato.fecha(d.getValue().getEncuesta().getFechaDesde())));
        colAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoEncuesta estado, boolean vacia) {
                super.updateItem(estado, vacia);
                if (vacia || estado == null) {
                    setGraphic(null);
                    return;
                }
                Label etiqueta = new Label(estado.getDescripcion());
                etiqueta.getStyleClass().addAll("insignia", "insignia-" + estado.name().toLowerCase());
                setGraphic(etiqueta);
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
                enlace.setText(fila.getEncuesta().estaCerrada() ? "Ver" : "Ver / editar");
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

    private void filtrar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String estado = cmbEstado.getValue();
        Periodo periodo = cmbPeriodo.getValue() == null ? Periodo.TODOS : cmbPeriodo.getValue();
        filtradas.setPredicate(fila -> {
            Encuesta e = fila.getEncuesta();
            boolean coincideTexto = texto.isEmpty()
                    || Formato.codigo("E", e.getId()).toLowerCase().contains(texto)
                    || Formato.codigo("H", e.getHogarId()).toLowerCase().contains(texto)
                    || fila.getEncuestador().toLowerCase().contains(texto)
                    || (e.getInformanteNombre() != null && e.getInformanteNombre().toLowerCase().contains(texto));
            boolean coincideEstado = estado == null || TODOS.equals(estado)
                    || e.getEstado().getDescripcion().equals(estado);
            return coincideTexto && coincideEstado && periodo.incluye(e);
        });
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(filtradas.size() + " de " + encuestas.size() + " encuestas");
    }
}
