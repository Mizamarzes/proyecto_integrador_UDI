package com.udi.model;

/**
 * Fila del listado de encuestas: la encuesta con el nombre de su encuestador
 * y la vivienda de su hogar (dirección y ubicación). No es una tabla.
 */
public class EncuestaResumen {

    private final Encuesta encuesta;
    private final String encuestador;
    private final Vivienda vivienda;

    /** {@code vivienda} puede ser null si no se encontró la del hogar. */
    public EncuestaResumen(Encuesta encuesta, String encuestador, Vivienda vivienda) {
        this.encuesta = encuesta;
        this.encuestador = encuestador;
        this.vivienda = vivienda;
    }

    public Encuesta getEncuesta() {
        return encuesta;
    }

    public String getEncuestador() {
        return encuestador;
    }

    public Vivienda getVivienda() {
        return vivienda;
    }

    public String getDireccion() {
        return texto(vivienda == null ? null : vivienda.getDireccion());
    }

    public String getRegion() {
        return texto(vivienda == null ? null : vivienda.getRegionNombre());
    }

    public String getDepartamento() {
        return texto(vivienda == null ? null : vivienda.getDepartamentoNombre());
    }

    public String getMunicipio() {
        return texto(vivienda == null ? null : vivienda.getMunicipioNombre());
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
