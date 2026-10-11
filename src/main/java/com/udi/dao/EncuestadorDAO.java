package com.udi.dao;

import com.udi.model.Encuestador;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Acceso a la tabla encuestador. */
public class EncuestadorDAO extends BaseDAO<Encuestador> {

    @Override
    protected String tabla() {
        return "encuestador";
    }

    @Override
    protected String secuencia() {
        return "seq_encuestador";
    }

    @Override
    protected Encuestador mapear(ResultSet rs) throws SQLException {
        Encuestador e = new Encuestador();
        mapearAuditoria(rs, e);
        e.setUsuarioId(getLong(rs, "usuario_id"));
        return e;
    }

    @Override
    public Encuestador insertar(Connection c, Encuestador e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO encuestador (id, usuario_id, created_by) "
                + "VALUES (?, ?, ?)";
        ejecutar(c, sql, id, e.getUsuarioId(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Encuestador e) throws SQLException {
        String sql = "UPDATE encuestador SET usuario_id = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getUsuarioId(), e.getId()) > 0;
    }

    public Optional<Encuestador> buscarPorUsuario(Connection c, long usuarioId) throws SQLException {
        return consultarUno(c, "SELECT * FROM encuestador WHERE usuario_id = ?", usuarioId);
    }
}
