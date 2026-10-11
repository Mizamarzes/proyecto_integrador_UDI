package com.udi.exception;

/**
 * Error técnico al acceder a la base de datos (conexión caída, restricción
 * violada, SQL inválido...). Envuelve la {@link java.sql.SQLException} original.
 */
public class DatosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
