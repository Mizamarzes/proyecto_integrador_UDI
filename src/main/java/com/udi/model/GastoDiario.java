package com.udi.model;

import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.model.enums.TipoGasto;
import com.udi.model.enums.UnidadMedida;

import java.math.BigDecimal;

/** Renglón de la tabla D. Gastos diarios (RF-03, RF-04). */
public class GastoDiario extends EntidadAuditable {

    private Long registroDiarioId;
    private Long encuestadorId;
    /** (7) Nulo si no fue compra */
    private Long lugarCompraId;
    /** (3) */
    private Integer numeroOrden;
    /** (4) */
    private String nombreArticulo;
    /** (5.1) */
    private BigDecimal cantidad;
    /** (5.2) */
    private UnidadMedida unidadMedida;
    /** (5.2) Cuál, si la unidad es OTRA */
    private String unidadMedidaOtra;
    /** (5.3) Solo unidad OTRA */
    private BigDecimal equivalenciaGr;
    /** (5.3) Solo unidad OTRA */
    private BigDecimal equivalenciaCm3;
    /** (6) */
    private FormaAdquisicion formaAdquisicion;
    /** (6) Cuál, si la forma es OTRA */
    private String formaAdquisicionOtra;
    /** (8) Pagado o estimado */
    private BigDecimal valor;
    /** (9) Nulo si no fue compra */
    private FrecuenciaCompra frecuenciaCompra;
    /** (10) */
    private TipoGasto tipoGasto;
    /** Persona a quien se entregó la mesada */
    private String beneficiarioMesada;

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

    public String getNombreArticulo() {
        return nombreArticulo;
    }

    public void setNombreArticulo(String nombreArticulo) {
        this.nombreArticulo = nombreArticulo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public UnidadMedida getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public String getUnidadMedidaOtra() {
        return unidadMedidaOtra;
    }

    public void setUnidadMedidaOtra(String unidadMedidaOtra) {
        this.unidadMedidaOtra = unidadMedidaOtra;
    }

    public BigDecimal getEquivalenciaGr() {
        return equivalenciaGr;
    }

    public void setEquivalenciaGr(BigDecimal equivalenciaGr) {
        this.equivalenciaGr = equivalenciaGr;
    }

    public BigDecimal getEquivalenciaCm3() {
        return equivalenciaCm3;
    }

    public void setEquivalenciaCm3(BigDecimal equivalenciaCm3) {
        this.equivalenciaCm3 = equivalenciaCm3;
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

    public String getBeneficiarioMesada() {
        return beneficiarioMesada;
    }

    public void setBeneficiarioMesada(String beneficiarioMesada) {
        this.beneficiarioMesada = beneficiarioMesada;
    }
}
