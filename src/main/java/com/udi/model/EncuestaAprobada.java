package com.udi.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila del cursor de {@code pr_exportar_aprobadas}: una encuesta aprobada con
 * la ubicación de su vivienda, el encuestador, quién la aprobó, sus visitas,
 * los días registrados y el gasto total. Es la hoja «Encuestas aprobadas» del
 * Excel consolidado de Verificación.
 */
public class EncuestaAprobada {

    private Long encuestaId;
    private String codigo;
    private Long hogarId;
    private Integer hogarNumero;
    private Integer totalPersonas;
    private String region;
    private String departamento;
    private String municipio;
    private String barrio;
    private String direccion;
    private String encuestador;
    private String informante;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private LocalDateTime fechaCierre;
    private String aprobadaPor;
    private LocalDateTime fechaAprobacion;
    private long visitas;
    private long dias;
    private BigDecimal gastoTotal = BigDecimal.ZERO;

    public Long getEncuestaId() {
        return encuestaId;
    }

    public void setEncuestaId(Long encuestaId) {
        this.encuestaId = encuestaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Long getHogarId() {
        return hogarId;
    }

    public void setHogarId(Long hogarId) {
        this.hogarId = hogarId;
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEncuestador() {
        return encuestador;
    }

    public void setEncuestador(String encuestador) {
        this.encuestador = encuestador;
    }

    public String getInformante() {
        return informante;
    }

    public void setInformante(String informante) {
        this.informante = informante;
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

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getAprobadaPor() {
        return aprobadaPor;
    }

    public void setAprobadaPor(String aprobadaPor) {
        this.aprobadaPor = aprobadaPor;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public long getVisitas() {
        return visitas;
    }

    public void setVisitas(long visitas) {
        this.visitas = visitas;
    }

    public long getDias() {
        return dias;
    }

    public void setDias(long dias) {
        this.dias = dias;
    }

    public BigDecimal getGastoTotal() {
        return gastoTotal;
    }

    public void setGastoTotal(BigDecimal gastoTotal) {
        this.gastoTotal = gastoTotal == null ? BigDecimal.ZERO : gastoTotal;
    }
}
