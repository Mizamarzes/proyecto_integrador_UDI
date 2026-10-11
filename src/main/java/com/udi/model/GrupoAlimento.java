package com.udi.model;

/** Grupo de alimentos del capítulo C. El id es el código del cuadernillo (1 a 25). */
public class GrupoAlimento extends EntidadAuditable {

    private String nombre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
