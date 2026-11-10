package com.udi.controller.encuestas;

import com.udi.model.CaracteristicaGasto;
import com.udi.model.GrupoAlimento;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.service.catalogos.CatalogoService;
import com.udi.service.encuestas.CaracteristicaService;
import com.udi.util.Formato;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pestaña «Características del gasto en alimentos (Cap. C)». Un renglón por
 * cada grupo de alimentos con la frecuencia de compra; si la frecuencia supera
 * la bisemana se habilitan C.3.1 (valor de la última compra) y C.3.2
 * (¿comprará en los 14 días?). RF-07: los 25 grupos deben quedar completos
 * antes de registrar los gastos diarios.
 */
public class EncuestaCapituloCController implements SeccionEncuesta {

    private static final String TODOS = "Todos los grupos";
    private static final String PENDIENTES = "Solo pendientes";
    private static final String SUPERAN = "Superan la bisemana";

    @FXML
    private Label lblAvance;
    @FXML
    private ProgressBar barAvance;
    @FXML
    private Label lblIncompleto;
    @FXML
    private HBox boxCompleto;
    @FXML
    private ComboBox<String> cmbFiltro;
    @FXML
    private Label lblConteo;
    @FXML
    private VBox filas;
    @FXML
    private Label lblVacio;
    @FXML
    private Button btnDescartar;
    @FXML
    private Button btnGuardar;

    private final CaracteristicaService caracteristicaService = new CaracteristicaService();
    private final CatalogoService catalogoService = new CatalogoService();

    private EncuestaDetalleController detalle;
    private final List<Fila> grupos = new ArrayList<>();
    /** Después de intentar guardar se marcan en rojo los grupos con errores. */
    private boolean intento;

    @FXML
    private void initialize() {
        cmbFiltro.getItems().setAll(TODOS, PENDIENTES, SUPERAN);
        cmbFiltro.setValue(TODOS);
        cmbFiltro.valueProperty().addListener((obs, antes, ahora) -> pintarFilas());
        Formato.mostrar(lblIncompleto, false);
        Formato.mostrar(boxCompleto, false);
        Formato.mostrar(lblVacio, false);
    }

    @Override
    public void mostrar(EncuestaDetalleController detalle) {
        this.detalle = detalle;
        Formato.mostrar(btnDescartar, detalle.esEditable());
        Formato.mostrar(btnGuardar, detalle.esEditable());
        long encuestaId = detalle.getEncuesta().getId();
        Tareas.ejecutar(() -> {
            Datos datos = new Datos();
            datos.grupos = catalogoService.listarGruposAlimento();
            for (CaracteristicaGasto c : caracteristicaService.listarPorEncuesta(encuestaId)) {
                datos.guardadas.put(c.getGrupoAlimentoId(), c);
            }
            return datos;
        }, datos -> {
            grupos.clear();
            for (GrupoAlimento g : datos.grupos) {
                grupos.add(new Fila(g, datos.guardadas.get(g.getId())));
            }
            pintarFilas();
        });
    }

    private void pintarFilas() {
        String filtro = cmbFiltro.getValue();
        filas.getChildren().clear();
        for (Fila f : grupos) {
            boolean incluir = TODOS.equals(filtro)
                    || (PENDIENTES.equals(filtro) && f.error() != null)
                    || (SUPERAN.equals(filtro) && f.supera());
            if (incluir) {
                filas.getChildren().add(f.nodo);
            }
        }
        Formato.mostrar(lblVacio, filas.getChildren().isEmpty());
        lblConteo.setText(filas.getChildren().size() + " de " + grupos.size() + " grupos");
        actualizarAvance();
    }

    private void actualizarAvance() {
        long listos = grupos.stream().filter(f -> f.error() == null).count();
        int total = grupos.size();
        lblAvance.setText(listos + " de " + total + " grupos diligenciados");
        barAvance.setProgress(total == 0 ? 0 : listos / (double) total);
        barAvance.getStyleClass().remove("barra-alerta");
        if (listos < total) {
            barAvance.getStyleClass().add("barra-alerta");
        }
        lblIncompleto.setText("Faltan " + (total - listos) + " grupos por diligenciar. "
                + "Los gastos diarios se habilitan cuando los " + total + " grupos estén completos.");
        Formato.mostrar(lblIncompleto, listos < total);
        Formato.mostrar(boxCompleto, total > 0 && listos == total);
        grupos.forEach(Fila::marcar);
    }

    @FXML
    private void descartar() {
        intento = false;
        detalle.avisar(null);
        detalle.recargarPestana();
    }

