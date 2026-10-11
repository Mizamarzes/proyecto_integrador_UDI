package com.udi.dao;

import com.udi.model.LugarCompra;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla lugar_compra (catálogo). */
public class LugarCompraDAO extends BaseDAO<LugarCompra> {

    @Override
    protected String tabla() {
        return "lugar_compra";
    }

    @Override
    protected String secuencia() {
        return "seq_lugar_compra";
    }

    @Override
    protected LugarCompra mapear(ResultSet rs) throws SQLException {
        LugarCompra e = new LugarCompra();
        mapearAuditoria(rs, e);
        e.setNombre(rs.getString("nombre"));
        return e;
    }

    @Override
    public LugarCompra insertar(Connection c, LugarCompra e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO lugar_compra (id, nombre, created_by) "
                + "VALUES (?, ?, ?)";
        ejecutar(c, sql, id, e.getNombre(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, LugarCompra e) throws SQLException {
        String sql = "UPDATE lugar_compra SET nombre = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getNombre(), e.getId()) > 0;
    }
}
