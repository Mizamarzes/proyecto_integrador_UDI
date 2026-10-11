package com.udi.dao;

import com.udi.model.Usuario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla usuario. */
public class UsuarioDAO extends BaseDAO<Usuario> {

    @Override
    protected String tabla() {
        return "usuario";
    }

    @Override
    protected String secuencia() {
        return "seq_usuario";
    }

    @Override
    protected Usuario mapear(ResultSet rs) throws SQLException {
        Usuario e = new Usuario();
        mapearAuditoria(rs, e);
        e.setEmail(rs.getString("email"));
        e.setPassword(rs.getString("password"));
        e.setNombre(rs.getString("nombre"));
        e.setApellido(rs.getString("apellido"));
        e.setTelefono(rs.getString("telefono"));
        e.setRolId(getLong(rs, "rol_id"));
        e.setActivo(getBoolean(rs, "activo"));
        e.setUltimoAcceso(getLocalDateTime(rs, "ultimo_acceso"));
        return e;
    }

    @Override
    public Usuario insertar(Connection c, Usuario e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO usuario (id, email, password, nombre, apellido, telefono, rol_id, activo, "
                + "ultimo_acceso, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getEmail(), e.getPassword(), e.getNombre(), e.getApellido(), e.getTelefono(),
                e.getRolId(), e.isActivo(), e.getUltimoAcceso(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Usuario e) throws SQLException {
        String sql = "UPDATE usuario SET email = ?, nombre = ?, apellido = ?, telefono = ?, rol_id = ?, "
                + "activo = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getEmail(), e.getNombre(), e.getApellido(), e.getTelefono(), e.getRolId(),
                e.isActivo(), e.getId()) > 0;
    }

    /** Busca por correo sin distinguir mayúsculas y minúsculas. */
    public Optional<Usuario> buscarPorEmail(Connection c, String email) throws SQLException {
        return consultarUno(c, "SELECT * FROM usuario WHERE LOWER(email) = LOWER(?)", email);
    }

    public List<Usuario> listarPorRol(Connection c, long rolId) throws SQLException {
        return consultar(c, "SELECT * FROM usuario WHERE rol_id = ? ORDER BY apellido, nombre", rolId);
    }

    /** Guarda el hash de la nueva contraseña. */
    public boolean actualizarPassword(Connection c, long id, String passwordHash) throws SQLException {
        return ejecutar(c, "UPDATE usuario SET password = ?, updated_at = SYSTIMESTAMP WHERE id = ?",
                passwordHash, id) > 0;
    }

    public boolean actualizarActivo(Connection c, long id, boolean activo) throws SQLException {
        return ejecutar(c, "UPDATE usuario SET activo = ?, updated_at = SYSTIMESTAMP WHERE id = ?", activo, id) > 0;
    }

    /** Registra el inicio de sesión. No cuenta como modificación (no cambia updated_at). */
    public boolean actualizarUltimoAcceso(Connection c, long id) throws SQLException {
        return ejecutar(c, "UPDATE usuario SET ultimo_acceso = SYSTIMESTAMP WHERE id = ?", id) > 0;
    }
}
