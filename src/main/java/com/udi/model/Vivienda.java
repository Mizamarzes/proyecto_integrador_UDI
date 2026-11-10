package com.udi.model;

/**
 * Espacio físico donde viven uno o más hogares (capítulo A).
 *
 * Los nombres de la ubicación (región, departamento, municipio) y el total de
 * hogares no son columnas de la tabla: los llena {@code ViviendaDAO} al
 * consultar, y el total de hogares (A.12) es la cantidad de hogares registrados.
 */
public class Vivienda extends EntidadAuditable {

    private Long municipioId;
    private Integer clase;
    private String sector;
    private String seccion;
    private String manzana;
    private String segmento;
    private Integer edificacionNumero;
    private Integer viviendaNumero;
    private String barrio;
    private String direccion;
    private String telefono;

    // Solo lectura, calculados en la consulta
    private Long departamentoId;
    private Long regionId;
    private String municipioNombre;
    private String departamentoNombre;
    private String regionNombre;
    private int totalHogares;

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

    public Long getDepartamentoId() {
        return departamentoId;
    }

    public void setDepartamentoId(Long departamentoId) {
        this.departamentoId = departamentoId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getMunicipioNombre() {
        return municipioNombre;
    }

    public void setMunicipioNombre(String municipioNombre) {
        this.municipioNombre = municipioNombre;
    }

    public String getDepartamentoNombre() {
        return departamentoNombre;
    }

    public void setDepartamentoNombre(String departamentoNombre) {
        this.departamentoNombre = departamentoNombre;
    }

    public String getRegionNombre() {
        return regionNombre;
    }

    public void setRegionNombre(String regionNombre) {
        this.regionNombre = regionNombre;
    }

    /** A.12: hogares registrados en la vivienda. */
    public int getTotalHogares() {
        return totalHogares;
    }

    public void setTotalHogares(int totalHogares) {
        this.totalHogares = totalHogares;
    }

    @Override
    public String toString() {
        return direccion;
    }
}
