package com.udi.model.enums;

/**
 * Decisión del crítico-digitador registrada en el historial de una encuesta
 * (tabla auditoria_encuesta, RF-10). Usa los mismos nombres que los estados
 * {@link EstadoEncuesta#APROBADA} y {@link EstadoEncuesta#RECHAZADA}.
 */
public enum AccionAuditoria {
    APROBADA("Aprobó el caso"),
    RECHAZADA("Rechazó el caso");

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
