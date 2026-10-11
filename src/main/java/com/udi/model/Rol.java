package com.udi.model;

/** Rol de usuario: ADMIN, SUPERVISOR, ENCUESTADOR... */
public class Rol extends EntidadAuditable {

    public static final String ADMIN = "ADMIN";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String ENCUESTADOR = "ENCUESTADOR";

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
