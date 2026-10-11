package com.udi.model;

/** Municipio (A.3). */
public class Municipio extends Entidad {

    private String nombre;
    private Long departamentoId;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getDepartamentoId() {
        return departamentoId;
    }

    public void setDepartamentoId(Long departamentoId) {
        this.departamentoId = departamentoId;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
