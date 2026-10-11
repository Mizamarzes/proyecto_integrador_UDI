package com.udi.dao;

import com.udi.model.Hogar;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla hogar. */
public class HogarDAO extends BaseDAO<Hogar> {

    @Override
    protected String tabla() {
        return "hogar";
    }

    @Override
    protected String secuencia() {
        return "seq_hogar";
    }

    @Override
    protected Hogar mapear(ResultSet rs) throws SQLException {
        Hogar e = new Hogar();
        mapearAuditoria(rs, e);
        e.setViviendaId(getLong(rs, "vivienda_id"));
        e.setHogarNumero(getInteger(rs, "hogar_numero"));
        e.setTotalPersonas(getInteger(rs, "total_personas"));
        e.setCiclo(rs.getString("ciclo"));
        return e;
    }

    @Override
    public Hogar insertar(Connection c, Hogar e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO hogar (id, vivienda_id, hogar_numero, total_personas, ciclo, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getViviendaId(), e.getHogarNumero(), e.getTotalPersonas(), e.getCiclo(),
                e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Hogar e) throws SQLException {
        String sql = "UPDATE hogar SET vivienda_id = ?, hogar_numero = ?, total_personas = ?, ciclo = ?, "
                + "updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getViviendaId(), e.getHogarNumero(), e.getTotalPersonas(), e.getCiclo(),
                e.getId()) > 0;
    }

    public List<Hogar> listarPorVivienda(Connection c, long viviendaId) throws SQLException {
        return consultar(c, "SELECT * FROM hogar WHERE vivienda_id = ? ORDER BY hogar_numero", viviendaId);
    }

    public long contarPorVivienda(Connection c, long viviendaId) throws SQLException {
        return consultarNumero(c, "SELECT COUNT(*) FROM hogar WHERE vivienda_id = ?", viviendaId);
    }

    public Optional<Hogar> buscarPorViviendaYNumero(Connection c, long viviendaId, int hogarNumero) throws SQLException {
        return consultarUno(c, "SELECT * FROM hogar WHERE vivienda_id = ? AND hogar_numero = ?", viviendaId, hogarNumero);
    }
}
