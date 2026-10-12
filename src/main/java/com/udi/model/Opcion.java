package com.udi.model;

import java.util.Objects;

/** Elemento de una lista desplegable: el id de un registro y el texto que se muestra. */
public class Opcion {

    private final long id;
    private final String texto;

    public Opcion(long id, String texto) {
        this.id = id;
        this.texto = texto;
    }

    public long getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Opcion && ((Opcion) o).id == id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return texto;
    }
}
