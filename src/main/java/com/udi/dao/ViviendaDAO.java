package com.udi.dao;

import com.udi.model.Vivienda;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Acceso a la tabla vivienda. */
public class ViviendaDAO extends BaseDAO<Vivienda> {

    @Override
    protected String tabla() {
        return "vivienda";
    }

    @Override
    protected String secuencia() {
        return "seq_vivienda";
    }

    @Override
    protected Vivienda mapear(ResultSet rs) throws SQLException {
        Vivienda e = new Vivienda();
        mapearAuditoria(rs, e);
        e.setMunicipioId(getLong(rs, "municipio_id"));
        e.setClase(getInteger(rs, "clase"));
        e.setSector(rs.getString("sector"));
        e.setSeccion(rs.getString("seccion"));
        e.setManzana(rs.getString("manzana"));
        e.setSegmento(rs.getString("segmento"));
        e.setEdificacionNumero(getInteger(rs, "edificacion_numero"));
        e.setViviendaNumero(getInteger(rs, "vivienda_numero"));
        e.setTotalHogares(getInteger(rs, "total_hogares"));
        e.setBarrio(rs.getString("barrio"));
        e.setDireccion(rs.getString("direccion"));
        e.setTelefono(rs.getString("telefono"));
        return e;
    }

    @Override
    public Vivienda insertar(Connection c, Vivienda e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, "
                + "edificacion_numero, vivienda_numero, total_hogares, barrio, direccion, telefono, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getMunicipioId(), e.getClase(), e.getSector(), e.getSeccion(), e.getManzana(),
                e.getSegmento(), e.getEdificacionNumero(), e.getViviendaNumero(), e.getTotalHogares(),
                e.getBarrio(), e.getDireccion(), e.getTelefono(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Vivienda e) throws SQLException {
        String sql = "UPDATE vivienda SET municipio_id = ?, clase = ?, sector = ?, seccion = ?, manzana = ?, "
                + "segmento = ?, edificacion_numero = ?, vivienda_numero = ?, total_hogares = ?, barrio = ?, "
                + "direccion = ?, telefono = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getMunicipioId(), e.getClase(), e.getSector(), e.getSeccion(),
                e.getManzana(), e.getSegmento(), e.getEdificacionNumero(), e.getViviendaNumero(),
                e.getTotalHogares(), e.getBarrio(), e.getDireccion(), e.getTelefono(), e.getId()) > 0;
    }

    public List<Vivienda> listarPorMunicipio(Connection c, long municipioId) throws SQLException {
        return consultar(c, "SELECT * FROM vivienda WHERE municipio_id = ? ORDER BY direccion", municipioId);
    }
}
