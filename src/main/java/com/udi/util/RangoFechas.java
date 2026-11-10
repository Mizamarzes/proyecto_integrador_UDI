package com.udi.util;

import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Filtro «desde / hasta» de los listados, hecho con dos DatePicker. Ambos
 * extremos son opcionales e inclusivos, y «hasta» no puede quedar antes de
 * «desde»: si se cruzan, el otro se iguala al que se acaba de elegir.
 */
public final class RangoFechas {

    private final DatePicker desde;
    private final DatePicker hasta;

    public RangoFechas(DatePicker desde, DatePicker hasta, Runnable alCambiar) {
        this.desde = desde;
        this.hasta = hasta;
        desde.setConverter(Formato.convertidorFecha());
        hasta.setConverter(Formato.convertidorFecha());
        desde.valueProperty().addListener((obs, antes, d) -> {
            if (d != null && hasta.getValue() != null && hasta.getValue().isBefore(d)) {
                hasta.setValue(d);
            }
            alCambiar.run();
        });
        hasta.valueProperty().addListener((obs, antes, h) -> {
            if (h != null && desde.getValue() != null && desde.getValue().isAfter(h)) {
                desde.setValue(h);
            }
            alCambiar.run();
        });
    }

    /** true si la fecha cae en el rango; sin fecha solo pasa cuando no hay rango. */
    public boolean incluye(LocalDateTime fecha) {
        LocalDate d = desde.getValue();
        LocalDate h = hasta.getValue();
        if (d == null && h == null) {
            return true;
        }
        if (fecha == null) {
            return false;
        }
        LocalDate dia = fecha.toLocalDate();
        return (d == null || !dia.isBefore(d)) && (h == null || !dia.isAfter(h));
    }

    public void limpiar() {
        desde.setValue(null);
        hasta.setValue(null);
    }

    /** Celda que muestra una fecha y hora como dd/mm/aaaa; la columna se ordena por la fecha real. */
    public static <S> TableCell<S, LocalDateTime> celdaFecha() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setText(vacia || fecha == null ? null : Formato.fecha(fecha.toLocalDate()));
            }
        };
    }
}
