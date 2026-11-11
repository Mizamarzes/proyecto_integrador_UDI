package com.udi.model;

import java.math.BigDecimal;

/** Fila de «Rendimiento por encuestador»: sus encuestas por estado, visitas y gasto registrado. */
public class ReporteEncuestador {

    private final Long encuestadorId;
    private final String nombre;
    private final long total;
    private final long abiertas;
    private final long cerradas;
    private final long reabiertas;
    private final long visitas;
    private final BigDecimal gasto;

    public ReporteEncuestador(Long encuestadorId, String nombre, long total, long abiertas, long cerradas,
                              long reabiertas, long visitas, BigDecimal gasto) {
        this.encuestadorId = encuestadorId;
        this.nombre = nombre;
        this.total = total;
        this.abiertas = abiertas;
        this.cerradas = cerradas;
        this.reabiertas = reabiertas;
        this.visitas = visitas;
        this.gasto = gasto == null ? BigDecimal.ZERO : gasto;
    }

    public Long getEncuestadorId() {
        return encuestadorId;
    }

    public String getNombre() {
        return nombre;
    }

    public long getTotal() {
        return total;
    }

    public long getAbiertas() {
        return abiertas;
    }

    public long getCerradas() {
        return cerradas;
    }

    public long getReabiertas() {
        return reabiertas;
    }

    public long getVisitas() {
        return visitas;
    }

    public BigDecimal getGasto() {
        return gasto;
    }
}
