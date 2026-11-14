package com.udi.dao.verificacion;

import com.udi.dao.BaseDAO;
import com.udi.model.EncuestaAprobada;
import oracle.jdbc.OracleTypes;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Exportación de las encuestas aprobadas (RF-10). No corresponde a una tabla,
 * así que no extiende {@link BaseDAO}: llama al procedimiento
 * {@code pr_exportar_aprobadas} (docs/db/plsql/procedimientos.sql), que
 * devuelve un SYS_REFCURSOR con una fila por encuesta APROBADA.
 */
public class ExportacionDAO {

    /**
     * Encuestas aprobadas de la ubicación indicada (cada filtro es opcional:
     * null = todas), ordenadas por región, departamento, municipio y código.
     */
    public List<EncuestaAprobada> listarAprobadas(Connection c, Long regionId, Long departamentoId, Long municipioId)
            throws SQLException {
        try (CallableStatement cs = c.prepareCall("{call pr_exportar_aprobadas(?, ?, ?, ?)}")) {
            BaseDAO.asignarParametros(cs, regionId, departamentoId, municipioId);
            // ojdbc6 es JDBC 4.0: no conoce Types.REF_CURSOR, el cursor se registra con el tipo de Oracle
            cs.registerOutParameter(4, OracleTypes.CURSOR);
            cs.execute();
            List<EncuestaAprobada> lista = new ArrayList<>();
            try (ResultSet rs = (ResultSet) cs.getObject(4)) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        }
    }

    private static EncuestaAprobada mapear(ResultSet rs) throws SQLException {
        EncuestaAprobada a = new EncuestaAprobada();
        a.setEncuestaId(rs.getLong("encuesta_id"));
        a.setCodigo(rs.getString("codigo"));
        a.setHogarId(rs.getLong("hogar_id"));
        a.setHogarNumero(entero(rs, "hogar_numero"));
        a.setTotalPersonas(entero(rs, "total_personas"));
        a.setRegion(rs.getString("region"));
        a.setDepartamento(rs.getString("departamento"));
        a.setMunicipio(rs.getString("municipio"));
        a.setBarrio(rs.getString("barrio"));
        a.setDireccion(rs.getString("direccion"));
        a.setEncuestador(rs.getString("encuestador"));
        a.setInformante(rs.getString("informante_nombre"));
        a.setFechaDesde(fecha(rs, "fecha_desde"));
        a.setFechaHasta(fecha(rs, "fecha_hasta"));
        a.setFechaCierre(fechaHora(rs, "fecha_cierre"));
        a.setAprobadaPor(rs.getString("aprobada_por"));
        a.setFechaAprobacion(fechaHora(rs, "fecha_aprobacion"));
        a.setVisitas(rs.getLong("visitas"));
        a.setDias(rs.getLong("dias"));
        a.setGastoTotal(rs.getBigDecimal("gasto_total"));
        return a;
    }

    private static Integer entero(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    private static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        Date valor = rs.getDate(columna);
        return valor == null ? null : valor.toLocalDate();
    }

    private static LocalDateTime fechaHora(ResultSet rs, String columna) throws SQLException {
        Timestamp valor = rs.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }
}
