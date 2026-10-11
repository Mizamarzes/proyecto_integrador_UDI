package com.udi.dao;

import com.udi.model.Rol;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Acceso a la tabla rol. */
public class RolDAO extends BaseDAO<Rol> {

    @Override
    protected String tabla() {
        return "rol";
    }

    @Override
    protected String secuencia() {
        return "seq_rol";
    }

    @Override
    protected Rol mapear(ResultSet rs) throws SQLException {
        Rol e = new Rol();
        mapearAuditoria(rs, e);
        e.setNombre(rs.getString("nombre"));
        return e;
    }

    @Override
    public Rol insertar(Connection c, Rol e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO rol (id, nombre, created_by) "
                + "VALUES (?, ?, ?)";
        ejecutar(c, sql, id, e.getNombre(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Rol e) throws SQLException {
        String sql = "UPDATE rol SET nombre = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getNombre(), e.getId()) > 0;
    }

    public Optional<Rol> buscarPorNombre(Connection c, String nombre) throws SQLException {
        return consultarUno(c, "SELECT * FROM rol WHERE nombre = ?", nombre);
    }
}
