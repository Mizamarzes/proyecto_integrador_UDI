package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.HogarDAO;
import com.udi.dao.VisitaDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.Visita;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Control de visitas del capítulo B (RF-08). */
public class VisitaService {

    public static final int MAXIMO_VISITAS = 14;

    private final VisitaDAO visitaDao = new VisitaDAO();
    private final HogarDAO hogarDao = new HogarDAO();

    public List<Visita> listar() {
        return ConexionBD.consultar(visitaDao::listar);
    }

    public List<Visita> listarPorHogar(long hogarId) {
        return ConexionBD.consultar(c -> visitaDao.listarPorHogar(c, hogarId));
    }

    public Optional<Visita> buscar(long id) {
        return ConexionBD.consultar(c -> visitaDao.buscarPorId(c, id));
    }

    /** Si no trae número de visita, se le asigna el siguiente del hogar. */
    public Visita crear(Visita visita) {
        Validar.requerido(visita.getHogarId(), "Hogar");
        return ConexionBD.enTransaccion(c -> {
            ReglasEncuesta.exigirHogarEditable(c, visita.getHogarId());
            if (visita.getNumeroVisita() == null) {
                visita.setNumeroVisita(visitaDao.siguienteNumero(c, visita.getHogarId()));
            }
            visita.setId(null);
            validar(c, visita);
            visita.setCreatedBy(Sesion.idUsuario());
            return visitaDao.insertar(c, visita);
        });
    }

    public Visita actualizar(Visita visita) {
        Validar.requerido(visita.getId(), "Id de la visita");
        return ConexionBD.enTransaccion(c -> {
            Visita actual = Validar.encontrado(visitaDao.buscarPorId(c, visita.getId()),
                    "No existe la visita " + visita.getId() + ".");
            ReglasEncuesta.exigirHogarEditable(c, actual.getHogarId());
            visita.setHogarId(actual.getHogarId());
            validar(c, visita);
            visitaDao.actualizar(c, visita);
            return visita;
        });
    }

    public boolean eliminar(long visitaId) {
        return ConexionBD.enTransaccion(c -> {
            Visita actual = Validar.encontrado(visitaDao.buscarPorId(c, visitaId), "No existe la visita " + visitaId + ".");
            ReglasEncuesta.exigirHogarEditable(c, actual.getHogarId());
            return visitaDao.eliminar(c, visitaId);
        });
    }

    private void validar(Connection c, Visita visita) throws SQLException {
        Validar.condicion(hogarDao.existe(c, visita.getHogarId()), "No existe el hogar " + visita.getHogarId() + ".");
        Validar.rango(visita.getNumeroVisita(), 1, MAXIMO_VISITAS, "Número de visita");
        Validar.requerido(visita.getFecha(), "Fecha de la visita");
        Validar.requerido(visita.getHoraInicio(), "Hora de inicio");
        Validar.requerido(visita.getResultadoVisita(), "Resultado de la entrevista");
        if (visita.getHoraFin() != null) {
            Validar.condicion(!visita.getHoraFin().isBefore(visita.getHoraInicio()),
                    "La hora de terminación no puede ser anterior a la hora de inicio.");
        }
        if (visita.getFechaProxima() != null) {
            Validar.condicion(!visita.getFechaProxima().isBefore(visita.getFecha()),
                    "La fecha de la próxima visita no puede ser anterior a la fecha de la visita.");
        }
        for (Visita otra : visitaDao.listarPorHogar(c, visita.getHogarId())) {
            if (otra.getNumeroVisita().equals(visita.getNumeroVisita()) && !otra.getId().equals(visita.getId())) {
                throw new ReglaNegocioException("El hogar ya tiene registrada la visita número " + visita.getNumeroVisita() + ".");
            }
        }
    }
}
