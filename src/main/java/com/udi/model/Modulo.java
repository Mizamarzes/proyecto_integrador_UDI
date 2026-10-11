package com.udi.model;

/** Sección de la aplicación a la que se da acceso por rol. */
public class Modulo extends EntidadAuditable {

    private String nombre;
    /** Identificador para el menú: usuarios, encuestas... */
    private String slug;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
