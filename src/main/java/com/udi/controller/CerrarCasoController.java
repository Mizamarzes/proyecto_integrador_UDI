package com.udi.controller;

import com.udi.model.Encuesta;
import com.udi.service.EncuestaService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/** Modal que confirma el cierre del caso (RF-06). */
public class CerrarCasoController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private Label lblPeriodo;
    @FXML
    private Label lblResultado;
    @FXML
    private Label lblDias;
    @FXML
    private Label lblVisitas;
    @FXML
    private CheckBox chkConfirma;
    @FXML
    private Label lblError;
    @FXML
    private Button btnCerrar;

    private final EncuestaService encuestaService = new EncuestaService();

    private long encuestaId;
    private Runnable alCerrar;

    @FXML
    private void initialize() {
        Formato.mostrar(lblError, false);
        chkConfirma.selectedProperty().addListener((obs, antes, ahora) -> Formato.mostrar(lblError, false));
    }

    public void preparar(Encuesta e, String hogar, int diasConInformacion, int visitas, Runnable alCerrar) {
        this.encuestaId = e.getId();
        this.alCerrar = alCerrar;
        lblTitulo.setText("¿Cerrar el caso " + Formato.codigo("E", e.getId()) + "?");
        lblSubtitulo.setText(hogar);
        lblPeriodo.setText(Formato.fecha(e.getFechaDesde()) + " – " + Formato.fecha(e.getFechaHasta()));
        lblResultado.setText(e.getResultadoCuadernillo() == null ? "—" : e.getResultadoCuadernillo().toString());
        lblDias.setText(diasConInformacion + " de " + EncuestaService.DIAS_PERIODO);
        lblVisitas.setText(String.valueOf(visitas));
    }

    @FXML
    private void cerrar() {
        if (!chkConfirma.isSelected()) {
            Formato.mostrar(lblError, true);
            return;
        }
        Tareas.accion(() -> encuestaService.cerrar(encuestaId), () -> {
            Modal.cerrar(btnCerrar);
            alCerrar.run();
        });
    }

    @FXML
    private void cancelar() {
        Modal.cerrar(btnCerrar);
    }
}
