package com.udi.util;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Formatos de fechas y códigos que se muestran en pantalla. */
public final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ES = new Locale("es", "CO");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HORA_ENTRADA = DateTimeFormatter.ofPattern("H:mm");

    private Formato() {
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : FECHA_HORA.format(fecha);
    }

    /** lunes 28 de septiembre de 2026 */
    public static String fechaLarga(LocalDate fecha) {
        return fecha == null ? "" : FECHA_LARGA.format(fecha);
    }

    /** 28/09 */
    public static String fechaCorta(LocalDate fecha) {
        return fecha == null ? "" : FECHA_CORTA.format(fecha);
    }

    /** 09:45 (24 horas) */
    public static String hora(LocalDateTime hora) {
        return hora == null ? "" : HORA.format(hora);
    }

    /** Lee una hora escrita como 9:45 o 09:45; null si está vacía o no es válida. */
    public static LocalTime leerHora(String texto) {
        try {
            return texto == null || texto.trim().isEmpty() ? null : LocalTime.parse(texto.trim(), HORA_ENTRADA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** $ 12.500 */
    public static String pesos(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        NumberFormat formato = NumberFormat.getIntegerInstance(ES);
        return "$ " + formato.format(valor);
    }

    /** 1.50 → 1.5, 2.00 → 2 */
    public static String numero(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }

    /** Lee un número escrito con punto o coma decimal; null si está vacío o no es válido. */
    public static BigDecimal leerNumero(String texto) {
        try {
            return texto == null || texto.trim().isEmpty() ? null : new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Deja escribir solo números enteros de hasta {@code digitos} cifras. */
    public static void soloEnteros(TextField campo, int digitos) {
        campo.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().matches("\\d{0," + digitos + "}") ? c : null));
    }

    /** Deja escribir números con decimales (punto o coma). */
    public static void soloDecimales(TextField campo) {
        campo.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().matches("\\d{0,12}([.,]\\d{0,3})?") ? c : null));
    }

    /** Deja escribir una hora HH:mm. */
    public static void soloHora(TextField campo) {
        campo.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().matches("\\d{0,2}(:\\d{0,2})?") ? c : null));
    }

    /** Muestra u oculta un nodo sin dejar su espacio en el contenedor. */
    public static void mostrar(Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    /** Texto recortado, o null si está vacío. */
    public static String texto(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
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
