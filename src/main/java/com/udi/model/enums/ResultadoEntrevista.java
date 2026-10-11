package com.udi.model.enums;

/** Resultado del cuadernillo (A.18) y de cada visita (capítulo B). */
public enum ResultadoEntrevista {
    EC("Encuesta completa"),
    EI("Encuesta incompleta"),
    NH("Nadie en el hogar"),
    AT("Ausente temporal"),
    R("Rechazo"),
    OC("Ocupado"),
    V("Vacante"),
    O("Otro motivo");

    private final String descripcion;

    ResultadoEntrevista(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return name() + " - " + descripcion;
    }
}
