package com.udi.model;

import java.time.LocalDate;

/** Encabezado de la página de un día del capítulo D (1 a 14). */
public class RegistroDiario extends EntidadAuditable {

    private Long encuestaId;
    /** 1 a 14 */
    private Integer numeroDia;
    /** D.1.2 = fechaDesde de la encuesta + numeroDia - 1 */
    private LocalDate fecha;
    private String observaciones;

    public Long getEncuestaId() {
        return encuestaId;
    }

    public void setEncuestaId(Long encuestaId) {
        this.encuestaId = encuestaId;
    }

    public Integer getNumeroDia() {
        return numeroDia;
    }

    public void setNumeroDia(Integer numeroDia) {
        this.numeroDia = numeroDia;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public String toString() {
        return "Día " + numeroDia;
    }
}
