package com.udi.dao.reportes;

import com.udi.dao.BaseDAO;
import com.udi.model.FiltroReporte;
import com.udi.model.ReporteEncuestador;
import com.udi.model.ValorReporte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas agregadas del módulo de reportes. No corresponde a una tabla, así
 * que no extiende {@link BaseDAO}: solo lee, agrupando encuestas, visitas y
 * renglones de los capítulos C, D y D1.
 *
 * Todas parten de las encuestas que cumplen los filtros (fecha de inicio,
 * ubicación de la vivienda y, para un encuestador, sus propias encuestas). El gasto de una encuesta es la suma de sus gastos
 * diarios (D) y de sus comidas fuera del hogar (D1).
 */
public class ReporteDAO {

    /** Encuesta con la ubicación de su vivienda; los filtros usan los alias e, v, m y d. */
    private static final String ENCUESTAS = " FROM encuesta e"
            + " JOIN hogar h ON h.id = e.hogar_id"
            + " JOIN vivienda v ON v.id = h.vivienda_id"
            + " JOIN municipio m ON m.id = v.municipio_id"
            + " JOIN departamento d ON d.id = m.departamento_id";

    @FunctionalInterface
    private interface Mapeo<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    /** Encuestas, visitas y gasto de cada encuestador, ordenados por nombre. */
    public List<ReporteEncuestador> porEncuestador(Connection c, FiltroReporte f) throws SQLException {
        Condicion w = condicion(f);
        String sql = "SELECT x.encuestador_id, u.nombre, u.apellido, COUNT(*) total,"
                + " SUM(CASE WHEN x.estado = 'ABIERTA' THEN 1 ELSE 0 END) abiertas,"
                // Aprobada cuenta como cerrada y en corrección como reabierta (vuelve a admitir cambios)
                + " SUM(CASE WHEN x.estado IN ('CERRADA', 'APROBADA') THEN 1 ELSE 0 END) cerradas,"
                + " SUM(CASE WHEN x.estado IN ('REABIERTA', 'EN_CORRECCION') THEN 1 ELSE 0 END) reabiertas,"
                + " SUM(x.visitas) visitas, SUM(x.gasto) gasto"
                + " FROM (SELECT e.encuestador_id, e.estado,"
                + "   (SELECT COUNT(*) FROM visita vi WHERE vi.hogar_id = e.hogar_id) visitas,"
                + "   (SELECT NVL(SUM(g.valor), 0) FROM gasto_diario g"
                + "      JOIN registro_diario r ON r.id = g.registro_diario_id WHERE r.encuesta_id = e.id)"
                + "   + (SELECT NVL(SUM(cf.valor), 0) FROM comida_fuera_hogar cf"
                + "      JOIN registro_diario r ON r.id = cf.registro_diario_id WHERE r.encuesta_id = e.id) gasto"
                + ENCUESTAS + w.sql + ") x"
                + " JOIN encuestador en ON en.id = x.encuestador_id"
                + " JOIN usuario u ON u.id = en.usuario_id"
                + " GROUP BY x.encuestador_id, u.nombre, u.apellido"
                + " ORDER BY u.nombre, u.apellido";
        return consultar(c, sql, w.parametros, rs -> new ReporteEncuestador(
                rs.getLong("encuestador_id"),
                rs.getString("nombre") + " " + rs.getString("apellido"),
                rs.getLong("total"), rs.getLong("abiertas"), rs.getLong("cerradas"), rs.getLong("reabiertas"),
                rs.getLong("visitas"), rs.getBigDecimal("gasto")));
    }

    /** Gasto por forma de adquisición; el código es el nombre del enum (COMPRA, REGALO…). */
    public List<ValorReporte> gastoPorForma(Connection c, FiltroReporte f) throws SQLException {
        return agruparRenglones(c, f, "x.forma_adquisicion", "x.forma_adquisicion", "", "x.forma_adquisicion");
    }

    /** Gasto por tipo (HOGAR o PERSONAL). */
    public List<ValorReporte> gastoPorTipo(Connection c, FiltroReporte f) throws SQLException {
        return agruparRenglones(c, f, "x.tipo_gasto", "x.tipo_gasto", "", "x.tipo_gasto");
    }

    /** Gasto de lo comprado por lugar de compra, de mayor a menor. */
    public List<ValorReporte> gastoPorLugar(Connection c, FiltroReporte f) throws SQLException {
        return agruparRenglones(c, f, "TO_CHAR(l.id)", "l.nombre",
                " JOIN lugar_compra l ON l.id = x.lugar_compra_id", "l.id, l.nombre");
    }

