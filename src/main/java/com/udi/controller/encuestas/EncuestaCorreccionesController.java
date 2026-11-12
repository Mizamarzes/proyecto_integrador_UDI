package com.udi.controller.encuestas;

import com.udi.model.AuditoriaEncuesta;
import com.udi.model.Encuesta;
import com.udi.model.enums.AccionAuditoria;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.service.encuestas.EncuestaService;
import com.udi.util.Formato;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Pestaña «Correcciones»: lo que el crítico-digitador pidió corregir cada vez
 * que rechazó el caso, y sus aprobaciones (auditoria_encuesta, RF-10), para que
 * el encuestador sepa qué ajustar antes de volver a cerrarlo.
 */
public class EncuestaCorreccionesController implements SeccionEncuesta {

    @FXML
    private Label lblRechazos;
    @FXML
    private Label lblEstado;
    @FXML
    private Label lblEstadoDetalle;
    @FXML
    private Label lblUltima;
    @FXML
    private Label lblUltimaFecha;
    @FXML
    private VBox lista;

    private final EncuestaService encuestaService = new EncuestaService();

    private Encuesta encuesta;

    @Override
    public void mostrar(EncuestaDetalleController detalle) {
        encuesta = detalle.getEncuesta();
        Tareas.ejecutar(() -> encuestaService.revisiones(encuesta.getId()), this::pintar);
    }

    /** {@code revisiones} viene de la más reciente a la más antigua. */
    private void pintar(List<AuditoriaEncuesta> revisiones) {
        long rechazos = revisiones.stream().filter(a -> a.getAccion() == AccionAuditoria.RECHAZADA).count();
        lblRechazos.setText(String.valueOf(rechazos));
        pintarEstado(rechazos);

        if (revisiones.isEmpty()) {
            lblUltima.setText("Sin revisiones");
            lblUltimaFecha.setText("—");
        } else {
            AuditoriaEncuesta ultima = revisiones.get(0);
            lblUltima.setText(ultima.getAccion().getDescripcion()
                    + (ultima.getUsuarioNombre() == null ? "" : " · " + ultima.getUsuarioNombre()));
            lblUltimaFecha.setText(Formato.fechaHora(ultima.getCreatedAt()));
        }

        lista.getChildren().clear();
        if (revisiones.isEmpty()) {
            Label vacio = new Label("La encuesta no tiene correcciones ni comentarios registrados.");
            vacio.getStyleClass().add("texto-suave");
            lista.getChildren().add(vacio);
            return;
        }
        long numero = rechazos;
        for (int i = 0; i < revisiones.size(); i++) {
            AuditoriaEncuesta a = revisiones.get(i);
            boolean rechazo = a.getAccion() == AccionAuditoria.RECHAZADA;
            // Solo el rechazo más reciente sigue pendiente, y mientras la encuesta siga rechazada
            boolean pendiente = rechazo && i == 0 && encuesta.getEstado() == EstadoEncuesta.RECHAZADA;
            lista.getChildren().add(fila(a, rechazo ? "Rechazo n.º " + numero : "Aprobación", pendiente));
            if (rechazo) {
                numero--;
            }
        }
    }

    private void pintarEstado(long rechazos) {
        switch (encuesta.getEstado()) {
            case RECHAZADA:
                lblEstado.setText("Pendiente de corregir");
                lblEstadoDetalle.setText("Corrija lo indicado y vuelva a cerrar el caso en la pestaña Cierre.");
                break;
            case PENDIENTE_APROBACION:
                lblEstado.setText("En revisión");
                lblEstadoDetalle.setText(rechazos > 0 ? "Las correcciones se enviaron al crítico-digitador."
                        : "El caso espera la revisión del crítico-digitador.");
                break;
            case APROBADA:
                lblEstado.setText("Aprobada");
                lblEstadoDetalle.setText("El caso no admite más cambios.");
                break;
            default:
                lblEstado.setText("En curso");
                lblEstadoDetalle.setText(rechazos > 0 ? "Se revisará de nuevo al cerrar el caso."
                        : "Aún no se ha enviado a revisión.");
        }
    }

    private static VBox fila(AuditoriaEncuesta a, String titulo, boolean pendiente) {
        boolean rechazo = a.getAccion() == AccionAuditoria.RECHAZADA;
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dato-valor");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label insignia = new Label(!rechazo ? "APROBADA" : pendiente ? "PENDIENTE DE CORREGIR" : "ATENDIDA");
        insignia.getStyleClass().addAll("insignia",
                !rechazo ? "insignia-aprobada" : pendiente ? "insignia-rechazada" : "insignia-ec");
        insignia.setMinWidth(Region.USE_PREF_SIZE);
        HBox encabezado = new HBox(10, lblTitulo, espacio, insignia);

        VBox fila = new VBox(6, encabezado);
        if (a.getObservacion() != null) {
            Label observacion = new Label(a.getObservacion());
            observacion.setWrapText(true);
            observacion.getStyleClass().add("revision-observacion");
            fila.getChildren().add(observacion);
        }
        Label quien = new Label((a.getUsuarioNombre() == null ? "" : a.getUsuarioNombre() + " · ")
                + Formato.fechaHora(a.getCreatedAt()));
        quien.getStyleClass().add("texto-suave");
        fila.getChildren().add(quien);
        fila.getStyleClass().addAll("tarjeta-interna", "revision",
                !rechazo ? "revision-aprobada" : pendiente ? "revision-pendiente" : "revision-atendida");
        return fila;
    }
}
