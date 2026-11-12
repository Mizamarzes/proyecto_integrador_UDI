package com.udi.model;

import java.math.BigDecimal;

/** Fila de «Rendimiento por encuestador»: sus encuestas por estado, visitas y gasto registrado. */
public class ReporteEncuestador {

    private final Long encuestadorId;
    private final String nombre;
    private final long total;
    private final long enCurso;
    private final long pendientes;
    private final long aprobadas;
    private final long rechazadas;
    private final long visitas;
    private final BigDecimal gasto;

    public ReporteEncuestador(Long encuestadorId, String nombre, long total, long enCurso, long pendientes,
                              long aprobadas, long rechazadas, long visitas, BigDecimal gasto) {
        this.encuestadorId = encuestadorId;
        this.nombre = nombre;
        this.total = total;
        this.enCurso = enCurso;
        this.pendientes = pendientes;
        this.aprobadas = aprobadas;
        this.rechazadas = rechazadas;
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

    public long getEnCurso() {
        return enCurso;
    }

    public long getPendientes() {
        return pendientes;
    }

    public long getAprobadas() {
        return aprobadas;
    }

    public long getRechazadas() {
        return rechazadas;
    }

    public long getVisitas() {
        return visitas;
    }

    public BigDecimal getGasto() {
        return gasto;
    }
}
