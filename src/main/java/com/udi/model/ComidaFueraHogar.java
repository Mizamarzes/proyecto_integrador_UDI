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
    private Long lugarCompraId;
    private Integer numeroOrden;
    private String descripcion;
    private TipoComida tipoComida;
    private BigDecimal cantidad;
    private FormaAdquisicion formaAdquisicion;
    private String formaAdquisicionOtra;
    private BigDecimal valor;
    private FrecuenciaCompra frecuenciaCompra;
    private TipoGasto tipoGasto;
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
