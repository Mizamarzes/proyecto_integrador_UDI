package com.udi.model;

/**
 * Resultado de una regla de la validación cruzada de un caso (RF-10). Los
 * errores impiden aprobarlo; los avisos solo piden revisar contra el
 * cuadernillo físico. No es una tabla.
 */
public class ReglaVerificacion {

    public enum Nivel {
        CORRECTA("Correcta"),
        AVISO("Aviso"),
        ERROR("Error");

        private final String descripcion;

        Nivel(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    private final String titulo;
    private final String detalle;
    private final Nivel nivel;

    public ReglaVerificacion(String titulo, String detalle, Nivel nivel) {
        this.titulo = titulo;
        this.detalle = detalle;
        this.nivel = nivel;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDetalle() {
        return detalle;
    }

    public Nivel getNivel() {
        return nivel;
    }
}
