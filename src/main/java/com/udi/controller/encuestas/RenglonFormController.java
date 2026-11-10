package com.udi.controller.encuestas;

import com.udi.model.ComidaFueraHogar;
import com.udi.model.GastoDiario;
import com.udi.model.LugarCompra;
import com.udi.model.RegistroDiario;
import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.model.enums.TipoComida;
import com.udi.model.enums.TipoGasto;
import com.udi.model.enums.UnidadMedida;
import com.udi.service.encuestas.GastoDiarioService;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal de un renglón del día: gasto diario (tabla D) o comida preparada
 * fuera del hogar (tabla D1). Los campos se habilitan según las reglas del
 * cuadernillo: equivalencias solo con la unidad 8 Otra, lugar y frecuencia
 * solo si fue compra, gasto personal solo los días 1 a 7.
 */
public class RenglonFormController {

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblNumero;
    @FXML
    private Label lblSubtitulo;
    @FXML
    private GridPane gridGasto;
    @FXML
    private TextField txtArticulo;
    @FXML
    private TextField txtCantidad;
    @FXML
    private ComboBox<UnidadMedida> cmbUnidad;
    @FXML
    private TextField txtUnidadOtra;
    @FXML
    private TextField txtEqGr;
    @FXML
    private TextField txtEqCm3;
    @FXML
    private TextField txtMesada;
    @FXML
    private VBox boxComida;
    @FXML
    private TextField txtDescripcion;
    @FXML
    private TextField txtCantidadComida;
    @FXML
    private CheckBox chkDomicilio;
    @FXML
    private ToggleGroup grupoComida;
    @FXML
    private RadioButton rbDesayuno;
    @FXML
    private RadioButton rbAlmuerzo;
    @FXML
    private RadioButton rbCena;
    @FXML
    private RadioButton rbOnces;
    @FXML
    private ComboBox<FormaAdquisicion> cmbForma;
    @FXML
    private TextField txtFormaOtra;
    @FXML
    private Label lblValor;
    @FXML
    private TextField txtValor;
    @FXML
    private ComboBox<LugarCompra> cmbLugar;
    @FXML
    private ComboBox<FrecuenciaCompra> cmbFrecuencia;
    @FXML
    private ToggleGroup grupoTipo;
    @FXML
    private RadioButton rbHogar;
    @FXML
    private RadioButton rbPersonal;
    @FXML
    private Label lblNotaPersonal;
    @FXML
    private Label lblError;
    @FXML
    private Button btnGuardar;

    private final GastoDiarioService gastoService = new GastoDiarioService();

    private RegistroDiario registro;
    private boolean esGasto;
    private Consumer<String> alGuardar;

    @FXML
    private void initialize() {
        rbDesayuno.setUserData(TipoComida.DESAYUNO);
        rbAlmuerzo.setUserData(TipoComida.ALMUERZO);
        rbCena.setUserData(TipoComida.CENA);
        rbOnces.setUserData(TipoComida.ONCES);

        Formato.soloDecimales(txtCantidad);
        Formato.soloDecimales(txtCantidadComida);
        Formato.soloDecimales(txtEqGr);
        Formato.soloDecimales(txtEqCm3);
        Formato.soloEnteros(txtValor, 12);

        cmbUnidad.getItems().setAll(UnidadMedida.values());
        cmbForma.getItems().setAll(FormaAdquisicion.values());
        List<FrecuenciaCompra> frecuencias = new ArrayList<>(List.of(FrecuenciaCompra.values()));
        // «Nunca» solo existe en el capítulo C
        frecuencias.remove(FrecuenciaCompra.NUNCA);
        cmbFrecuencia.getItems().setAll(frecuencias);
        cmbLugar.setConverter(new StringConverter<>() {
            @Override
            public String toString(LugarCompra lugar) {
                return lugar == null ? "" : lugar.getId() + " · " + lugar.getNombre();
            }

            @Override
            public LugarCompra fromString(String texto) {
                return null;
            }
        });

        cmbUnidad.valueProperty().addListener((obs, antes, ahora) -> habilitar());
        cmbForma.valueProperty().addListener((obs, antes, ahora) -> habilitar());
        cmbForma.setValue(FormaAdquisicion.COMPRA);
        Formato.mostrar(lblError, false);
        habilitar();
    }

