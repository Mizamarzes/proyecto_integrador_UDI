package com.udi.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Resultado del módulo de reportes para unos filtros: el rendimiento por
 * encuestador (de donde salen los indicadores y el estado de las encuestas) y
 * los totales del gasto registrado en los capítulos C, D y D1.
 */
public class Reporte {

    private final List<ReporteEncuestador> encuestadores;
    /** Capítulo C: hogares que compran cada grupo (frecuencia distinta de «Nunca»), de mayor a menor. */
    private final List<ValorReporte> grupos;
    /** Gasto por forma de adquisición, en el orden de sus códigos 1 a 7. */
    private final List<ValorReporte> formas;
    /** Gasto de lo comprado por lugar de compra, de mayor a menor. */
    private final List<ValorReporte> lugares;
    /** Gasto del hogar y gasto personal. */
    private final List<ValorReporte> tipos;
    private final long articulos;
    private final long comidas;
    private final long domicilios;

    public Reporte(List<ReporteEncuestador> encuestadores, List<ValorReporte> grupos, List<ValorReporte> formas,
                   List<ValorReporte> lugares, List<ValorReporte> tipos, long articulos, long comidas, long domicilios) {
        this.encuestadores = encuestadores;
        this.grupos = grupos;
        this.formas = formas;
        this.lugares = lugares;
        this.tipos = tipos;
        this.articulos = articulos;
        this.comidas = comidas;
        this.domicilios = domicilios;
    }

    public List<ReporteEncuestador> getEncuestadores() {
        return encuestadores;
    }

    public List<ValorReporte> getGrupos() {
        return grupos;
    }

    public List<ValorReporte> getFormas() {
        return formas;
    }

    public List<ValorReporte> getLugares() {
        return lugares;
    }

    public List<ValorReporte> getTipos() {
        return tipos;
    }

    /** Renglones del capítulo D (gastos diarios). */
    public long getArticulos() {
        return articulos;
    }

    /** Renglones del capítulo D1 (comidas preparadas fuera del hogar). */
    public long getComidas() {
        return comidas;
    }

    /** Comidas fuera del hogar recibidas a domicilio. */
    public long getDomicilios() {
        return domicilios;
    }

    public boolean estaVacio() {
        return getTotal() == 0;
    }

    public long getTotal() {
        return sumar(ReporteEncuestador::getTotal);
    }

    public long getAbiertas() {
        return sumar(ReporteEncuestador::getAbiertas);
    }

    public long getCerradas() {
        return sumar(ReporteEncuestador::getCerradas);
    }

    public long getReabiertas() {
        return sumar(ReporteEncuestador::getReabiertas);
    }

    public long getVisitas() {
        return sumar(ReporteEncuestador::getVisitas);
    }

    public BigDecimal getGasto() {
        return encuestadores.stream().map(ReporteEncuestador::getGasto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long sumar(ToLongFunction<ReporteEncuestador> campo) {
        return encuestadores.stream().mapToLong(campo).sum();
    }
}
