package com.udi.model;

/** Rol de usuario: ADMIN, ENCUESTADOR o CRITICO_DIGITADOR. */
public class Rol extends EntidadAuditable {

    public static final String ADMIN = "ADMIN";
    public static final String ENCUESTADOR = "ENCUESTADOR";
    public static final String CRITICO_DIGITADOR = "CRITICO_DIGITADOR";

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
