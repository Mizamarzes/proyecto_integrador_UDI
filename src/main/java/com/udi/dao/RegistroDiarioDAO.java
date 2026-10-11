package com.udi.dao;

import com.udi.model.RegistroDiario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla registro_diario (página de cada día del capítulo D). */
public class RegistroDiarioDAO extends BaseDAO<RegistroDiario> {

    @Override
    protected String tabla() {
        return "registro_diario";
    }

    @Override
    protected String secuencia() {
        return "seq_registro_diario";
    }

    @Override
    protected RegistroDiario mapear(ResultSet rs) throws SQLException {
        RegistroDiario e = new RegistroDiario();
        mapearAuditoria(rs, e);
        e.setEncuestaId(getLong(rs, "encuesta_id"));
        e.setNumeroDia(getInteger(rs, "numero_dia"));
        e.setFecha(getLocalDate(rs, "fecha"));
        e.setObservaciones(rs.getString("observaciones"));
        return e;
    }

    @Override
    public RegistroDiario insertar(Connection c, RegistroDiario e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO registro_diario (id, encuesta_id, numero_dia, fecha, observaciones, "
                + "created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getEncuestaId(), e.getNumeroDia(), e.getFecha(), e.getObservaciones(),
                e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, RegistroDiario e) throws SQLException {
        String sql = "UPDATE registro_diario SET encuesta_id = ?, numero_dia = ?, fecha = ?, "
                + "observaciones = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getEncuestaId(), e.getNumeroDia(), e.getFecha(), e.getObservaciones(),
                e.getId()) > 0;
    }

    public List<RegistroDiario> listarPorEncuesta(Connection c, long encuestaId) throws SQLException {
        return consultar(c, "SELECT * FROM registro_diario WHERE encuesta_id = ? ORDER BY numero_dia", encuestaId);
    }

    public Optional<RegistroDiario> buscarPorEncuestaYDia(Connection c, long encuestaId, int numeroDia) throws SQLException {
        return consultarUno(c, "SELECT * FROM registro_diario WHERE encuesta_id = ? AND numero_dia = ?", encuestaId, numeroDia);
    }
}