    /** Guarda los grupos completos que cambiaron; los incompletos quedan marcados en rojo. */
    @FXML
    private void guardar() {
        intento = true;
        actualizarAvance();
        long encuestaId = detalle.getEncuesta().getId();
        List<CaracteristicaGasto> cambios = new ArrayList<>();
        for (Fila f : grupos) {
            if (f.error() == null && f.cambio()) {
                cambios.add(f.leer(encuestaId));
            }
        }
        long pendientes = grupos.stream().filter(f -> f.error() != null).count();
        long listos = grupos.size() - pendientes;
        Tareas.ejecutar(() -> {
            for (CaracteristicaGasto c : cambios) {
                if (c.getId() == null) {
                    caracteristicaService.crear(c);
                } else {
                    caracteristicaService.actualizar(c);
                }
            }
            return cambios.size();
        }, guardados -> {
            detalle.avisar(pendientes == 0
                    ? "Capítulo C guardado y completo: " + listos + " de " + grupos.size() + " grupos."
                    : "Se guardaron " + listos + " grupos. Quedan " + pendientes + " por completar (marcados en rojo).");
            if (pendientes == 0) {
                detalle.recargarPestana();
            } else {
                // Se actualizan los ids de los renglones nuevos sin perder lo que falta corregir
                mostrarDespuesDeGuardar(encuestaId);
            }
        });
    }

    /** Relee los ids guardados para que el siguiente guardado actualice en lugar de crear. */
    private void mostrarDespuesDeGuardar(long encuestaId) {
        Tareas.ejecutar(() -> caracteristicaService.listarPorEncuesta(encuestaId), guardadas -> {
            Map<Long, CaracteristicaGasto> porGrupo = new HashMap<>();
            guardadas.forEach(c -> porGrupo.put(c.getGrupoAlimentoId(), c));
            grupos.forEach(f -> f.guardada = porGrupo.get(f.grupo.getId()));
        });
    }

    @FXML
    private void irGastos() {
        detalle.ir(EncuestaDetalleController.Pestana.GASTOS);
    }

    private static final class Datos {
        private List<GrupoAlimento> grupos = new ArrayList<>();
        private final Map<Long, CaracteristicaGasto> guardadas = new HashMap<>();
    }

    /** Renglón de un grupo de alimentos con sus controles. */
    private final class Fila {
        private final GrupoAlimento grupo;
        private CaracteristicaGasto guardada;
        private final ComboBox<FrecuenciaCompra> cmbFrecuencia = new ComboBox<>();
        private final TextField txtValor = new TextField();
        private final ToggleGroup compra = new ToggleGroup();
        private final RadioButton rbSi = new RadioButton("Sí");
        private final RadioButton rbNo = new RadioButton("No");
        private final Label lblError = new Label();
        private final HBox nodo;
        private final HBox boxValor;
        private final HBox boxCompra;
        private final Label noAplicaValor = noAplica();
        private final Label noAplicaCompra = noAplica();

