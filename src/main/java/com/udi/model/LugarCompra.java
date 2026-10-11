package com.udi.model;

/** Lugar de compra. El id es el código del cuadernillo (1, 4, 6, 7, 10...). */
public class LugarCompra extends EntidadAuditable {

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
