package com.udi.service;

import com.udi.dao.EncuestaDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.Encuesta;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/** Reglas sobre el estado de la encuesta que comparten varios servicios (RF-06). */
final class ReglasEncuesta {

    private static final EncuestaDAO ENCUESTA_DAO = new EncuestaDAO();

    private ReglasEncuesta() {
    }

    /** Devuelve la encuesta si existe y no está cerrada. */
    static Encuesta exigirEditable(Connection c, long encuestaId) throws SQLException {
        Encuesta encuesta = Validar.encontrado(ENCUESTA_DAO.buscarPorId(c, encuestaId),
                "No existe la encuesta " + encuestaId + ".");
        if (encuesta.estaCerrada()) {
            throw new ReglaNegocioException("La encuesta " + encuestaId
                    + " está cerrada. Un administrador debe reabrirla para hacer cambios.");
        }
        return encuesta;
    }

    /** Falla si el hogar tiene una encuesta cerrada. Un hogar sin encuesta se puede editar. */
    static void exigirHogarEditable(Connection c, long hogarId) throws SQLException {
        Optional<Encuesta> encuesta = ENCUESTA_DAO.buscarPorHogar(c, hogarId);
        if (encuesta.isPresent()) {
            exigirEditable(c, encuesta.get().getId());
        }
    }
}
