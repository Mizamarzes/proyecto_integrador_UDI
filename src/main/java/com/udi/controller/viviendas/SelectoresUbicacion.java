package com.udi.controller.viviendas;

import com.udi.componentes.SelectBuscable;
import com.udi.model.Departamento;
import com.udi.model.Entidad;
import com.udi.model.Municipio;
import com.udi.model.Region;
import com.udi.model.Vivienda;
import com.udi.service.catalogos.UbicacionService;
import com.udi.util.Tareas;

/**
 * Encadena los selects de región, departamento y municipio: cada uno solo
 * ofrece las opciones de la selección anterior y, si esta cambia, se limpian
 * los siguientes que ya no le corresponden.
 *
 * Con {@code obligatorio} (formulario) el departamento y el municipio se
 * habilitan cuando se elige el nivel anterior; sin él (filtros) siempre están
 * habilitados y elegir un municipio completa su departamento y región.
 */
final class SelectoresUbicacion {

    private final SelectBuscable<Region> region;
    private final SelectBuscable<Departamento> departamento;
    private final SelectBuscable<Municipio> municipio;
    private final boolean obligatorio;
    private final UbicacionService servicio = new UbicacionService();

    SelectoresUbicacion(SelectBuscable<Region> region, SelectBuscable<Departamento> departamento,
                        SelectBuscable<Municipio> municipio, boolean obligatorio) {
        this.region = region;
        this.departamento = departamento;
        this.municipio = municipio;
        this.obligatorio = obligatorio;

        region.setFuente(servicio::buscarRegiones);
        departamento.setFuente((texto, desde, limite) ->
                servicio.buscarDepartamentos(id(region.getValue()), texto, desde, limite));
        municipio.setFuente((texto, desde, limite) ->
                servicio.buscarMunicipios(id(region.getValue()), id(departamento.getValue()), texto, desde, limite));

        region.valueProperty().addListener((obs, antes, r) -> {
            Departamento d = departamento.getValue();
            if (d != null && (r == null || !r.getId().equals(d.getRegionId()))) {
                departamento.setValue(null);
            }
            actualizarHabilitados();
        });
        departamento.valueProperty().addListener((obs, antes, d) -> {
            Municipio m = municipio.getValue();
            if (m != null && (d == null || !d.getId().equals(m.getDepartamentoId()))) {
                municipio.setValue(null);
            }
            if (d != null && !obligatorio && region.getValue() == null) {
                region.setValue(regionDe(d.getRegionId(), null));
            }
            actualizarHabilitados();
        });
        municipio.valueProperty().addListener((obs, antes, m) -> {
            if (m != null && !obligatorio && departamento.getValue() == null) {
                // Completa el departamento (y su región) del municipio elegido
                completarDesde(m);
            }
        });
        actualizarHabilitados();
    }

    /** Selecciona la ubicación de una vivienda existente, sin consultar la base. */
    void seleccionar(Vivienda v) {
        if (v.getMunicipioId() == null) {
            return;
        }
        region.setValue(regionDe(v.getRegionId(), v.getRegionNombre()));
        Departamento d = new Departamento();
        d.setId(v.getDepartamentoId());
        d.setNombre(v.getDepartamentoNombre());
        d.setRegionId(v.getRegionId());
        departamento.setValue(d);
        Municipio m = new Municipio();
        m.setId(v.getMunicipioId());
        m.setNombre(v.getMunicipioNombre());
        m.setDepartamentoId(v.getDepartamentoId());
        municipio.setValue(m);
    }

    void limpiar() {
        municipio.setValue(null);
        departamento.setValue(null);
        region.setValue(null);
    }

    private void actualizarHabilitados() {
        if (obligatorio) {
            departamento.setDisable(region.getValue() == null);
            municipio.setDisable(departamento.getValue() == null);
        }
    }

    private void completarDesde(Municipio m) {
        Tareas.ejecutar(() -> servicio.buscarDepartamento(m.getDepartamentoId()).orElse(null), d -> {
            if (d == null || municipio.getValue() != m || departamento.getValue() != null) {
                return;
            }
            if (region.getValue() == null || !region.getValue().getId().equals(d.getRegionId())) {
                region.setValue(regionDe(d.getRegionId(), null));
            }
            departamento.setValue(d);
        });
    }

    /**
     * Región con solo su id (y su nombre si se conoce). Si falta el nombre se
     * pide a la base; el select se repinta al reemplazar el valor.
     */
    private Region regionDe(Long id, String nombre) {
        Region r = new Region();
        r.setId(id);
        r.setNombre(nombre == null ? "…" : nombre);
        if (nombre == null && id != null) {
            Tareas.ejecutar(() -> servicio.buscarRegion(id).orElse(null), encontrada -> {
                if (encontrada != null && region.getValue() == r) {
                    region.setValue(encontrada);
                }
            });
        }
        return r;
    }

    private static Long id(Entidad entidad) {
        return entidad == null ? null : entidad.getId();
    }
}
