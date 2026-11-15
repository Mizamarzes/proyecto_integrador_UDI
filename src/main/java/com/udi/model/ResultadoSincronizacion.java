package com.udi.model;

/**
 * Resultado de {@code pr_sincronizar_aprobadas}: cuántas encuestas aprobadas
 * se copiaron por primera vez al esquema destino y cuántas se reemplazaron
 * porque se volvieron a aprobar después de la última sincronización.
 */
public class ResultadoSincronizacion {

    private final int nuevas;
    private final int actualizadas;

    public ResultadoSincronizacion(int nuevas, int actualizadas) {
        this.nuevas = nuevas;
        this.actualizadas = actualizadas;
    }

    public int getNuevas() {
        return nuevas;
    }

    public int getActualizadas() {
        return actualizadas;
    }

    /** true si el destino ya tenía todas las aprobadas al día. */
    public boolean sinCambios() {
        return nuevas == 0 && actualizadas == 0;
    }
}
