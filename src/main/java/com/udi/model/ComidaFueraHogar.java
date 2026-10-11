package com.udi.model;

import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.model.enums.TipoComida;
import com.udi.model.enums.TipoGasto;

import java.math.BigDecimal;

/** Renglón de la tabla D1. Comidas preparadas fuera del hogar (RF-09). */
public class ComidaFueraHogar extends EntidadAuditable {

    private Long registroDiarioId;
    private Long encuestadorId;
    /** (7) Nulo si no fue compra */
    private Long lugarCompraId;
    /** (3) */
    private Integer numeroOrden;
    /** (4) */
    private String descripcion;
    /** (4.1) */
    private TipoComida tipoComida;
    /** (5) */
    private BigDecimal cantidad;
    /** (6) */
    private FormaAdquisicion formaAdquisicion;
    /** (6) Cuál, si la forma es OTRA */
    private String formaAdquisicionOtra;
    /** (8) */
    private BigDecimal valor;
    /** (9) Nulo si no fue compra */
    private FrecuenciaCompra frecuenciaCompra;
    /** (10) */
    private TipoGasto tipoGasto;
    /** (11) */
    private boolean aDomicilio;

    public Long getRegistroDiarioId() {
        return registroDiarioId;
    }

    public void setRegistroDiarioId(Long registroDiarioId) {
        this.registroDiarioId = registroDiarioId;
    }

    public Long getEncuestadorId() {
        return encuestadorId;
    }

    public void setEncuestadorId(Long encuestadorId) {
        this.encuestadorId = encuestadorId;
    }

    public Long getLugarCompraId() {
        return lugarCompraId;
    }

    public void setLugarCompraId(Long lugarCompraId) {
        this.lugarCompraId = lugarCompraId;
    }

    public Integer getNumeroOrden() {
        return numeroOrden;
    }

    public void setNumeroOrden(Integer numeroOrden) {
        this.numeroOrden = numeroOrden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoComida getTipoComida() {
        return tipoComida;
    }

    public void setTipoComida(TipoComida tipoComida) {
        this.tipoComida = tipoComida;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public FormaAdquisicion getFormaAdquisicion() {
        return formaAdquisicion;
    }

    public void setFormaAdquisicion(FormaAdquisicion formaAdquisicion) {
        this.formaAdquisicion = formaAdquisicion;
    }

    public String getFormaAdquisicionOtra() {
        return formaAdquisicionOtra;
    }

    public void setFormaAdquisicionOtra(String formaAdquisicionOtra) {
        this.formaAdquisicionOtra = formaAdquisicionOtra;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FrecuenciaCompra getFrecuenciaCompra() {
        return frecuenciaCompra;
    }

    public void setFrecuenciaCompra(FrecuenciaCompra frecuenciaCompra) {
        this.frecuenciaCompra = frecuenciaCompra;
    }

    public TipoGasto getTipoGasto() {
        return tipoGasto;
    }

    public void setTipoGasto(TipoGasto tipoGasto) {
        this.tipoGasto = tipoGasto;
    }

    public boolean isADomicilio() {
        return aDomicilio;
    }

    public void setADomicilio(boolean aDomicilio) {
        this.aDomicilio = aDomicilio;
    }
}
