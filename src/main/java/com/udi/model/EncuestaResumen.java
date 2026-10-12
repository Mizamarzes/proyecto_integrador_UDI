package com.udi.model;

/** Fila del listado de encuestas: la encuesta con el nombre de su encuestador. No es una tabla. */
public class EncuestaResumen {

    private final Encuesta encuesta;
    private final String encuestador;

    public EncuestaResumen(Encuesta encuesta, String encuestador) {
        this.encuesta = encuesta;
        this.encuestador = encuestador;
    }

    public Encuesta getEncuesta() {
        return encuesta;
    }

    public String getEncuestador() {
        return encuestador;
    }
}
