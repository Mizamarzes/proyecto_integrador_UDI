package com.udi.util;

import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Formatos de fechas y códigos que se muestran en pantalla. */
public final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formato() {
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : FECHA_HORA.format(fecha);
    }

    /** Código visible de un registro, por ejemplo E-000123 para la encuesta 123. */
    public static String codigo(String prefijo, Long id) {
        return id == null ? "" : String.format("%s-%06d", prefijo, id);
    }

    /** ENCUESTADOR → Encuestador, CRITICO_DIGITADOR → Critico digitador. */
    public static String nombreRol(String rol) {
        if (rol == null || rol.isEmpty()) {
            return "";
        }
        String texto = rol.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** Convertidor dd/MM/yyyy para los DatePicker. */
    public static StringConverter<LocalDate> convertidorFecha() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                try {
                    return texto == null || texto.trim().isEmpty() ? null : LocalDate.parse(texto.trim(), FECHA);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
    }
}
