package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.EncuestaDAO;
import com.udi.dao.HogarDAO;
import com.udi.dao.MunicipioDAO;
import com.udi.dao.ViviendaDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.Hogar;
import com.udi.model.Vivienda;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Viviendas y hogares del capítulo A (RF-02). */
public class HogarService {

    private final ViviendaDAO viviendaDao = new ViviendaDAO();
    private final HogarDAO hogarDao = new HogarDAO();
    private final MunicipioDAO municipioDao = new MunicipioDAO();
    private final EncuestaDAO encuestaDao = new EncuestaDAO();

    // ------------------------------------------------------------------
    // Viviendas
    // ------------------------------------------------------------------

    public List<Vivienda> listarViviendas() {
        return ConexionBD.consultar(viviendaDao::listar);
    }

    public List<Vivienda> listarViviendasPorMunicipio(long municipioId) {
        return ConexionBD.consultar(c -> viviendaDao.listarPorMunicipio(c, municipioId));
    }

    public Optional<Vivienda> buscarVivienda(long id) {
        return ConexionBD.consultar(c -> viviendaDao.buscarPorId(c, id));
    }

    public Vivienda crearVivienda(Vivienda vivienda) {
        return ConexionBD.enTransaccion(c -> {
            validarVivienda(c, vivienda);
            vivienda.setId(null);
            vivienda.setCreatedBy(Sesion.idUsuario());
            return viviendaDao.insertar(c, vivienda);
        });
    }

    /** total_hogares no puede quedar por debajo de los hogares ya registrados. */
    public Vivienda actualizarVivienda(Vivienda vivienda) {
        Validar.requerido(vivienda.getId(), "Id de la vivienda");
        return ConexionBD.enTransaccion(c -> {
            validarVivienda(c, vivienda);
            long registrados = hogarDao.contarPorVivienda(c, vivienda.getId());
            Validar.condicion(vivienda.getTotalHogares() >= registrados, "La vivienda ya tiene " + registrados
                    + " hogares registrados; el total de hogares no puede ser menor.");
            Validar.condicion(viviendaDao.actualizar(c, vivienda), "No existe la vivienda " + vivienda.getId() + ".");
            return vivienda;
        });
    }

    /** Solo se puede eliminar una vivienda sin hogares. */
    public boolean eliminarVivienda(long viviendaId) {
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(hogarDao.contarPorVivienda(c, viviendaId) == 0,
                    "No se puede eliminar la vivienda porque tiene hogares registrados.");
            return viviendaDao.eliminar(c, viviendaId);
        });
    }

    private void validarVivienda(Connection c, Vivienda vivienda) throws SQLException {
        Validar.requerido(vivienda.getMunicipioId(), "Municipio");
        Validar.rango(vivienda.getClase(), 1, 3, "Clase");
        Validar.rango(vivienda.getTotalHogares(), 1, 99, "Total de hogares en la vivienda");
        Validar.requerido(vivienda.getDireccion(), "Dirección");
        Validar.condicion(municipioDao.existe(c, vivienda.getMunicipioId()),
                "No existe el municipio " + vivienda.getMunicipioId() + ".");
    }

    // ------------------------------------------------------------------
    // Hogares
    // ------------------------------------------------------------------

    public List<Hogar> listarHogares() {
        return ConexionBD.consultar(hogarDao::listar);
    }

    public List<Hogar> listarHogaresPorVivienda(long viviendaId) {
        return ConexionBD.consultar(c -> hogarDao.listarPorVivienda(c, viviendaId));
    }

    public Optional<Hogar> buscarHogar(long id) {
        return ConexionBD.consultar(c -> hogarDao.buscarPorId(c, id));
    }

    public Hogar crearHogar(Hogar hogar) {
        return ConexionBD.enTransaccion(c -> {
            hogar.setId(null);
            validarHogar(c, hogar);
            hogar.setCreatedBy(Sesion.idUsuario());
            return hogarDao.insertar(c, hogar);
        });
    }

    public Hogar actualizarHogar(Hogar hogar) {
        Validar.requerido(hogar.getId(), "Id del hogar");
        return ConexionBD.enTransaccion(c -> {
            ReglasEncuesta.exigirHogarEditable(c, hogar.getId());
            validarHogar(c, hogar);
            Validar.condicion(hogarDao.actualizar(c, hogar), "No existe el hogar " + hogar.getId() + ".");
            return hogar;
        });
    }

    /** Solo se puede eliminar un hogar que todavía no tiene encuesta. */
    public boolean eliminarHogar(long hogarId) {
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(!encuestaDao.buscarPorHogar(c, hogarId).isPresent(),
                    "No se puede eliminar el hogar porque ya tiene una encuesta.");
            return hogarDao.eliminar(c, hogarId);
        });
    }

    /** A.13: el número del hogar va de 1 al total de hogares de la vivienda y no se repite. */
    private void validarHogar(Connection c, Hogar hogar) throws SQLException {
        Validar.requerido(hogar.getViviendaId(), "Vivienda");
        Vivienda vivienda = Validar.encontrado(viviendaDao.buscarPorId(c, hogar.getViviendaId()),
                "No existe la vivienda " + hogar.getViviendaId() + ".");
        Validar.rango(hogar.getHogarNumero(), 1, vivienda.getTotalHogares(), "Hogar número");
        Validar.rango(hogar.getTotalPersonas(), 1, 99, "Total de personas en el hogar");
        Optional<Hogar> mismoNumero = hogarDao.buscarPorViviendaYNumero(c, hogar.getViviendaId(), hogar.getHogarNumero());
        if (mismoNumero.isPresent() && !mismoNumero.get().getId().equals(hogar.getId())) {
            throw new ReglaNegocioException("La vivienda ya tiene registrado el hogar número " + hogar.getHogarNumero() + ".");
        }
    }
}
