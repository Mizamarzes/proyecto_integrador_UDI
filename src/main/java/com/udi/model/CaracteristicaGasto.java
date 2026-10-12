package com.udi.model;

import com.udi.model.enums.FrecuenciaCompra;

import java.math.BigDecimal;

public class CaracteristicaGasto extends EntidadAuditable {

    private Long encuestaId;
    private Long grupoAlimentoId;
    private Long encuestadorId;
    private FrecuenciaCompra frecuenciaCompra;
    private BigDecimal valorUltimaCompra;
    private Boolean compraraEnPeriodo;

    public Long getEncuestaId() {
        return encuestaId;
    }

    public void setEncuestaId(Long encuestaId) {
        this.encuestaId = encuestaId;
    }

    public Long getGrupoAlimentoId() {
        return grupoAlimentoId;
    }

    public void setGrupoAlimentoId(Long grupoAlimentoId) {
        this.grupoAlimentoId = grupoAlimentoId;
    }

    public Long getEncuestadorId() {
        return encuestadorId;
    }

    public void setEncuestadorId(Long encuestadorId) {
        this.encuestadorId = encuestadorId;
    }

    public FrecuenciaCompra getFrecuenciaCompra() {
        return frecuenciaCompra;
    }

    public void setFrecuenciaCompra(FrecuenciaCompra frecuenciaCompra) {
        this.frecuenciaCompra = frecuenciaCompra;
    }

    public BigDecimal getValorUltimaCompra() {
        return valorUltimaCompra;
    }

    public void setValorUltimaCompra(BigDecimal valorUltimaCompra) {
        this.valorUltimaCompra = valorUltimaCompra;
    }

    public Boolean getCompraraEnPeriodo() {
        return compraraEnPeriodo;
    }

    public void setCompraraEnPeriodo(Boolean compraraEnPeriodo) {
        this.compraraEnPeriodo = compraraEnPeriodo;
    }
}
