package com.udi.dao.viviendas;

import com.udi.dao.BaseDAO;
import com.udi.model.Vivienda;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla vivienda. Las consultas traen además los nombres de la
 * ubicación (municipio, departamento y región) y el total de hogares
 * registrados, que no se guardan en la tabla.
 */
public class ViviendaDAO extends BaseDAO<Vivienda> {

    private static final String SELECT = "SELECT v.*, m.nombre AS municipio_nombre, "
            + "d.id AS departamento_id, d.nombre AS departamento_nombre, "
            + "r.id AS region_id, r.nombre AS region_nombre, "
            + "(SELECT COUNT(*) FROM hogar h WHERE h.vivienda_id = v.id) AS total_hogares "
            + "FROM vivienda v "
            + "JOIN municipio m ON m.id = v.municipio_id "
            + "JOIN departamento d ON d.id = m.departamento_id "
            + "JOIN region r ON r.id = d.region_id ";

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
        e.setBarrio(rs.getString("barrio"));
        e.setDireccion(rs.getString("direccion"));
        e.setTelefono(rs.getString("telefono"));
        e.setMunicipioNombre(rs.getString("municipio_nombre"));
        e.setDepartamentoId(getLong(rs, "departamento_id"));
        e.setDepartamentoNombre(rs.getString("departamento_nombre"));
        e.setRegionId(getLong(rs, "region_id"));
        e.setRegionNombre(rs.getString("region_nombre"));
        e.setTotalHogares(rs.getInt("total_hogares"));
        return e;
    }

    @Override
    public Optional<Vivienda> buscarPorId(Connection c, long id) throws SQLException {
        return consultarUno(c, SELECT + "WHERE v.id = ?", id);
    }

    @Override
    public List<Vivienda> listar(Connection c) throws SQLException {
        return consultar(c, SELECT + "ORDER BY v.id");
    }

    @Override
    public Vivienda insertar(Connection c, Vivienda e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO vivienda (id, municipio_id, clase, sector, seccion, manzana, segmento, "
                + "edificacion_numero, vivienda_numero, barrio, direccion, telefono, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getMunicipioId(), e.getClase(), e.getSector(), e.getSeccion(), e.getManzana(),
                e.getSegmento(), e.getEdificacionNumero(), e.getViviendaNumero(),
                e.getBarrio(), e.getDireccion(), e.getTelefono(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Vivienda e) throws SQLException {
        String sql = "UPDATE vivienda SET municipio_id = ?, clase = ?, sector = ?, seccion = ?, manzana = ?, "
                + "segmento = ?, edificacion_numero = ?, vivienda_numero = ?, barrio = ?, "
                + "direccion = ?, telefono = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getMunicipioId(), e.getClase(), e.getSector(), e.getSeccion(),
                e.getManzana(), e.getSegmento(), e.getEdificacionNumero(), e.getViviendaNumero(),
                e.getBarrio(), e.getDireccion(), e.getTelefono(), e.getId()) > 0;
    }

    public List<Vivienda> listarPorMunicipio(Connection c, long municipioId) throws SQLException {
        return consultar(c, SELECT + "WHERE v.municipio_id = ? ORDER BY v.direccion", municipioId);
    }

    /** Viviendas de la ubicación indicada; cada filtro en null no se aplica. */
    public List<Vivienda> filtrar(Connection c, Long regionId, Long departamentoId, Long municipioId)
            throws SQLException {
        return consultar(c, SELECT + "WHERE (? IS NULL OR r.id = ?) AND (? IS NULL OR d.id = ?) "
                        + "AND (? IS NULL OR v.municipio_id = ?) ORDER BY v.id",
                regionId, regionId, departamentoId, departamentoId, municipioId, municipioId);
    }
}
