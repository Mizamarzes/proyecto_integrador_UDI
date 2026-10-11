package com.udi.exception;

/**
 * Se lanza cuando una operación incumple una regla del cuadernillo o de los
 * requisitos funcionales. El mensaje está pensado para mostrarse al usuario.
 */
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
