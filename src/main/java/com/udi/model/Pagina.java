package com.udi.model;

import java.util.List;

/**
 * Una página de resultados de una búsqueda paginada: los registros de la
 * página y si quedan más después de ella (para el botón «Ver más»).
 */
public class Pagina<T> {

    private final List<T> registros;
    private final boolean hayMas;

    public Pagina(List<T> registros, boolean hayMas) {
        this.registros = registros;
        this.hayMas = hayMas;
    }

    public List<T> getRegistros() {
        return registros;
    }

    public boolean isHayMas() {
        return hayMas;
    }
}
