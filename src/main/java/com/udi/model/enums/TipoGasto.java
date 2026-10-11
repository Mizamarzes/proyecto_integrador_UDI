package com.udi.model.enums;

/** Gasto del hogar (1) o gasto personal (2) (capítulos D y D1, columna 10). */
public enum TipoGasto {
    HOGAR(1, "Gasto del hogar"),
    PERSONAL(2, "Gasto personal");

    private final int codigo;
    private final String descripcion;

    TipoGasto(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return codigo + ". " + descripcion;
    }
}
