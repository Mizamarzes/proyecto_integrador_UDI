package com.udi.componentes;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Pie de tabla paginada en memoria, con el mismo diseño que «Rendimiento por
 * encuestador» de Reportes: el total a la izquierda y «« ‹ Anterior · Página x
 * de y · Siguiente › »» a la derecha. Se coloca debajo de la tabla en el FXML
 * y se enlaza con {@link #vincular}: la tabla muestra solo la página actual de
 * la lista (normalmente un {@code FilteredList}) y ordenar por una columna
 * ordena la lista completa, no solo la página visible.
 * <p>
 * Opcionalmente repite una fila al final de cada página ({@link #setFilaFinal},
 * p. ej. el Total de Reportes) y fija el alto de la tabla al de una página
 * ({@link #ajustarAlto}) para que la tabla no crezca ni los botones se muevan.
 */
public class Paginador<T> extends HBox {

    public static final int FILAS_POR_PAGINA = 10;

    private final Label lblResumen = new Label();
    private final Label lblPagina = new Label();
    private final Button btnPrimera = boton("«");
    private final Button btnAnterior = boton("‹ Anterior");
    private final Button btnSiguiente = boton("Siguiente ›");
    private final Button btnUltima = boton("»");

    /** Filas de la página visible; es lo que muestra la tabla. */
    private final ObservableList<T> pagina = FXCollections.observableArrayList();
    private SortedList<T> ordenadas;
    private TableView<T> tabla;
    /** Fila que se agrega al final de cada página sin contar en el total; null = ninguna. */
    private Supplier<T> filaFinal;
    /** Alto de cada fila para fijar el alto de la tabla; 0 = la tabla toma su alto del layout. */
    private double altoFila;
    private int filasPorPagina = FILAS_POR_PAGINA;
    private int paginaActual;
    private String singular = "registro";
    private String plural = "registros";

    public Paginador() {
        getStyleClass().add("paginacion");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8.0);
        lblResumen.getStyleClass().add("texto-suave");
        lblResumen.setMinWidth(Region.USE_PREF_SIZE);
        lblPagina.getStyleClass().add("paginacion-pagina");
        lblPagina.setMinWidth(Region.USE_PREF_SIZE);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        getChildren().addAll(lblResumen, espacio, btnPrimera, btnAnterior, lblPagina, btnSiguiente, btnUltima);

        btnPrimera.setOnAction(e -> irA(0));
        btnAnterior.setOnAction(e -> irA(paginaActual - 1));
        btnSiguiente.setOnAction(e -> irA(paginaActual + 1));
        btnUltima.setOnAction(e -> irA(totalPaginas() - 1));
        mostrar();
    }

    /**
     * Muestra en la tabla las filas de {@code fuente} por páginas.
     *
     * @param singular nombre de un registro para el resumen («encuesta»)
     * @param plural   nombre de varios registros («encuestas»)
     */
    public void vincular(TableView<T> tabla, ObservableList<T> fuente, String singular, String plural) {
        this.singular = singular;
        this.plural = plural;
        this.tabla = tabla;
        ordenadas = new SortedList<>(fuente);
        // La tabla solo calcula el comparador de sus columnas; el orden se aplica
        // a la lista completa y luego se toma la página
        ordenadas.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setSortPolicy(t -> true);
        tabla.setItems(pagina);
        // Filtrar, recargar u ordenar vuelve a la primera página
        ordenadas.addListener((ListChangeListener<T>) c -> irA(0));
        irA(0);
    }

    /** Repite al final de cada página la fila que da {@code fila} (se pide de nuevo al cambiar de página). */
    public void setFilaFinal(Supplier<T> fila) {
        filaFinal = fila;
        mostrar();
    }

    /**
     * Fija el alto de la tabla a sus filas de alto {@code alto} (encabezado y
     * fila final incluidos). Con varias páginas mantiene el alto de una página
     * llena, así el pie no se mueve en la última.
     */
    public void ajustarAlto(double alto) {
        altoFila = alto;
        if (tabla != null) {
            tabla.setFixedCellSize(alto);
        }
        mostrar();
    }

    public int getFilasPorPagina() {
        return filasPorPagina;
    }

    public void setFilasPorPagina(int filas) {
        filasPorPagina = Math.max(1, filas);
        irA(0);
    }

    private void irA(int nueva) {
        paginaActual = nueva;
        mostrar();
    }

    private void mostrar() {
        int total = ordenadas == null ? 0 : ordenadas.size();
        int paginas = totalPaginas();
        paginaActual = Math.max(0, Math.min(paginaActual, paginas - 1));
        int desde = paginaActual * filasPorPagina;
        int hasta = Math.min(desde + filasPorPagina, total);
        if (ordenadas != null) {
            List<T> filas = new ArrayList<>(ordenadas.subList(desde, hasta));
            T fin = filaFinal == null ? null : filaFinal.get();
            if (fin != null) {
                filas.add(fin);
            }
            pagina.setAll(filas);
        }
        if (tabla != null && altoFila > 0) {
            int visibles = paginas > 1 ? filasPorPagina : total;
            int extra = 1 + (filaFinal != null ? 1 : 0); // encabezado y fila final
            tabla.setFixedCellSize(altoFila);
            tabla.setPrefHeight(altoFila * (visibles + extra) + 4);
        }

        String sufijo = total == 1 ? " " + singular : " " + plural;
        lblResumen.setText(paginas > 1
                ? "Mostrando " + (desde + 1) + "–" + hasta + " de " + total + sufijo
                : total + sufijo);
        lblPagina.setText("Página " + (paginaActual + 1) + " de " + paginas);
        btnPrimera.setDisable(paginaActual == 0);
        btnAnterior.setDisable(paginaActual == 0);
        btnSiguiente.setDisable(paginaActual >= paginas - 1);
        btnUltima.setDisable(paginaActual >= paginas - 1);
    }

    private int totalPaginas() {
        int total = ordenadas == null ? 0 : ordenadas.size();
        return Math.max(1, (total + filasPorPagina - 1) / filasPorPagina);
    }

    private static Button boton(String texto) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("boton-secundario");
        boton.setMinWidth(Region.USE_PREF_SIZE);
        return boton;
    }
}
