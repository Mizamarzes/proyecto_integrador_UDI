package com.udi.service.viviendas;

import com.udi.config.ConexionBD;
import com.udi.dao.catalogos.MunicipioDAO;
import com.udi.dao.encuestas.EncuestaDAO;
import com.udi.dao.viviendas.HogarDAO;
import com.udi.dao.viviendas.ViviendaDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.RutasModulo;
import com.udi.model.Hogar;
import com.udi.model.Vivienda;
import com.udi.service.encuestas.ReglasEncuesta;
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

    /** Viviendas filtradas por región, departamento y municipio; cada filtro en null no se aplica. */
    public List<Vivienda> listarViviendas(Long regionId, Long departamentoId, Long municipioId) {
        return ConexionBD.consultar(c -> viviendaDao.filtrar(c, regionId, departamentoId, municipioId));
    }

    public List<Vivienda> listarViviendasPorMunicipio(long municipioId) {
        return ConexionBD.consultar(c -> viviendaDao.listarPorMunicipio(c, municipioId));
    }

    public Optional<Vivienda> buscarVivienda(long id) {
        return ConexionBD.consultar(c -> viviendaDao.buscarPorId(c, id));
    }

    public Vivienda crearVivienda(Vivienda vivienda) {
        Sesion.exigirModulo(RutasModulo.VIVIENDAS);
        return ConexionBD.enTransaccion(c -> {
            validarVivienda(c, vivienda);
            vivienda.setId(null);
            vivienda.setCreatedBy(Sesion.idUsuario());
            viviendaDao.insertar(c, vivienda);
            return viviendaDao.buscarPorId(c, vivienda.getId()).orElse(vivienda);
        });
    }

    public Vivienda actualizarVivienda(Vivienda vivienda) {
        Sesion.exigirModulo(RutasModulo.VIVIENDAS);
        Validar.requerido(vivienda.getId(), "Id de la vivienda");
        return ConexionBD.enTransaccion(c -> {
            validarVivienda(c, vivienda);
            Validar.condicion(viviendaDao.actualizar(c, vivienda), "No existe la vivienda " + vivienda.getId() + ".");
            return viviendaDao.buscarPorId(c, vivienda.getId()).orElse(vivienda);
        });
    }

    /** Solo se puede eliminar una vivienda sin hogares. */
    public boolean eliminarVivienda(long viviendaId) {
        Sesion.exigirModulo(RutasModulo.VIVIENDAS);
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(hogarDao.contarPorVivienda(c, viviendaId) == 0,
                    "No se puede eliminar la vivienda porque tiene hogares registrados.");
            return viviendaDao.eliminar(c, viviendaId);
        });
    }

    private void validarVivienda(Connection c, Vivienda vivienda) throws SQLException {
        Validar.requerido(vivienda.getMunicipioId(), "Municipio");
        Validar.rango(vivienda.getClase(), 1, 3, "Clase");
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

    /** El número del hogar (A.13) lo asigna el sistema: el siguiente al mayor de la vivienda. */
    public Hogar crearHogar(Hogar hogar) {
        Sesion.exigirModulo(RutasModulo.VIVIENDAS);
        return ConexionBD.enTransaccion(c -> {
            hogar.setId(null);
            Validar.requerido(hogar.getViviendaId(), "Vivienda");
            hogar.setHogarNumero(hogarDao.siguienteNumero(c, hogar.getViviendaId()));
            Validar.condicion(hogar.getHogarNumero() <= 99, "La vivienda ya tiene el máximo de 99 hogares.");
            validarHogar(c, hogar);
            hogar.setCreatedBy(Sesion.idUsuario());
            return hogarDao.insertar(c, hogar);
        });
    }

    /** El número del hogar no cambia al editar. */
    public Hogar actualizarHogar(Hogar hogar) {
        Sesion.exigirModulo(RutasModulo.VIVIENDAS);
        Validar.requerido(hogar.getId(), "Id del hogar");
        return ConexionBD.enTransaccion(c -> {
            ReglasEncuesta.exigirHogarEditable(c, hogar.getId());
            Hogar actual = Validar.encontrado(hogarDao.buscarPorId(c, hogar.getId()), "No existe el hogar " + hogar.getId() + ".");
            hogar.setViviendaId(actual.getViviendaId());
            hogar.setHogarNumero(actual.getHogarNumero());
            validarHogar(c, hogar);
            Validar.condicion(hogarDao.actualizar(c, hogar), "No existe el hogar " + hogar.getId() + ".");
            return hogar;
        });
    }

    /** Solo se puede eliminar un hogar que todavía no tiene encuesta. */
    public boolean eliminarHogar(long hogarId) {
        Sesion.exigirModulo(RutasModulo.VIVIENDAS);
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(!encuestaDao.buscarPorHogar(c, hogarId).isPresent(),
                    "No se puede eliminar el hogar porque ya tiene una encuesta.");
            return hogarDao.eliminar(c, hogarId);
        });
    }

    /**
     * A.13: el número del hogar va de 1 a 99 (NUMBER(2)) y no se repite en la
     * vivienda. El total de hogares de la vivienda (A.12) es la cantidad registrada.
     */
    private void validarHogar(Connection c, Hogar hogar) throws SQLException {
        Validar.requerido(hogar.getViviendaId(), "Vivienda");
        Validar.condicion(viviendaDao.existe(c, hogar.getViviendaId()),
                "No existe la vivienda " + hogar.getViviendaId() + ".");
        Validar.rango(hogar.getHogarNumero(), 1, 99, "Hogar número");
        Validar.rango(hogar.getTotalPersonas(), 1, 99, "Total de personas en el hogar");
        Optional<Hogar> mismoNumero = hogarDao.buscarPorViviendaYNumero(c, hogar.getViviendaId(), hogar.getHogarNumero());
        if (mismoNumero.isPresent() && !mismoNumero.get().getId().equals(hogar.getId())) {
            throw new ReglaNegocioException("La vivienda ya tiene registrado el hogar número " + hogar.getHogarNumero() + ".");
        }
    }
}
