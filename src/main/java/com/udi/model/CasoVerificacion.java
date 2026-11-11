package com.udi.model;

import java.util.Collections;
import java.util.List;

/**
 * Fila de la bandeja de verificación: la encuesta (con encuestador y
 * vivienda) y el resultado de su validación cruzada. No es una tabla.
 */
public class CasoVerificacion {

    private final EncuestaResumen resumen;
    private final List<ReglaVerificacion> reglas;

    public CasoVerificacion(EncuestaResumen resumen, List<ReglaVerificacion> reglas) {
        this.resumen = resumen;
        this.reglas = Collections.unmodifiableList(reglas);
    }

    public EncuestaResumen getResumen() {
        return resumen;
    }

    public Encuesta getEncuesta() {
        return resumen.getEncuesta();
    }

    public List<ReglaVerificacion> getReglas() {
        return reglas;
    }

    public long getErrores() {
        return contar(ReglaVerificacion.Nivel.ERROR);
    }

    public long getAvisos() {
        return contar(ReglaVerificacion.Nivel.AVISO);
    }

    public long getCorrectas() {
        return contar(ReglaVerificacion.Nivel.CORRECTA);
    }

    private long contar(ReglaVerificacion.Nivel nivel) {
        return reglas.stream().filter(r -> r.getNivel() == nivel).count();
    }
}
