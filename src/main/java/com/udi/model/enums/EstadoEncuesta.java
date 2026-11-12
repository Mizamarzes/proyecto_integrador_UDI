package com.udi.model.enums;

/**
 * Estado del caso (RF-06, RF-10).
 *
 * EN_CURSO → PENDIENTE_APROBACION (el encuestador cierra el día 14) →
 * APROBADA (el crítico-digitador la verifica y exporta). Desde
 * PENDIENTE_APROBACION el crítico puede marcarla RECHAZADA, con una
 * observación; el encuestador la corrige y al cerrarla de nuevo vuelve a
 * PENDIENTE_APROBACION. Un administrador puede devolver a EN_CURSO un caso
 * pendiente o aprobado.
 */
public enum EstadoEncuesta {
    EN_CURSO("En curso"),
    PENDIENTE_APROBACION("Pendiente de aprobación"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada");

    private final String descripcion;

    EstadoEncuesta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Una encuesta pendiente de aprobación o aprobada no admite registros ni modificaciones. */
    public boolean admiteCambios() {
        return this == EN_CURSO || this == RECHAZADA;
    }

    /** Estados que aparecen en la bandeja de verificación. */
    public boolean enVerificacion() {
        return this != EN_CURSO;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
