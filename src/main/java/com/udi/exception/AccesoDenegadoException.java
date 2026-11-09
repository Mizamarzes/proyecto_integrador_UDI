package com.udi.exception;

/**
 * El usuario no tiene sesión o su rol no le permite abrir una vista o hacer
 * una operación. Es una regla de negocio más, con su propio título en las alertas.
 */
public class AccesoDenegadoException extends ReglaNegocioException {

    private static final long serialVersionUID = 1L;

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
