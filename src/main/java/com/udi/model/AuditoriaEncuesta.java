package com.udi.model;

import com.udi.model.enums.AccionAuditoria;

/** Registro del historial de una encuesta: quién hizo qué y cuándo (RF-10). */
public class AuditoriaEncuesta extends EntidadAuditable {

    private Long encuestaId;
    private AccionAuditoria accion;
    private String observacion;
    /** Nombre del usuario que hizo la acción (created_by). No es una columna. */
    private String usuarioNombre;

    public AuditoriaEncuesta() {
    }

    public AuditoriaEncuesta(Long encuestaId, AccionAuditoria accion, String observacion) {
        this.encuestaId = encuestaId;
        this.accion = accion;
        this.observacion = observacion;
    }

    public Long getEncuestaId() {
        return encuestaId;
    }

    public void setEncuestaId(Long encuestaId) {
        this.encuestaId = encuestaId;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public void setAccion(AccionAuditoria accion) {
        this.accion = accion;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }
}
