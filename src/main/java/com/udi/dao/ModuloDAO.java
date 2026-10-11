package com.udi.dao;

import com.udi.model.Modulo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla modulo. */
public class ModuloDAO extends BaseDAO<Modulo> {

    @Override
    protected String tabla() {
        return "modulo";
    }

    @Override
    protected String secuencia() {
        return "seq_modulo";
    }

    @Override
    protected Modulo mapear(ResultSet rs) throws SQLException {
        Modulo e = new Modulo();
        mapearAuditoria(rs, e);
        e.setNombre(rs.getString("nombre"));
        e.setSlug(rs.getString("slug"));
        return e;
    }

    @Override
    public Modulo insertar(Connection c, Modulo e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO modulo (id, nombre, slug, created_by) "
                + "VALUES (?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getNombre(), e.getSlug(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Modulo e) throws SQLException {
        String sql = "UPDATE modulo SET nombre = ?, slug = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getNombre(), e.getSlug(), e.getId()) > 0;
    }

    public Optional<Modulo> buscarPorSlug(Connection c, String slug) throws SQLException {
        return consultarUno(c, "SELECT * FROM modulo WHERE slug = ?", slug);
    }

    /** Módulos a los que tiene acceso un rol. */
    public List<Modulo> listarPorRol(Connection c, long rolId) throws SQLException {
        return consultar(c, "SELECT m.* FROM modulo m JOIN rol_modulo rm ON rm.modulo_id = m.id"
                + " WHERE rm.rol_id = ? ORDER BY m.id", rolId);
    }
}
