package com.udi.model.enums;

/** Tipo de comida preparada fuera del hogar (capítulo D1, columna 4.1). */
public enum TipoComida {
    DESAYUNO("Desayuno"),
    ALMUERZO("Almuerzo"),
    CENA("Cena"),
    ONCES("Onces, medias nueves, merienda, snacks");

    private final String descripcion;

    TipoComida(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
