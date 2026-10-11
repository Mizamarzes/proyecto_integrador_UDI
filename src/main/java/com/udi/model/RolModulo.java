package com.udi.model;

/** Permiso de un rol sobre un módulo (relación N:M TIENE PERMISO). */
public class RolModulo extends EntidadAuditable {

    private Long rolId;
    private Long moduloId;

    public Long getRolId() {
        return rolId;
    }

    public void setRolId(Long rolId) {
        this.rolId = rolId;
    }

    public Long getModuloId() {
        return moduloId;
    }

    public void setModuloId(Long moduloId) {
        this.moduloId = moduloId;
    }
}
