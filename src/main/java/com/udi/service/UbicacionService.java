package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.DepartamentoDAO;
import com.udi.dao.MunicipioDAO;
import com.udi.dao.RegionDAO;
import com.udi.model.Departamento;
import com.udi.model.Municipio;
import com.udi.model.Region;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.util.List;
import java.util.Optional;

/**
 * Catálogos geográficos del capítulo A: región, departamento y municipio.
 * Las operaciones que modifican datos solo las puede hacer un administrador.
 */
public class UbicacionService {

    private final RegionDAO regionDao = new RegionDAO();
    private final DepartamentoDAO departamentoDao = new DepartamentoDAO();
    private final MunicipioDAO municipioDao = new MunicipioDAO();

    // ------------------------------------------------------------------
    // Regiones
    // ------------------------------------------------------------------

    public List<Region> listarRegiones() {
        return ConexionBD.consultar(regionDao::listar);
    }

    public Optional<Region> buscarRegion(long id) {
        return ConexionBD.consultar(c -> regionDao.buscarPorId(c, id));
    }

    public Region crearRegion(Region region) {
        Sesion.exigirAdmin();
        Validar.requerido(region.getNombre(), "Nombre de la región");
        region.setId(null);
        return ConexionBD.enTransaccion(c -> regionDao.insertar(c, region));
    }

    public Region actualizarRegion(Region region) {
        Sesion.exigirAdmin();
        Validar.requerido(region.getId(), "Id de la región");
        Validar.requerido(region.getNombre(), "Nombre de la región");
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(regionDao.actualizar(c, region), "No existe la región " + region.getId() + ".");
            return region;
        });
    }

    /** Falla si la región tiene departamentos. */
    public boolean eliminarRegion(long regionId) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(departamentoDao.listarPorRegion(c, regionId).isEmpty(),
                    "No se puede eliminar la región porque tiene departamentos.");
            return regionDao.eliminar(c, regionId);
        });
    }

    // ------------------------------------------------------------------
    // Departamentos
    // ------------------------------------------------------------------

    public List<Departamento> listarDepartamentos() {
        return ConexionBD.consultar(departamentoDao::listar);
    }

    public List<Departamento> listarDepartamentosPorRegion(long regionId) {
        return ConexionBD.consultar(c -> departamentoDao.listarPorRegion(c, regionId));
    }

    public Optional<Departamento> buscarDepartamento(long id) {
        return ConexionBD.consultar(c -> departamentoDao.buscarPorId(c, id));
    }

    public Departamento crearDepartamento(Departamento departamento) {
        Sesion.exigirAdmin();
        validarDepartamento(departamento);
        departamento.setId(null);
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(regionDao.existe(c, departamento.getRegionId()),
                    "No existe la región " + departamento.getRegionId() + ".");
            return departamentoDao.insertar(c, departamento);
        });
    }

    public Departamento actualizarDepartamento(Departamento departamento) {
        Sesion.exigirAdmin();
        Validar.requerido(departamento.getId(), "Id del departamento");
        validarDepartamento(departamento);
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(regionDao.existe(c, departamento.getRegionId()),
                    "No existe la región " + departamento.getRegionId() + ".");
            Validar.condicion(departamentoDao.actualizar(c, departamento),
                    "No existe el departamento " + departamento.getId() + ".");
            return departamento;
        });
    }

    /** Falla si el departamento tiene municipios. */
    public boolean eliminarDepartamento(long departamentoId) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(municipioDao.listarPorDepartamento(c, departamentoId).isEmpty(),
                    "No se puede eliminar el departamento porque tiene municipios.");
            return departamentoDao.eliminar(c, departamentoId);
        });
    }

    private static void validarDepartamento(Departamento departamento) {
        Validar.requerido(departamento.getNombre(), "Nombre del departamento");
        Validar.requerido(departamento.getRegionId(), "Región");
    }

    // ------------------------------------------------------------------
    // Municipios
    // ------------------------------------------------------------------

    public List<Municipio> listarMunicipios() {
        return ConexionBD.consultar(municipioDao::listar);
    }

    public List<Municipio> listarMunicipiosPorDepartamento(long departamentoId) {
        return ConexionBD.consultar(c -> municipioDao.listarPorDepartamento(c, departamentoId));
    }

    public Optional<Municipio> buscarMunicipio(long id) {
        return ConexionBD.consultar(c -> municipioDao.buscarPorId(c, id));
    }

    public Municipio crearMunicipio(Municipio municipio) {
        Sesion.exigirAdmin();
        validarMunicipio(municipio);
        municipio.setId(null);
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(departamentoDao.existe(c, municipio.getDepartamentoId()),
                    "No existe el departamento " + municipio.getDepartamentoId() + ".");
            return municipioDao.insertar(c, municipio);
        });
    }

    public Municipio actualizarMunicipio(Municipio municipio) {
        Sesion.exigirAdmin();
        Validar.requerido(municipio.getId(), "Id del municipio");
        validarMunicipio(municipio);
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(departamentoDao.existe(c, municipio.getDepartamentoId()),
                    "No existe el departamento " + municipio.getDepartamentoId() + ".");
            Validar.condicion(municipioDao.actualizar(c, municipio), "No existe el municipio " + municipio.getId() + ".");
            return municipio;
        });
    }

    /** Falla (por llave foránea) si el municipio tiene viviendas. */
    public boolean eliminarMunicipio(long municipioId) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> municipioDao.eliminar(c, municipioId));
    }

    private static void validarMunicipio(Municipio municipio) {
        Validar.requerido(municipio.getNombre(), "Nombre del municipio");
        Validar.requerido(municipio.getDepartamentoId(), "Departamento");
    }
}
