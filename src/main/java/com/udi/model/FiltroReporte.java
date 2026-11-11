package com.udi.model;

import java.time.LocalDate;

/**
 * Filtros del módulo de reportes. Un valor null significa «sin límite» o
 * «todos». Las fechas se comparan con el inicio de la encuesta (A.21 fecha
 * desde) y ambas son inclusivas.
 *
 * {@code encuestadorId} no es un filtro de la pantalla: lo pone el servicio
 * cuando quien consulta es un encuestador, para que solo vea lo suyo.
 */
public class FiltroReporte {

    private LocalDate desde;
    private LocalDate hasta;
    private Long regionId;
    private Long departamentoId;
    private Long municipioId;
    private Long encuestadorId;

    public FiltroReporte() {
    }

    public FiltroReporte(LocalDate desde, LocalDate hasta, Long regionId, Long departamentoId, Long municipioId) {
        this.desde = desde;
        this.hasta = hasta;
        this.regionId = regionId;
        this.departamentoId = departamentoId;
        this.municipioId = municipioId;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public Long getDepartamentoId() {
        return departamentoId;
    }

    public void setDepartamentoId(Long departamentoId) {
        this.departamentoId = departamentoId;
    }

    public Long getMunicipioId() {
        return municipioId;
    }

    public void setMunicipioId(Long municipioId) {
        this.municipioId = municipioId;
    }

    public Long getEncuestadorId() {
        return encuestadorId;
    }

    public void setEncuestadorId(Long encuestadorId) {
        this.encuestadorId = encuestadorId;
    }
}
