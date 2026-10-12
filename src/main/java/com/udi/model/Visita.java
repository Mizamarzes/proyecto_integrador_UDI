package com.udi.model;

import com.udi.model.enums.ResultadoEntrevista;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Visita al hogar (capítulo B, RF-08). */
public class Visita extends EntidadAuditable {

    private Long hogarId;
    private Integer numeroVisita;
    private LocalDate fecha;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private ResultadoEntrevista resultadoVisita;
    private String partesPendientes;
    private LocalDate fechaProxima;
    private LocalDateTime horaProxima;

    public Long getHogarId() {
        return hogarId;
    }

    public void setHogarId(Long hogarId) {
        this.hogarId = hogarId;
    }

    public Integer getNumeroVisita() {
        return numeroVisita;
    }

    public void setNumeroVisita(Integer numeroVisita) {
        this.numeroVisita = numeroVisita;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalDateTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalDateTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalDateTime horaFin) {
        this.horaFin = horaFin;
    }

    public ResultadoEntrevista getResultadoVisita() {
        return resultadoVisita;
    }

    public void setResultadoVisita(ResultadoEntrevista resultadoVisita) {
        this.resultadoVisita = resultadoVisita;
    }

    public String getPartesPendientes() {
        return partesPendientes;
    }

    public void setPartesPendientes(String partesPendientes) {
        this.partesPendientes = partesPendientes;
    }

    public LocalDate getFechaProxima() {
        return fechaProxima;
    }

    public void setFechaProxima(LocalDate fechaProxima) {
        this.fechaProxima = fechaProxima;
    }

    public LocalDateTime getHoraProxima() {
        return horaProxima;
    }

    public void setHoraProxima(LocalDateTime horaProxima) {
        this.horaProxima = horaProxima;
    }
}
