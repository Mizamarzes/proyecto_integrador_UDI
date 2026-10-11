package com.udi.model;

/** Espacio físico donde viven uno o más hogares (capítulo A). */
public class Vivienda extends EntidadAuditable {

    /** A.3 */
    private Long municipioId;
    /** A.4 (1 = cabecera) */
    private Integer clase;
    /** A.5 */
    private String sector;
    /** A.6 */
    private String seccion;
    /** A.7 */
    private String manzana;
    /** A.8 */
    private String segmento;
    /** A.10 */
    private Integer edificacionNumero;
    /** A.11 */
    private Integer viviendaNumero;
    /** A.12 Total de hogares en la vivienda */
    private Integer totalHogares;
    /** A.15 */
    private String barrio;
    /** A.16 */
    private String direccion;
    /** A.17 */
    private String telefono;

    public Long getMunicipioId() {
        return municipioId;
    }

    public void setMunicipioId(Long municipioId) {
        this.municipioId = municipioId;
    }

    public Integer getClase() {
        return clase;
    }

    public void setClase(Integer clase) {
        this.clase = clase;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getSeccion() {
        return seccion;
    }

    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }

    public String getManzana() {
        return manzana;
    }

    public void setManzana(String manzana) {
        this.manzana = manzana;
    }

    public String getSegmento() {
        return segmento;
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
    }

    public Integer getEdificacionNumero() {
        return edificacionNumero;
    }

    public void setEdificacionNumero(Integer edificacionNumero) {
        this.edificacionNumero = edificacionNumero;
    }

    public Integer getViviendaNumero() {
        return viviendaNumero;
    }

    public void setViviendaNumero(Integer viviendaNumero) {
        this.viviendaNumero = viviendaNumero;
    }

    public Integer getTotalHogares() {
        return totalHogares;
    }

    public void setTotalHogares(Integer totalHogares) {
        this.totalHogares = totalHogares;
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    @Override
    public String toString() {
        return direccion;
    }
}
