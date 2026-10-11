package com.udi.dao;

import com.udi.model.Departamento;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Acceso a la tabla departamento. */
public class DepartamentoDAO extends BaseDAO<Departamento> {

    @Override
    protected String tabla() {
        return "departamento";
    }

    @Override
    protected String secuencia() {
        return "seq_departamento";
    }

    @Override
    protected Departamento mapear(ResultSet rs) throws SQLException {
        Departamento e = new Departamento();
        mapearAuditoria(rs, e);
        e.setNombre(rs.getString("nombre"));
        e.setRegionId(getLong(rs, "region_id"));
        return e;
    }

    @Override
    public Departamento insertar(Connection c, Departamento e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO departamento (id, nombre, region_id) "
                + "VALUES (?, ?, ?)";
        ejecutar(c, sql, id, e.getNombre(), e.getRegionId());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Departamento e) throws SQLException {
        String sql = "UPDATE departamento SET nombre = ?, region_id = ? WHERE id = ?";
        return ejecutar(c, sql, e.getNombre(), e.getRegionId(), e.getId()) > 0;
    }

    public List<Departamento> listarPorRegion(Connection c, long regionId) throws SQLException {
        return consultar(c, "SELECT * FROM departamento WHERE region_id = ? ORDER BY nombre", regionId);
    }
}