    /** Gasto nuevo de la tabla D del día. */
    public void gasto(RegistroDiario registro, int numero, List<LugarCompra> lugares, Consumer<String> alGuardar) {
        preparar(registro, numero, lugares, alGuardar, true);
        lblTitulo.setText("Agregar gasto (tabla D)");
        lblSubtitulo.setText("D. Gastos diarios · Día " + registro.getNumeroDia() + ", " + Formato.fechaLarga(registro.getFecha()) + ".");
        btnGuardar.setText("Guardar gasto");
    }

    /** Comida nueva de la tabla D1 del día. */
    public void comida(RegistroDiario registro, int numero, List<LugarCompra> lugares, Consumer<String> alGuardar) {
        preparar(registro, numero, lugares, alGuardar, false);
        lblTitulo.setText("Agregar comida (tabla D1)");
        lblSubtitulo.setText("D1. Comidas preparadas fuera del hogar · Día " + registro.getNumeroDia() + ", "
                + Formato.fechaLarga(registro.getFecha()) + ".");
        btnGuardar.setText("Guardar comida");
    }

    private void preparar(RegistroDiario registro, int numero, List<LugarCompra> lugares, Consumer<String> alGuardar, boolean esGasto) {
        this.registro = registro;
        this.alGuardar = alGuardar;
        this.esGasto = esGasto;
        lblNumero.setText("N.º " + numero);
        cmbLugar.getItems().setAll(lugares);
        Formato.mostrar(gridGasto, esGasto);
        Formato.mostrar(boxComida, !esGasto);
        boolean admitePersonal = registro.getNumeroDia() <= GastoDiarioService.ULTIMO_DIA_GASTOS_PERSONALES;
        rbPersonal.setDisable(!admitePersonal);
        lblNotaPersonal.setText(admitePersonal
                ? "Días 1 a " + GastoDiarioService.ULTIMO_DIA_GASTOS_PERSONALES + ": admite gastos personales."
                : "Desde el día " + (GastoDiarioService.ULTIMO_DIA_GASTOS_PERSONALES + 1) + " solo gastos del hogar.");
    }

    /** Habilita los campos que dependen de la unidad y de la forma de adquisición. */
    private void habilitar() {
        boolean unidadOtra = cmbUnidad.getValue() == UnidadMedida.OTRA;
        txtUnidadOtra.setDisable(!unidadOtra);
        txtEqGr.setDisable(!unidadOtra);
        txtEqCm3.setDisable(!unidadOtra);
        if (!unidadOtra) {
            txtUnidadOtra.clear();
            txtEqGr.clear();
            txtEqCm3.clear();
        }

        FormaAdquisicion forma = cmbForma.getValue();
        boolean formaOtra = forma == FormaAdquisicion.OTRA;
        txtFormaOtra.setDisable(!formaOtra);
        if (!formaOtra) {
            txtFormaOtra.clear();
        }
        boolean compra = forma != null && forma.esCompra();
        cmbLugar.setDisable(!compra);
        cmbFrecuencia.setDisable(!compra);
        if (!compra) {
            cmbLugar.setValue(null);
            cmbFrecuencia.setValue(null);
        }
        cmbLugar.setPromptText(compra ? "Seleccione" : "No aplica: no fue compra");
        cmbFrecuencia.setPromptText(compra ? "Seleccione" : "No aplica: no fue compra");
        lblValor.setText(compra || forma == null ? "(8) Valor pagado *" : "(8) Valor estimado (si tuviera que comprarlo) *");
    }

    @FXML
    private void guardar() {
        String error = validar();
        if (error != null) {
            lblError.setText(error);
            Formato.mostrar(lblError, true);
            return;
        }
        Formato.mostrar(lblError, false);
        if (esGasto) {
            GastoDiario gasto = leerGasto();
            Tareas.ejecutar(() -> gastoService.crearGasto(gasto), guardado -> terminar(
                    "Gasto " + guardado.getNumeroOrden() + " agregado a la tabla D del día " + registro.getNumeroDia() + "."));
        } else {
            ComidaFueraHogar comida = leerComida();
            Tareas.ejecutar(() -> gastoService.crearComida(comida), guardada -> terminar(
                    "Comida " + guardada.getNumeroOrden() + " agregada a la tabla D1 del día " + registro.getNumeroDia() + "."));
        }
    }

