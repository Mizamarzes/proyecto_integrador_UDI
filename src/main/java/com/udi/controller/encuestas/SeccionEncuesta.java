package com.udi.controller.encuestas;

/**
 * Pestaña del detalle de encuesta (visitas, capítulo A, capítulo C, gastos,
 * cierre y correcciones). El detalle la carga y le pasa la encuesta con su hogar y vivienda.
 */
public interface SeccionEncuesta {

    void mostrar(EncuestaDetalleController detalle);
}
