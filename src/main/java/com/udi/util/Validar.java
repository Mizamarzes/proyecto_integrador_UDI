package com.udi.util;

import com.udi.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.util.Optional;

/** Validaciones comunes de los servicios. Lanzan {@link ReglaNegocioException}. */
public final class Validar {

    private Validar() {
    }

    public static void requerido(Object valor, String campo) {
        if (valor == null) {
            throw new ReglaNegocioException("El campo «" + campo + "» es obligatorio.");
        }
    }

    public static void requerido(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ReglaNegocioException("El campo «" + campo + "» es obligatorio.");
        }
    }

    public static void vacio(Object valor, String campo, String motivo) {
        if (valor != null && !(valor instanceof String && ((String) valor).trim().isEmpty())) {
            throw new ReglaNegocioException("El campo «" + campo + "» debe quedar vacío " + motivo + ".");
        }
    }

    public static void rango(Integer valor, int minimo, int maximo, String campo) {
        requerido(valor, campo);
        if (valor < minimo || valor > maximo) {
            throw new ReglaNegocioException("El campo «" + campo + "» debe estar entre " + minimo + " y " + maximo + ".");
        }
    }

    public static void positivo(BigDecimal valor, String campo) {
        requerido(valor, campo);
        if (valor.signum() <= 0) {
            throw new ReglaNegocioException("El campo «" + campo + "» debe ser mayor que cero.");
        }
    }

    public static void noNegativo(BigDecimal valor, String campo) {
        if (valor != null && valor.signum() < 0) {
            throw new ReglaNegocioException("El campo «" + campo + "» no puede ser negativo.");
        }
    }

    /** Devuelve el valor si existe; si no, lanza la excepción con el mensaje. */
    public static <T> T encontrado(Optional<T> valor, String mensaje) {
        return valor.orElseThrow(() -> new ReglaNegocioException(mensaje));
    }

    /**
     * Regla de las opciones "Otra, ¿cuál?": si se eligió OTRA el texto es
     * obligatorio; si no, debe quedar vacío.
     */
    public static void otraCual(boolean esOtra, String cual, String campo) {
        if (esOtra) {
            requerido(cual, campo + " (¿cuál?)");
        } else {
            vacio(cual, campo + " (¿cuál?)", "si no se eligió la opción Otra");
        }
    }

    public static void condicion(boolean cumple, String mensaje) {
        if (!cumple) {
            throw new ReglaNegocioException(mensaje);
        }
    }
}
