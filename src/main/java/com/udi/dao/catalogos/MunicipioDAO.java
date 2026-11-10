package com.udi.dao.catalogos;

import com.udi.dao.BaseDAO;
import com.udi.model.Municipio;
import com.udi.model.Pagina;

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

    /**
     * Municipios cuyo nombre contiene {@code texto}. {@code departamentoId} y
     * {@code regionId} en null no filtran.
     */
    public Pagina<Municipio> buscar(Connection c, Long regionId, Long departamentoId, String texto, int desde, int limite)
            throws SQLException {
        return consultarPagina(c, "SELECT m.* FROM municipio m JOIN departamento d ON d.id = m.departamento_id "
                        + "WHERE " + sinTildes("m.nombre") + " LIKE ? AND (? IS NULL OR m.departamento_id = ?) "
                        + "AND (? IS NULL OR d.region_id = ?) ORDER BY m.nombre, m.id",
                desde, limite, patron(texto), departamentoId, departamentoId, regionId, regionId);
    }
}
