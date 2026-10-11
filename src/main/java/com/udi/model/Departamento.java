package com.udi.model;

/** Departamento (A.2). */
public class Departamento extends Entidad {

    private String nombre;
    private Long regionId;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
