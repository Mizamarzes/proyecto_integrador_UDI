package com.udi.model;

import com.udi.model.enums.EstadoEncuesta;
import com.udi.model.enums.FormaPago;
import com.udi.model.enums.ResultadoEntrevista;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cuadernillo aplicado a un hogar durante 14 días (capítulos A y D2). */
public class Encuesta extends EntidadAuditable {

    private Long hogarId;
    private Long encuestadorId;
    private String informanteNombre;
    private Integer informanteOrden;
    private ResultadoEntrevista resultadoCuadernillo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Boolean gastoFiestas;
    private FormaPago formaPago;
    private String formaPagoOtra;
    private String observaciones;
    private EstadoEncuesta estado;
    private LocalDateTime fechaCierre;
    private Long cerradaPor;

    public Long getHogarId() {
        return hogarId;
    }

    public void setHogarId(Long hogarId) {
        this.hogarId = hogarId;
    }

    public Long getEncuestadorId() {
        return encuestadorId;
    }

    public void setEncuestadorId(Long encuestadorId) {
        this.encuestadorId = encuestadorId;
    }

    public String getInformanteNombre() {
        return informanteNombre;
    }

    public void setInformanteNombre(String informanteNombre) {
        this.informanteNombre = informanteNombre;
    }

    public Integer getInformanteOrden() {
        return informanteOrden;
    }

    public void setInformanteOrden(Integer informanteOrden) {
        this.informanteOrden = informanteOrden;
    }

    public ResultadoEntrevista getResultadoCuadernillo() {
        return resultadoCuadernillo;
    }

    public void setResultadoCuadernillo(ResultadoEntrevista resultadoCuadernillo) {
        this.resultadoCuadernillo = resultadoCuadernillo;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Boolean getGastoFiestas() {
        return gastoFiestas;
    }

    public void setGastoFiestas(Boolean gastoFiestas) {
        this.gastoFiestas = gastoFiestas;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(FormaPago formaPago) {
        this.formaPago = formaPago;
    }

    public String getFormaPagoOtra() {
        return formaPagoOtra;
    }

    public void setFormaPagoOtra(String formaPagoOtra) {
        this.formaPagoOtra = formaPagoOtra;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public EstadoEncuesta getEstado() {
        return estado;
    }

    public void setEstado(EstadoEncuesta estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public Long getCerradaPor() {
        return cerradaPor;
    }

    public void setCerradaPor(Long cerradaPor) {
        this.cerradaPor = cerradaPor;
    }

    /** true si el caso está cerrado y no admite cambios (RF-06). */
    public boolean estaCerrada() {
        return estado != null && !estado.admiteCambios();
    }
}
