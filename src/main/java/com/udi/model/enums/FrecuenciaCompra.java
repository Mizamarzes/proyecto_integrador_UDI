package com.udi.model.enums;

/** Frecuencia de compra (capítulo C, columna 2, y capítulos D y D1, columna 9). */
public enum FrecuenciaCompra {
    NUNCA("1", "Nunca", false),
    DIARIO("2", "Diario", false),
    VARIAS_VECES_SEMANA("2.1", "Varias veces por semana", false),
    SEMANAL("3", "Semanal", false),
    QUINCENAL("4", "Quincenal", false),
    MENSUAL("5", "Mensual", true),
    BIMESTRAL("6", "Bimestral", true),
    TRIMESTRAL("7", "Trimestral", true),
    ESPORADICA("9", "Esporádica", true);

    private final String codigo;
    private final String descripcion;
    private final boolean superaBisemana;

    FrecuenciaCompra(String codigo, String descripcion, boolean superaBisemana) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.superaBisemana = superaBisemana;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Si es true, en el capítulo C se preguntan C.3.1 y C.3.2. */
    public boolean superaBisemana() {
        return superaBisemana;
    }

    @Override
    public String toString() {
        return codigo + ". " + descripcion;
    }
}
