package com.udi.model.enums;

/** Unidad de medida (capítulo D, columna 5.2). */
public enum UnidadMedida {
    CENTIMETROS_CUBICOS(1, "Centímetros cúbicos"),
    LITRO(2, "Litro"),
    GRAMOS(3, "Gramos"),
    ONZAS(4, "Onzas"),
    LIBRA(5, "Libra"),
    KILO(6, "Kilo"),
    ARROBA(7, "Arroba"),
    OTRA(8, "Otra");

    private final int codigo;
    private final String descripcion;

    UnidadMedida(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Las unidades tradicionales no llevan equivalencia en gramos o cm3 (5.3). */
    public boolean esTradicional() {
        return this != OTRA;
    }

    @Override
    public String toString() {
        return codigo + ". " + descripcion;
    }
}