    /** Revisión rápida de los obligatorios; las reglas completas las aplica el servicio. */
    private String validar() {
        if (esGasto) {
            if (Formato.texto(txtArticulo.getText()) == null) {
                return "Escriba el nombre del artículo o servicio (4).";
            }
            if (Formato.leerNumero(txtCantidad.getText()) == null) {
                return "Escriba la cantidad adquirida (5.1).";
            }
            if (cmbUnidad.getValue() == null) {
                return "Seleccione la unidad de medida (5.2).";
            }
            if (cmbUnidad.getValue() == UnidadMedida.OTRA && Formato.texto(txtUnidadOtra.getText()) == null) {
                return "Escriba cuál es la otra unidad de medida (5.2).";
            }
        } else {
            if (Formato.texto(txtDescripcion.getText()) == null) {
                return "Escriba la descripción de la comida (4).";
            }
            if (Formato.leerNumero(txtCantidadComida.getText()) == null) {
                return "Escriba la cantidad adquirida (5).";
            }
            if (grupoComida.getSelectedToggle() == null) {
                return "Seleccione el tipo de comida (4.1).";
            }
        }
        if (cmbForma.getValue() == null) {
            return "Seleccione la forma de adquisición (6).";
        }
        if (cmbForma.getValue() == FormaAdquisicion.OTRA && Formato.texto(txtFormaOtra.getText()) == null) {
            return "Escriba cuál es la otra forma de adquisición (6).";
        }
        if (cmbForma.getValue().esCompra() && cmbFrecuencia.getValue() == null) {
            return "Seleccione la frecuencia de compra (9).";
        }
        if (Formato.leerNumero(txtValor.getText()) == null) {
            return "Escriba el valor pagado o estimado (8).";
        }
        return null;
    }

    private GastoDiario leerGasto() {
        GastoDiario g = new GastoDiario();
        g.setRegistroDiarioId(registro.getId());
        g.setNombreArticulo(Formato.texto(txtArticulo.getText()));
        g.setCantidad(Formato.leerNumero(txtCantidad.getText()));
        g.setUnidadMedida(cmbUnidad.getValue());
        g.setUnidadMedidaOtra(Formato.texto(txtUnidadOtra.getText()));
        g.setEquivalenciaGr(Formato.leerNumero(txtEqGr.getText()));
        g.setEquivalenciaCm3(Formato.leerNumero(txtEqCm3.getText()));
        g.setBeneficiarioMesada(Formato.texto(txtMesada.getText()));
        g.setFormaAdquisicion(cmbForma.getValue());
        g.setFormaAdquisicionOtra(Formato.texto(txtFormaOtra.getText()));
        g.setLugarCompraId(cmbLugar.getValue() == null ? null : cmbLugar.getValue().getId());
        g.setFrecuenciaCompra(cmbFrecuencia.getValue());
        g.setValor(valor());
        g.setTipoGasto(tipo());
        return g;
    }

    private ComidaFueraHogar leerComida() {
        ComidaFueraHogar m = new ComidaFueraHogar();
        m.setRegistroDiarioId(registro.getId());
        m.setDescripcion(Formato.texto(txtDescripcion.getText()));
        m.setCantidad(Formato.leerNumero(txtCantidadComida.getText()));
        m.setTipoComida((TipoComida) grupoComida.getSelectedToggle().getUserData());
        m.setADomicilio(chkDomicilio.isSelected());
        m.setFormaAdquisicion(cmbForma.getValue());
        m.setFormaAdquisicionOtra(Formato.texto(txtFormaOtra.getText()));
        m.setLugarCompraId(cmbLugar.getValue() == null ? null : cmbLugar.getValue().getId());
        m.setFrecuenciaCompra(cmbFrecuencia.getValue());
        m.setValor(valor());
        m.setTipoGasto(tipo());
        return m;
    }

    private BigDecimal valor() {
        return Formato.leerNumero(txtValor.getText());
    }

    private TipoGasto tipo() {
        return grupoTipo.getSelectedToggle() == rbPersonal ? TipoGasto.PERSONAL : TipoGasto.HOGAR;
    }

    private void terminar(String mensaje) {
        Modal.cerrar(btnGuardar);
        alGuardar.accept(mensaje);
    }

    @FXML
    private void cancelar() {
        Modal.cerrar(btnGuardar);
    }
}
