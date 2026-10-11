package com.udi.model.enums;

/** D2.2 Principal forma de pago. */
public enum FormaPago {
    TARJETA_DEBITO("Tarjeta débito"),
    TARJETA_CREDITO("Tarjeta de crédito"),
    EFECTIVO("Efectivo"),
    FIADO("Fiado"),
    BONOS("Bonos"),
    OTRA("Otra");

    private final String descripcion;

    FormaPago(String descripcion) {
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
