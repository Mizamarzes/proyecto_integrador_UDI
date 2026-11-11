package com.udi.model.enums;

/**
 * Estado del caso (RF-06, RF-10).
 *
 * ABIERTA → CERRADA (el encuestador cierra el día 14) → APROBADA (el
 * crítico-digitador la verifica y exporta). Desde CERRADA el crítico puede
 * devolverla a EN_CORRECCION, y al cerrarla de nuevo vuelve a CERRADA. Un
 * administrador puede reabrir un caso cerrado (REABIERTA).
 */
public enum EstadoEncuesta {
    ABIERTA("Abierta"),
    CERRADA("Cerrada"),
    REABIERTA("Reabierta"),
    EN_CORRECCION("En corrección"),
    APROBADA("Aprobada");

    private final String descripcion;

    EstadoEncuesta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Una encuesta cerrada o aprobada no admite registros ni modificaciones. */
    public boolean admiteCambios() {
        return this != CERRADA && this != APROBADA;
    }

    /** Estados que aparecen en la bandeja de verificación. */
    public boolean enVerificacion() {
        return this == CERRADA || this == EN_CORRECCION || this == APROBADA;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
