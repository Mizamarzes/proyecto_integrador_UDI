package com.udi.controller;

import com.udi.model.Visita;
import com.udi.model.enums.ResultadoEntrevista;
import com.udi.service.EncuestaService;
import com.udi.service.VisitaService;
import com.udi.util.Formato;
import com.udi.util.Tareas;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.List;

/** Pestaña «Control de visitas»: resumen del hogar y tabla de visitas (capítulo B, RF-08). */
public class EncuestaVisitasController implements SeccionEncuesta {

    @FXML
    private Label lblTotal;
    @FXML
    private Label lblProxima;
    @FXML
    private ProgressBar barAvance;
    @FXML
    private Label lblAvance;
    @FXML
    private Label lblUltima;
    @FXML
    private Label lblUltimaFecha;
    @FXML
    private Label lblDiligenciado;
    @FXML
    private TableView<Visita> tabla;
    @FXML
    private TableColumn<Visita, String> colNumero;
    @FXML
    private TableColumn<Visita, String> colFecha;
    @FXML
    private TableColumn<Visita, ResultadoEntrevista> colResultado;
    @FXML
    private TableColumn<Visita, String> colPendientes;
    @FXML
    private TableColumn<Visita, String> colProxima;
    @FXML
    private TableColumn<Visita, Visita> colAcciones;

    private final VisitaService visitaService = new VisitaService();

    private EncuestaDetalleController detalle;
    private List<Visita> visitas = List.of();

    @Override
    public void mostrar(EncuestaDetalleController detalle) {
        this.detalle = detalle;
        configurarTabla();
        int dia = EncuestaController.diaActual(detalle.getEncuesta());
        barAvance.setProgress(dia / (double) EncuestaService.DIAS_PERIODO);
        lblAvance.setText(detalle.getEncuesta().estaCerrada() ? "Periodo completo" : "Día " + dia + " de " + EncuestaService.DIAS_PERIODO);
        Tareas.ejecutar(() -> visitaService.listarPorHogar(detalle.getEncuesta().getHogarId()), this::pintar);
    }

    private void pintar(List<Visita> lista) {
        visitas = lista;
        tabla.getItems().setAll(lista);
        lblTotal.setText(String.valueOf(lista.size()));
        lblDiligenciado.setText("Total diligenciado: " + lista.size() + " de " + VisitaService.MAXIMO_VISITAS + " visitas");
        if (lista.isEmpty()) {
            lblProxima.setText("Sin programar");
            lblUltima.setText("Sin visitas");
            lblUltimaFecha.setText("");
            return;
        }
        Visita ultima = lista.get(lista.size() - 1);
        lblUltima.setText(ultima.getResultadoVisita().toString());
        lblUltimaFecha.setText(Formato.fecha(ultima.getFecha()) + " · " + Formato.hora(ultima.getHoraInicio()));
        lblProxima.setText(ultima.getFechaProxima() == null ? "Por concertar" : proxima(ultima));
    }

    private static String proxima(Visita v) {
        if (v.getFechaProxima() == null) {
            return "";
        }
        String texto = v.getFechaProxima().equals(LocalDate.now()) ? "Hoy" : Formato.fecha(v.getFechaProxima());
        return v.getHoraProxima() == null ? texto : texto + " · " + Formato.hora(v.getHoraProxima());
    }

    private void configurarTabla() {
        tabla.setPlaceholder(new Label("El hogar todavía no tiene visitas registradas."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colNumero.setCellValueFactory(d -> new ReadOnlyStringWrapper(String.valueOf(d.getValue().getNumeroVisita())));
        colFecha.setCellValueFactory(d -> {
            Visita v = d.getValue();
            String horas = Formato.hora(v.getHoraInicio()) + (v.getHoraFin() == null ? "" : " – " + Formato.hora(v.getHoraFin()));
            return new ReadOnlyStringWrapper(Formato.fecha(v.getFecha()) + "  " + horas);
        });
        colResultado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getResultadoVisita()));
        colPendientes.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getPartesPendientes() == null ? "—" : d.getValue().getPartesPendientes()));
        colProxima.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getFechaProxima() == null ? "Por concertar" : proxima(d.getValue())));
        colAcciones.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));

        colResultado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ResultadoEntrevista resultado, boolean vacia) {
                super.updateItem(resultado, vacia);
                setGraphic(vacia || resultado == null ? null : insigniaResultado(resultado));
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink enlace = new Hyperlink("Editar");

            {
                enlace.getStyleClass().add("enlace");
                enlace.setOnAction(e -> detalle.abrirVisita(visitas, getItem()));
            }

            @Override
            protected void updateItem(Visita visita, boolean vacia) {
                super.updateItem(visita, vacia);
                setGraphic(vacia || visita == null || !detalle.esEditable() ? null : enlace);
            }
        });
    }

    /** Insignia de color con el resultado de la entrevista (EC, EI, AT…). */
    static Label insigniaResultado(ResultadoEntrevista resultado) {
        Label etiqueta = new Label(resultado.toString());
        etiqueta.getStyleClass().addAll("insignia", "insignia-" + resultado.name().toLowerCase());
        return etiqueta;
    }
}
