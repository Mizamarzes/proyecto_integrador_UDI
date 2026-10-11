package com.udi.model.enums;

/** Estado del caso (RF-06). */
public enum EstadoEncuesta {
    ABIERTA("Abierta"),
    CERRADA("Cerrada"),
    REABIERTA("Reabierta");

    private final String descripcion;

    EstadoEncuesta(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Una encuesta cerrada no admite registros ni modificaciones. */
    public boolean admiteCambios() {
        return this != CERRADA;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
