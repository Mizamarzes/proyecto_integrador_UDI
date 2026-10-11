package com.udi.model.enums;

/** Forma de adquisición (capítulos D y D1, columna 6). */
public enum FormaAdquisicion {
    COMPRA(1, "Compra"),
    PAGO_TRABAJO(2, "Recibidos como pago por trabajo"),
    REGALO(3, "Regalo o donación"),
    INTERCAMBIO(4, "Intercambio o trueque"),
    PRODUCCION_PROPIA(5, "Traídos de la finca o producidos por el hogar"),
    NEGOCIO_PROPIO(6, "Tomados de un negocio del hogar"),
    OTRA(7, "Otra");

    private final int codigo;
    private final String descripcion;

    FormaAdquisicion(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Si no es compra, el lugar y la frecuencia de compra van vacíos y el valor es estimado. */
    public boolean esCompra() {
        return this == COMPRA;
    }

    @Override
    public String toString() {
        return codigo + ". " + descripcion;
    }
}
