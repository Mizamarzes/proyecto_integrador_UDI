package com.udi.dao;

import com.udi.model.Entidad;
import com.udi.model.EntidadAuditable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Operaciones comunes de todos los DAO: buscar por id, listar, eliminar y
 * contar, más utilidades para ejecutar SQL y convertir tipos entre Java y Oracle.
 *
 * Cada DAO concreto define su tabla, su secuencia, cómo convertir una fila en
 * objeto ({@link #mapear}) y sus sentencias INSERT y UPDATE.
 *
 * Todos los métodos reciben la {@link Connection}: así el servicio puede
 * agrupar varias operaciones en una misma transacción.
 *
 * Conversión de tipos: LocalDate - DATE, LocalDateTime - TIMESTAMP,
 * Boolean - CHAR(1) 'S'/'N', enum - VARCHAR2 con el nombre del enum.
 */
public abstract class BaseDAO<T extends Entidad> {

    /** Nombre de la tabla en Oracle. */
    protected abstract String tabla();

    /** Nombre de la secuencia que genera el id. */
    protected abstract String secuencia();

    /** Convierte la fila actual del ResultSet en un objeto. */
    protected abstract T mapear(ResultSet rs) throws SQLException;

    /** Inserta el objeto, le asigna el id generado y lo devuelve. */
    public abstract T insertar(Connection c, T entidad) throws SQLException;

    /** Actualiza el objeto por su id. Devuelve false si no existe. */
    public abstract boolean actualizar(Connection c, T entidad) throws SQLException;

    public Optional<T> buscarPorId(Connection c, long id) throws SQLException {
        return consultarUno(c, "SELECT * FROM " + tabla() + " WHERE id = ?", id);
    }

    public List<T> listar(Connection c) throws SQLException {
        return consultar(c, "SELECT * FROM " + tabla() + " ORDER BY id");
    }

    public boolean eliminar(Connection c, long id) throws SQLException {
        return ejecutar(c, "DELETE FROM " + tabla() + " WHERE id = ?", id) > 0;
    }

    public long contar(Connection c) throws SQLException {
        return consultarNumero(c, "SELECT COUNT(*) FROM " + tabla());
    }

    public boolean existe(Connection c, long id) throws SQLException {
        return consultarNumero(c, "SELECT COUNT(*) FROM " + tabla() + " WHERE id = ?", id) > 0;
    }

    // ------------------------------------------------------------------
    // Utilidades para los DAO concretos
    // ------------------------------------------------------------------

    /**
     * Devuelve el id de la entidad si ya lo trae (por ejemplo, catálogos que
     * usan el código del cuadernillo como id) o el siguiente de la secuencia.
     */
    protected long asignarId(Connection c, T entidad) throws SQLException {
        if (entidad.getId() == null) {
            entidad.setId(consultarNumero(c, "SELECT " + secuencia() + ".NEXTVAL FROM dual"));
        }
        return entidad.getId();
    }

    protected List<T> consultar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            asignarParametros(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    protected Optional<T> consultarUno(Connection c, String sql, Object... parametros) throws SQLException {
        List<T> resultado = consultar(c, sql, parametros);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    /** Para consultas que devuelven un solo número: COUNT, MAX, NEXTVAL. Null se toma como 0. */
    protected long consultarNumero(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            asignarParametros(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /** Ejecuta un INSERT, UPDATE o DELETE y devuelve las filas afectadas. */
    protected int ejecutar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            asignarParametros(ps, parametros);
            return ps.executeUpdate();
        }
    }

    private static void asignarParametros(PreparedStatement ps, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            int indice = i + 1;
            Object valor = parametros[i];
            if (valor == null) {
                ps.setNull(indice, Types.VARCHAR);
            } else if (valor instanceof String) {
                ps.setString(indice, (String) valor);
            } else if (valor instanceof Long) {
                ps.setLong(indice, (Long) valor);
            } else if (valor instanceof Integer) {
                ps.setInt(indice, (Integer) valor);
            } else if (valor instanceof BigDecimal) {
                ps.setBigDecimal(indice, (BigDecimal) valor);
            } else if (valor instanceof Boolean) {
                ps.setString(indice, (Boolean) valor ? "S" : "N");
            } else if (valor instanceof LocalDate) {
                ps.setDate(indice, Date.valueOf((LocalDate) valor));
            } else if (valor instanceof LocalDateTime) {
                ps.setTimestamp(indice, Timestamp.valueOf((LocalDateTime) valor));
            } else if (valor instanceof Enum) {
                ps.setString(indice, ((Enum<?>) valor).name());
            } else {
                ps.setObject(indice, valor);
            }
        }
    }

    /** Llena created_at y, si la entidad es auditable, updated_at y created_by. */
    protected static void mapearAuditoria(ResultSet rs, Entidad entidad) throws SQLException {
        entidad.setId(rs.getLong("id"));
        entidad.setCreatedAt(getLocalDateTime(rs, "created_at"));
        if (entidad instanceof EntidadAuditable) {
            EntidadAuditable auditable = (EntidadAuditable) entidad;
            auditable.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
            auditable.setCreatedBy(getLong(rs, "created_by"));
        }
    }

    protected static Long getLong(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    protected static Integer getInteger(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    protected static BigDecimal getBigDecimal(ResultSet rs, String columna) throws SQLException {
        return rs.getBigDecimal(columna);
    }

    protected static LocalDate getLocalDate(ResultSet rs, String columna) throws SQLException {
        Date valor = rs.getDate(columna);
        return valor == null ? null : valor.toLocalDate();
    }

    protected static LocalDateTime getLocalDateTime(ResultSet rs, String columna) throws SQLException {
        Timestamp valor = rs.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    /** CHAR(1) 'S'/'N' a Boolean. Null se mantiene como null. */
    protected static Boolean getBoolean(ResultSet rs, String columna) throws SQLException {
        String valor = rs.getString(columna);
        return valor == null ? null : "S".equals(valor);
    }

    protected static <E extends Enum<E>> E getEnum(ResultSet rs, String columna, Class<E> tipo) throws SQLException {
        String valor = rs.getString(columna);
        return valor == null ? null : Enum.valueOf(tipo, valor);
    }
}