        private Fila(GrupoAlimento grupo, CaracteristicaGasto guardada) {
            this.grupo = grupo;
            this.guardada = guardada;

            Label codigo = new Label(String.format("%02d", grupo.getId()));
            codigo.getStyleClass().add("dato-codigo");
            codigo.setPrefWidth(50);
            codigo.setMinWidth(50);

            Label nombre = new Label(grupo.getNombre());
            nombre.setWrapText(true);
            lblError.getStyleClass().add("texto-alerta");
            lblError.setWrapText(true);
            VBox boxNombre = new VBox(2, nombre, lblError);
            HBox.setHgrow(boxNombre, Priority.ALWAYS);
            boxNombre.setMaxWidth(Double.MAX_VALUE);

            cmbFrecuencia.getItems().setAll(FrecuenciaCompra.values());
            cmbFrecuencia.setPromptText("Seleccione…");
            cmbFrecuencia.getStyleClass().add("combo-linea");
            cmbFrecuencia.setPrefWidth(220);
            cmbFrecuencia.setMinWidth(220);

            Formato.soloEnteros(txtValor, 12);
            txtValor.setPromptText("0");
            txtValor.getStyleClass().add("campo-caja");
            Label pesos = new Label("$");
            pesos.getStyleClass().add("texto-suave");
            boxValor = new HBox(6, pesos, txtValor);
            boxValor.setAlignment(Pos.CENTER_LEFT);
            StackPane celdaValor = new StackPane(boxValor, noAplicaValor);
            celdaValor.setAlignment(Pos.CENTER_LEFT);
            celdaValor.setPrefWidth(160);
            celdaValor.setMinWidth(160);

            rbSi.setToggleGroup(compra);
            rbNo.setToggleGroup(compra);
            boxCompra = new HBox(16, rbSi, rbNo);
            boxCompra.setAlignment(Pos.CENTER_LEFT);
            StackPane celdaCompra = new StackPane(boxCompra, noAplicaCompra);
            celdaCompra.setAlignment(Pos.CENTER_LEFT);
            celdaCompra.setPrefWidth(180);
            celdaCompra.setMinWidth(180);

            nodo = new HBox(12, codigo, boxNombre, cmbFrecuencia, celdaValor, celdaCompra);
            nodo.setAlignment(Pos.CENTER_LEFT);
            nodo.getStyleClass().add("grilla-fila");

            if (guardada != null) {
                cmbFrecuencia.setValue(guardada.getFrecuenciaCompra());
                txtValor.setText(guardada.getValorUltimaCompra() == null ? "" : guardada.getValorUltimaCompra().toBigInteger().toString());
                if (guardada.getCompraraEnPeriodo() != null) {
                    (guardada.getCompraraEnPeriodo() ? rbSi : rbNo).setSelected(true);
                }
            }
            habilitar();

            cmbFrecuencia.valueProperty().addListener((obs, antes, ahora) -> {
                if (!supera()) {
                    txtValor.clear();
                    compra.selectToggle(null);
                }
                habilitar();
                actualizarAvance();
            });
            txtValor.textProperty().addListener((obs, antes, ahora) -> actualizarAvance());
            compra.selectedToggleProperty().addListener((obs, antes, ahora) -> actualizarAvance());

            boolean editable = detalle.esEditable();
            cmbFrecuencia.setDisable(!editable);
            txtValor.setDisable(!editable);
            rbSi.setDisable(!editable);
            rbNo.setDisable(!editable);
        }

        private Label noAplica() {
            Label etiqueta = new Label("No aplica");
            etiqueta.getStyleClass().add("texto-suave");
            return etiqueta;
        }

        private boolean supera() {
            return cmbFrecuencia.getValue() != null && cmbFrecuencia.getValue().superaBisemana();
        }

        private void habilitar() {
            boolean supera = supera();
            boxValor.setVisible(supera);
            boxCompra.setVisible(supera);
            noAplicaValor.setVisible(!supera);
            noAplicaCompra.setVisible(!supera);
        }

        private BigDecimal valor() {
            return Formato.leerNumero(txtValor.getText());
        }

        private Boolean comprara() {
            return compra.getSelectedToggle() == null ? null : compra.getSelectedToggle() == rbSi;
        }

        /** Qué le falta al grupo, o null si está completo. */
        private String error() {
            if (cmbFrecuencia.getValue() == null) {
                return "Falta la frecuencia de compra.";
            }
            if (supera() && valor() == null) {
                return "Supera la bisemana: falta el valor de la última compra (3.1).";
            }
            if (supera() && comprara() == null) {
                return "Supera la bisemana: indique si lo comprará en los 14 días (3.2).";
            }
            return null;
        }

        private void marcar() {
            String error = error();
            boolean conError = intento && error != null;
            lblError.setText(conError ? error : "");
            Formato.mostrar(lblError, conError);
            nodo.getStyleClass().removeAll("fila-error", "fila-pendiente");
            if (conError) {
                nodo.getStyleClass().add("fila-error");
            } else if (cmbFrecuencia.getValue() == null) {
                nodo.getStyleClass().add("fila-pendiente");
            }
        }

        /** true si lo que hay en pantalla es distinto de lo guardado. */
        private boolean cambio() {
            if (guardada == null) {
                return true;
            }
            BigDecimal antes = guardada.getValorUltimaCompra();
            boolean mismoValor = antes == null ? valor() == null : valor() != null && antes.compareTo(valor()) == 0;
            return guardada.getFrecuenciaCompra() != cmbFrecuencia.getValue()
                    || !mismoValor
                    || !Objects.equals(guardada.getCompraraEnPeriodo(), comprara());
        }

        private CaracteristicaGasto leer(long encuestaId) {
            CaracteristicaGasto c = new CaracteristicaGasto();
            if (guardada != null) {
                c.setId(guardada.getId());
                c.setEncuestadorId(guardada.getEncuestadorId());
            }
            c.setEncuestaId(encuestaId);
            c.setGrupoAlimentoId(grupo.getId());
            c.setFrecuenciaCompra(cmbFrecuencia.getValue());
            c.setValorUltimaCompra(supera() ? valor() : null);
            c.setCompraraEnPeriodo(supera() ? comprara() : null);
            return c;
        }
    }
}
