package com.udi.controller;

import com.udi.model.Modulo;
import com.udi.model.Rol;
import com.udi.service.RolService;
import com.udi.util.Alertas;
import com.udi.util.Formato;
import com.udi.util.Modal;
import com.udi.util.Tareas;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Roles y permisos: matriz de roles (filas) por módulos (columnas). Cada
 * casilla es una fila de rol_modulo. El checkbox del rol marca toda su fila y
 * el del módulo toda su columna. Los cambios se guardan juntos con
 * {@link RolService#guardarPermisos}; el rol ADMIN se muestra bloqueado.
 */
public class RolController {

    private static final String VISTA_FORMULARIO = "modulos/roles/partials/rol_form";
    private static final double ANCHO_ROL = 230;
    private static final double ANCHO_MODULO = 112;

    @FXML
    private GridPane matriz;
    @FXML
    private Button btnGuardar;
    @FXML
    private Label lblEstado;

    private final RolService rolService = new RolService();

    private List<Rol> roles = new ArrayList<>();
    private List<Modulo> modulos = new ArrayList<>();
    /** Permisos como están en la base de datos, para saber si hay cambios sin guardar. */
    private Map<Long, Set<Long>> guardado = new HashMap<>();

    /** rolId → moduloId → casilla, solo de los roles editables. */
    private final Map<Long, Map<Long, CheckBox>> casillas = new LinkedHashMap<>();
    private final Map<Long, CheckBox> checksRol = new HashMap<>();
    private final Map<Long, CheckBox> checksModulo = new HashMap<>();
    private final Map<Long, Label> detallesRol = new HashMap<>();
    private boolean recienGuardado;

    @FXML
    private void initialize() {
        btnGuardar.setDisable(true);
        cargar();
    }

    private void cargar() {
        Tareas.ejecutar(() -> {
            Datos datos = new Datos();
            datos.roles = rolService.listarRoles();
            datos.modulos = rolService.listarModulos();
            datos.permisos = rolService.matrizPermisos();
            return datos;
        }, datos -> {
            roles = datos.roles;
            modulos = datos.modulos;
            guardado = datos.permisos;
            pintar();
        });
    }

    // ------------------------------------------------------------------
    // Matriz
    // ------------------------------------------------------------------

    private void pintar() {
        matriz.getChildren().clear();
        matriz.getColumnConstraints().clear();
        casillas.clear();
        checksRol.clear();
        checksModulo.clear();
        detallesRol.clear();

        ColumnConstraints colRol = new ColumnConstraints(ANCHO_ROL);
        colRol.setHgrow(Priority.SOMETIMES);
        matriz.getColumnConstraints().add(colRol);
        for (int i = 0; i < modulos.size(); i++) {
            ColumnConstraints col = new ColumnConstraints(ANCHO_MODULO);
            col.setHgrow(Priority.ALWAYS);
            matriz.getColumnConstraints().add(col);
        }

        // Encabezado: "Roles" y un checkbox por módulo que marca la columna
        Label titulo = new Label("Roles");
        HBox celdaTitulo = celda(titulo, Pos.BOTTOM_LEFT, "matriz-encabezado", "matriz-encabezado-rol");
        matriz.add(celdaTitulo, 0, 0);
        for (int c = 0; c < modulos.size(); c++) {
            Modulo modulo = modulos.get(c);
            Label nombre = new Label(modulo.getNombre());
            nombre.setWrapText(true);
            nombre.setMaxWidth(ANCHO_MODULO - 16);
            CheckBox check = new CheckBox();
            check.setAccessibleText("Marcar " + modulo.getNombre() + " para todos los roles");
            check.setOnAction(e -> marcarColumna(modulo.getId(), check.isSelected()));
            checksModulo.put(modulo.getId(), check);
            VBox caja = new VBox(10, nombre, check);
            caja.setAlignment(Pos.BOTTOM_CENTER);
            matriz.add(celda(caja, Pos.BOTTOM_CENTER, "matriz-encabezado"), c + 1, 0);
        }

        // Filas: una por rol
        for (int f = 0; f < roles.size(); f++) {
            pintarFila(roles.get(f), f + 1);
        }
        refrescar();
    }

    private void pintarFila(Rol rol, int fila) {
        boolean admin = Rol.ADMIN.equals(rol.getNombre());
        String[] clases = fila % 2 == 0
                ? new String[]{"matriz-celda", "matriz-fila-par"} : new String[]{"matriz-celda"};

        CheckBox checkRol = new CheckBox();
        checkRol.setAccessibleText(Formato.nombreRol(rol.getNombre()) + ": marcar todos los módulos");
        Label nombre = new Label(Formato.nombreRol(rol.getNombre()));
        nombre.getStyleClass().add("matriz-rol-nombre");
        Label detalle = new Label(admin ? "Acceso total" : "");
        detalle.getStyleClass().add("matriz-rol-detalle");
        VBox textos = new VBox(2, nombre, detalle);
        if (!admin) {
            Hyperlink renombrar = new Hyperlink("Renombrar");
            renombrar.getStyleClass().add("enlace");
            renombrar.setOnAction(e -> renombrarRol(rol));
            Hyperlink eliminar = new Hyperlink("Eliminar");
            eliminar.getStyleClass().add("enlace");
            eliminar.setOnAction(e -> eliminarRol(rol));
            textos.getChildren().add(new HBox(10, renombrar, eliminar));
        }
        HBox cabecera = new HBox(10, checkRol, textos);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        matriz.add(celda(cabecera, Pos.CENTER_LEFT, clases), 0, fila);

        if (admin) {
            checkRol.setSelected(true);
            checkRol.setDisable(true);
        } else {
            checkRol.setOnAction(e -> marcarFila(rol.getId(), checkRol.isSelected()));
            checksRol.put(rol.getId(), checkRol);
            detallesRol.put(rol.getId(), detalle);
            casillas.put(rol.getId(), new LinkedHashMap<>());
        }

        Set<Long> permisos = guardado.getOrDefault(rol.getId(), Collections.emptySet());
        for (int c = 0; c < modulos.size(); c++) {
            Modulo modulo = modulos.get(c);
            CheckBox check = new CheckBox();
            check.setAccessibleText(Formato.nombreRol(rol.getNombre()) + " · " + modulo.getNombre());
            if (admin) {
                check.setSelected(true);
                check.setDisable(true);
            } else {
                check.setSelected(permisos.contains(modulo.getId()));
                check.selectedProperty().addListener((obs, antes, ahora) -> {
                    recienGuardado = false;
                    refrescar();
                });
                casillas.get(rol.getId()).put(modulo.getId(), check);
            }
            matriz.add(celda(check, Pos.CENTER, clases), c + 1, fila);
        }
    }

    private static HBox celda(Node contenido, Pos alineacion, String... clases) {
        HBox caja = new HBox(contenido);
        caja.setAlignment(alineacion);
        caja.setMaxHeight(Double.MAX_VALUE);
        caja.getStyleClass().addAll(clases);
        GridPane.setFillHeight(caja, true);
        GridPane.setFillWidth(caja, true);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private void marcarFila(long rolId, boolean valor) {
        casillas.get(rolId).values().forEach(c -> c.setSelected(valor));
    }

    private void marcarColumna(long moduloId, boolean valor) {
        casillas.values().forEach(fila -> fila.get(moduloId).setSelected(valor));
    }

    /** Actualiza los checkboxes de fila y columna, los contadores y el estado de guardado. */
    private void refrescar() {
        for (Map.Entry<Long, Map<Long, CheckBox>> fila : casillas.entrySet()) {
            long marcadas = fila.getValue().values().stream().filter(CheckBox::isSelected).count();
            agregado(checksRol.get(fila.getKey()), marcadas, fila.getValue().size());
            detallesRol.get(fila.getKey()).setText(marcadas + " de " + modulos.size() + " módulos");
        }
        for (Map.Entry<Long, CheckBox> columna : checksModulo.entrySet()) {
            long marcadas = casillas.values().stream().filter(f -> f.get(columna.getKey()).isSelected()).count();
            agregado(columna.getValue(), marcadas, casillas.size());
            columna.getValue().setDisable(casillas.isEmpty());
        }

        boolean cambios = hayCambios();
        btnGuardar.setDisable(!cambios);
        lblEstado.getStyleClass().removeAll("pendiente", "guardado");
        if (cambios) {
            lblEstado.setText("Hay cambios sin guardar");
            lblEstado.getStyleClass().add("pendiente");
        } else if (recienGuardado) {
            lblEstado.setText("Permisos guardados");
            lblEstado.getStyleClass().add("guardado");
        } else {
            lblEstado.setText("Sin cambios");
        }
    }

    /** Marcado si están todas, indeterminado si están algunas. */
    private static void agregado(CheckBox check, long marcadas, int total) {
        check.setIndeterminate(marcadas > 0 && marcadas < total);
        check.setSelected(total > 0 && marcadas == total);
    }

    /** Permisos marcados en pantalla de los roles editables. */
    private Map<Long, Set<Long>> leer() {
        Map<Long, Set<Long>> permisos = new HashMap<>();
        casillas.forEach((rolId, fila) -> {
            Set<Long> marcados = new HashSet<>();
            fila.forEach((moduloId, check) -> {
                if (check.isSelected()) {
                    marcados.add(moduloId);
                }
            });
            permisos.put(rolId, marcados);
        });
        return permisos;
    }

    private Map<Long, Set<Long>> soloEditables(Map<Long, Set<Long>> permisos) {
        Map<Long, Set<Long>> resultado = new HashMap<>();
        for (Long rolId : casillas.keySet()) {
            resultado.put(rolId, permisos.getOrDefault(rolId, Collections.emptySet()));
        }
        return resultado;
    }

    private boolean hayCambios() {
        return !leer().equals(soloEditables(guardado));
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    @FXML
    private void guardar() {
        Map<Long, Set<Long>> permisos = leer();
        Tareas.accion(() -> rolService.guardarPermisos(permisos), () -> {
            guardado = new HashMap<>(permisos);
            recienGuardado = true;
            refrescar();
        });
    }

    @FXML
    private void nuevoRol() {
        if (descartarCambios()) {
            abrirFormulario(null, creado -> cargar());
        }
    }

    private void renombrarRol(Rol rol) {
        if (descartarCambios()) {
            abrirFormulario(rol, guardadoRol -> cargar());
        }
    }

    private void eliminarRol(Rol rol) {
        if (!descartarCambios()) {
            return;
        }
        if (Alertas.confirmar("Eliminar rol", "¿Eliminar el rol " + Formato.nombreRol(rol.getNombre())
                + " y sus permisos? No se puede si hay usuarios con ese rol.")) {
            Tareas.ejecutar(() -> rolService.eliminarRol(rol.getId()), eliminado -> cargar());
        }
    }

    private void abrirFormulario(Rol rol, Consumer<Rol> alGuardar) {
        Modal<RolFormController> modal = Modal.cargar(VISTA_FORMULARIO, rol == null ? "Nuevo rol" : "Renombrar rol");
        modal.controlador().preparar(rol, alGuardar);
        modal.mostrar();
    }

    /** Al recargar la matriz se pierden los cambios sin guardar; se pide confirmación. */
    private boolean descartarCambios() {
        return !hayCambios() || Alertas.confirmar("Cambios sin guardar",
                "Hay permisos sin guardar que se perderán. ¿Desea continuar?");
    }

    private static final class Datos {
        private List<Rol> roles;
        private List<Modulo> modulos;
        private Map<Long, Set<Long>> permisos;
    }
}
