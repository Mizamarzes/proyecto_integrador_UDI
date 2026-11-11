package com.udi.dao.verificacion;

import com.udi.dao.BaseDAO;
import com.udi.model.AuditoriaEncuesta;
import com.udi.model.enums.AccionAuditoria;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla auditoria_encuesta (historial de cada caso, RF-10). */
public class AuditoriaEncuestaDAO extends BaseDAO<AuditoriaEncuesta> {

    /** Une el nombre del usuario que hizo la acción. */
    private static final String SELECT = "SELECT a.*, u.nombre || ' ' || u.apellido usuario_nombre"
            + " FROM auditoria_encuesta a LEFT JOIN usuario u ON u.id = a.created_by";

    @Override
    protected String tabla() {
        return "auditoria_encuesta";
    }

    @Override
    protected String secuencia() {
        return "seq_auditoria_encuesta";
    }

    @Override
    protected AuditoriaEncuesta mapear(ResultSet rs) throws SQLException {
        AuditoriaEncuesta e = new AuditoriaEncuesta();
        mapearAuditoria(rs, e);
        e.setEncuestaId(getLong(rs, "encuesta_id"));
        e.setAccion(getEnum(rs, "accion", AccionAuditoria.class));
        e.setObservacion(rs.getString("observacion"));
        e.setUsuarioNombre(rs.getString("usuario_nombre"));
        return e;
    }

    @Override
    public Optional<AuditoriaEncuesta> buscarPorId(Connection c, long id) throws SQLException {
        return consultarUno(c, SELECT + " WHERE a.id = ?", id);
    }

    @Override
    public List<AuditoriaEncuesta> listar(Connection c) throws SQLException {
        return consultar(c, SELECT + " ORDER BY a.id");
    }

    @Override
    public AuditoriaEncuesta insertar(Connection c, AuditoriaEncuesta e) throws SQLException {
        long id = asignarId(c, e);
        ejecutar(c, "INSERT INTO auditoria_encuesta (id, encuesta_id, accion, observacion, created_by)"
                + " VALUES (?, ?, ?, ?, ?)", id, e.getEncuestaId(), e.getAccion(), e.getObservacion(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, AuditoriaEncuesta e) throws SQLException {
        return ejecutar(c, "UPDATE auditoria_encuesta SET accion = ?, observacion = ?, updated_at = SYSTIMESTAMP"
                + " WHERE id = ?", e.getAccion(), e.getObservacion(), e.getId()) > 0;
    }

    /** Historial de la encuesta, del más reciente al más antiguo. */
    public List<AuditoriaEncuesta> listarPorEncuesta(Connection c, long encuestaId) throws SQLException {
        return consultar(c, SELECT + " WHERE a.encuesta_id = ? ORDER BY a.created_at DESC, a.id DESC", encuestaId);
    }

    /** Último registro de la acción en la encuesta (por ejemplo, la última devolución). */
    public Optional<AuditoriaEncuesta> ultimaPorAccion(Connection c, long encuestaId, AccionAuditoria accion)
            throws SQLException {
        return consultarUno(c, SELECT + " WHERE a.encuesta_id = ? AND a.accion = ? ORDER BY a.created_at DESC, a.id DESC",
                encuestaId, accion);
    }
}
