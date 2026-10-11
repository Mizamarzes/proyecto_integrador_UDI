package com.udi.model;

/** Personas que comparten la vivienda y un presupuesto común para la comida. */
public class Hogar extends EntidadAuditable {

    private Long viviendaId;
    /** A.13 Hogar número dentro de la vivienda */
    private Integer hogarNumero;
    /** A.14 Total de personas en el hogar */
    private Integer totalPersonas;
    /** A.9 */
    private String ciclo;

    public Long getViviendaId() {
        return viviendaId;
    }

    public void setViviendaId(Long viviendaId) {
        this.viviendaId = viviendaId;
    }

    public Integer getHogarNumero() {
        return hogarNumero;
    }

    public void setHogarNumero(Integer hogarNumero) {
        this.hogarNumero = hogarNumero;
    }

    public Integer getTotalPersonas() {
        return totalPersonas;
    }

    public void setTotalPersonas(Integer totalPersonas) {
        this.totalPersonas = totalPersonas;
    }

    public String getCiclo() {
        return ciclo;
    }

    public void setCiclo(String ciclo) {
        this.ciclo = ciclo;
    }

    @Override
    public String toString() {
        return "Hogar " + hogarNumero;
    }
}
