package com.udi.model.enums;

/** Acción registrada en el historial de una encuesta (tabla auditoria_encuesta, RF-10). */
public enum AccionAuditoria {
    CIERRE("Cerró el caso"),
    REAPERTURA("Reabrió el caso"),
    DEVOLUCION("Devolvió el caso a corrección"),
    APROBACION("Aprobó el caso"),
    EXPORTACION("Exportó el caso a Excel");

    private final String descripcion;

    AccionAuditoria(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
