package com.udi.model;

/** Usuario que hace trabajo de campo (especialización de Usuario). */
public class Encuestador extends EntidadAuditable {

    private Long usuarioId;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}
