package com.udi.dao;

import com.udi.model.GrupoAlimento;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla grupo_alimento (catálogo del capítulo C). */
public class GrupoAlimentoDAO extends BaseDAO<GrupoAlimento> {

    @Override
    protected String tabla() {
        return "grupo_alimento";
    }

    @Override
    protected String secuencia() {
        return "seq_grupo_alimento";
    }

    @Override
    protected GrupoAlimento mapear(ResultSet rs) throws SQLException {
        GrupoAlimento e = new GrupoAlimento();
        mapearAuditoria(rs, e);
        e.setNombre(rs.getString("nombre"));
        return e;
    }

    @Override
    public GrupoAlimento insertar(Connection c, GrupoAlimento e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO grupo_alimento (id, nombre, created_by) "
                + "VALUES (?, ?, ?)";
        ejecutar(c, sql, id, e.getNombre(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, GrupoAlimento e) throws SQLException {
        String sql = "UPDATE grupo_alimento SET nombre = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getNombre(), e.getId()) > 0;
    }
}
