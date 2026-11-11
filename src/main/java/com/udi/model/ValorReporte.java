package com.udi.model;

import java.math.BigDecimal;

/**
 * Una barra de un reporte: el código y el nombre de la categoría (grupo de
 * alimento, forma de adquisición, lugar de compra…) y su valor, que según el
 * reporte es un monto en pesos o un conteo de hogares.
 */
public class ValorReporte {

    private final String codigo;
    private final String nombre;
    private final BigDecimal valor;

    public ValorReporte(String codigo, String nombre, BigDecimal valor) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.valor = valor == null ? BigDecimal.ZERO : valor;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
