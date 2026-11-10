package com.udi.dao.catalogos;

import com.udi.dao.BaseDAO;
import com.udi.model.Pagina;
import com.udi.model.Region;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla region. */
public class RegionDAO extends BaseDAO<Region> {

    @Override
    protected String tabla() {
        return "region";
    }

    @Override
    protected String secuencia() {
        return "seq_region";
    }

    @Override
    protected Region mapear(ResultSet rs) throws SQLException {
        Region e = new Region();
        mapearAuditoria(rs, e);
        e.setNombre(rs.getString("nombre"));
        return e;
    }

    @Override
    public Region insertar(Connection c, Region e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO region (id, nombre) "
                + "VALUES (?, ?)";
        ejecutar(c, sql, id, e.getNombre());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Region e) throws SQLException {
        String sql = "UPDATE region SET nombre = ? WHERE id = ?";
        return ejecutar(c, sql, e.getNombre(), e.getId()) > 0;
    }

    /** Regiones cuyo nombre contiene {@code texto}, ordenadas por nombre. */
    public Pagina<Region> buscar(Connection c, String texto, int desde, int limite) throws SQLException {
        return consultarPagina(c, "SELECT * FROM region WHERE " + sinTildes("nombre") + " LIKE ? ORDER BY nombre, id",
                desde, limite, patron(texto));
    }
}