    /** Renglones del capítulo D, del D1 y comidas a domicilio, en ese orden. */
    public long[] contarRenglones(Connection c, FiltroReporte f) throws SQLException {
        Condicion w = condicion(f);
        String sql = "SELECT NVL(SUM(CASE WHEN x.origen = 'D' THEN 1 ELSE 0 END), 0) articulos,"
                + " NVL(SUM(CASE WHEN x.origen = 'D1' THEN 1 ELSE 0 END), 0) comidas,"
                + " NVL(SUM(CASE WHEN x.a_domicilio = 'S' THEN 1 ELSE 0 END), 0) domicilios"
                + " FROM (" + renglones(w) + ") x";
        List<long[]> filas = consultar(c, sql, w.dosVeces(), rs ->
                new long[]{rs.getLong("articulos"), rs.getLong("comidas"), rs.getLong("domicilios")});
        return filas.isEmpty() ? new long[3] : filas.get(0);
    }

    /**
     * Capítulo C: cuántos hogares compran cada grupo de alimento (frecuencia
     * distinta de «Nunca»), de mayor a menor. El código es el del cuadernillo.
     */
    public List<ValorReporte> hogaresPorGrupo(Connection c, FiltroReporte f) throws SQLException {
        Condicion w = condicion(f);
        String sql = "SELECT ga.id, ga.nombre, COUNT(*) hogares"
                + " FROM caracteristica_gasto cg JOIN grupo_alimento ga ON ga.id = cg.grupo_alimento_id"
                + " WHERE cg.frecuencia_compra <> 'NUNCA'"
                + " AND cg.encuesta_id IN (SELECT e.id" + ENCUESTAS + w.sql + ")"
                + " GROUP BY ga.id, ga.nombre ORDER BY hogares DESC, ga.id";
        return consultar(c, sql, w.parametros, rs -> new ValorReporte(
                String.format("%02d", rs.getLong("id")), rs.getString("nombre"), rs.getBigDecimal("hogares")));
    }

    // ------------------------------------------------------------------

    /**
     * Agrupa los renglones de D y D1 de las encuestas filtradas y suma su valor.
     * {@code union} permite unir un catálogo (para el nombre del lugar).
     */
    private List<ValorReporte> agruparRenglones(Connection c, FiltroReporte f, String codigo, String nombre,
                                                String union, String agrupar) throws SQLException {
        Condicion w = condicion(f);
        String sql = "SELECT " + codigo + " codigo, " + nombre + " nombre, SUM(x.valor) valor"
                + " FROM (" + renglones(w) + ") x" + union
                + " GROUP BY " + agrupar + " ORDER BY valor DESC";
        return consultar(c, sql, w.dosVeces(), rs ->
                new ValorReporte(rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("valor")));
    }

    /** Renglones de gastos diarios (D) y comidas fuera del hogar (D1) de las encuestas filtradas. */
    private static String renglones(Condicion w) {
        String encuestas = " WHERE r.encuesta_id IN (SELECT e.id" + ENCUESTAS + w.sql + ")";
        return "SELECT g.forma_adquisicion, g.lugar_compra_id, g.tipo_gasto, g.valor, 'N' a_domicilio, 'D' origen"
                + " FROM gasto_diario g JOIN registro_diario r ON r.id = g.registro_diario_id" + encuestas
                + " UNION ALL"
                + " SELECT cf.forma_adquisicion, cf.lugar_compra_id, cf.tipo_gasto, cf.valor, cf.a_domicilio, 'D1' origen"
                + " FROM comida_fuera_hogar cf JOIN registro_diario r ON r.id = cf.registro_diario_id" + encuestas;
    }

    /** WHERE de las encuestas según los filtros que vienen con valor. */
    private static Condicion condicion(FiltroReporte f) {
        Condicion w = new Condicion();
        if (f.getDesde() != null) {
            w.y("e.fecha_desde >= ?", f.getDesde());
        }
        if (f.getHasta() != null) {
            // Menor que el día siguiente: incluye todo el día «hasta» aunque la fecha traiga hora
            w.y("e.fecha_desde < ?", f.getHasta().plusDays(1));
        }
        if (f.getRegionId() != null) {
            w.y("d.region_id = ?", f.getRegionId());
        }
        if (f.getDepartamentoId() != null) {
            w.y("m.departamento_id = ?", f.getDepartamentoId());
        }
        if (f.getMunicipioId() != null) {
            w.y("v.municipio_id = ?", f.getMunicipioId());
        }
        if (f.getEncuestadorId() != null) {
            w.y("e.encuestador_id = ?", f.getEncuestadorId());
        }
        return w;
    }

    private static <T> List<T> consultar(Connection c, String sql, List<Object> parametros, Mapeo<T> mapeo)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            BaseDAO.asignarParametros(ps, parametros.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                List<T> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapeo.mapear(rs));
                }
                return resultado;
            }
        }
    }

    /** Condiciones unidas con AND y sus parámetros en orden. */
    private static final class Condicion {
        private String sql = "";
        private final List<Object> parametros = new ArrayList<>();

        private void y(String condicion, Object parametro) {
            sql += (sql.isEmpty() ? " WHERE " : " AND ") + condicion;
            parametros.add(parametro);
        }

        /** Los renglones repiten la condición en las dos partes del UNION ALL. */
        private List<Object> dosVeces() {
            List<Object> todos = new ArrayList<>(parametros);
            todos.addAll(parametros);
            return todos;
        }
    }
}
