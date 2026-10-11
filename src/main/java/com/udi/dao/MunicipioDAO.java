package com.udi.dao;

import com.udi.model.Municipio;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Acceso a la tabla municipio. */
public class MunicipioDAO extends BaseDAO<Municipio> {

    @Override
    protected String tabla() {
        return "municipio";
    }

    @Override
    protected String secuencia() {
        return "seq_municipio";
    }

    @Override
    protected Municipio mapear(ResultSet rs) throws SQLException {
        Municipio e = new Municipio();
        mapearAuditoria(rs, e);
        e.setNombre(rs.getString("nombre"));
        e.setDepartamentoId(getLong(rs, "departamento_id"));
        return e;
    }

    @Override
    public Municipio insertar(Connection c, Municipio e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO municipio (id, nombre, departamento_id) "
                + "VALUES (?, ?, ?)";
        ejecutar(c, sql, id, e.getNombre(), e.getDepartamentoId());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Municipio e) throws SQLException {
        String sql = "UPDATE municipio SET nombre = ?, departamento_id = ? WHERE id = ?";
        return ejecutar(c, sql, e.getNombre(), e.getDepartamentoId(), e.getId()) > 0;
    }

    public List<Municipio> listarPorDepartamento(Connection c, long departamentoId) throws SQLException {
        return consultar(c, "SELECT * FROM municipio WHERE departamento_id = ? ORDER BY nombre", departamentoId);
    }
}
