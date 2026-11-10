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

    /**
     * Roles que el código reconoce por su nombre: ADMIN (acceso total) y
     * ENCUESTADOR ("Mis Encuestas", registro en la tabla encuestador y creación
     * de encuestas). No se pueden eliminar; los permisos del ENCUESTADOR sí se editan.
     */
    public boolean esDelSistema() {
        return ADMIN.equals(nombre) || ENCUESTADOR.equals(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
