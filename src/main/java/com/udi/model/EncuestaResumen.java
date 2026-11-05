package com.udi.model;

/**
 * Fila del listado de encuestas: la encuesta con el nombre de su encuestador
 * y la dirección de la vivienda del hogar. No es una tabla.
 */
public class EncuestaResumen {

    private final Encuesta encuesta;
    private final String encuestador;
    private final String direccion;

    public EncuestaResumen(Encuesta encuesta, String encuestador, String direccion) {
        this.encuesta = encuesta;
        this.encuestador = encuestador;
        this.direccion = direccion;
    }

    public String getDireccion() {
        return direccion;
    }

    public Encuesta getEncuesta() {
        return encuesta;
    }

    public String getEncuestador() {
        return encuestador;
    }
}
