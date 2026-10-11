package com.udi.dao;

import com.udi.model.RolModulo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla rol_modulo (permisos de cada rol). */
public class RolModuloDAO extends BaseDAO<RolModulo> {

    @Override
    protected String tabla() {
        return "rol_modulo";
    }

    @Override
    protected String secuencia() {
        return "seq_rol_modulo";
    }

    @Override
    protected RolModulo mapear(ResultSet rs) throws SQLException {
        RolModulo e = new RolModulo();
        mapearAuditoria(rs, e);
        e.setRolId(getLong(rs, "rol_id"));
        e.setModuloId(getLong(rs, "modulo_id"));
        return e;
    }

    @Override
    public RolModulo insertar(Connection c, RolModulo e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO rol_modulo (id, rol_id, modulo_id, created_by) "
                + "VALUES (?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getRolId(), e.getModuloId(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, RolModulo e) throws SQLException {
        String sql = "UPDATE rol_modulo SET rol_id = ?, modulo_id = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getRolId(), e.getModuloId(), e.getId()) > 0;
    }

    public List<RolModulo> listarPorRol(Connection c, long rolId) throws SQLException {
        return consultar(c, "SELECT * FROM rol_modulo WHERE rol_id = ? ORDER BY modulo_id", rolId);
    }

    public Optional<RolModulo> buscarPorRolYModulo(Connection c, long rolId, long moduloId) throws SQLException {
        return consultarUno(c, "SELECT * FROM rol_modulo WHERE rol_id = ? AND modulo_id = ?", rolId, moduloId);
    }

    public boolean eliminarPorRolYModulo(Connection c, long rolId, long moduloId) throws SQLException {
        return ejecutar(c, "DELETE FROM rol_modulo WHERE rol_id = ? AND modulo_id = ?", rolId, moduloId) > 0;
    }
}
