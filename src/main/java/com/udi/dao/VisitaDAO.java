package com.udi.dao;

import com.udi.model.Visita;
import com.udi.model.enums.ResultadoEntrevista;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Acceso a la tabla visita (capítulo B). */
public class VisitaDAO extends BaseDAO<Visita> {

    @Override
    protected String tabla() {
        return "visita";
    }

    @Override
    protected String secuencia() {
        return "seq_visita";
    }

    @Override
    protected Visita mapear(ResultSet rs) throws SQLException {
        Visita e = new Visita();
        mapearAuditoria(rs, e);
        e.setHogarId(getLong(rs, "hogar_id"));
        e.setNumeroVisita(getInteger(rs, "numero_visita"));
        e.setFecha(getLocalDate(rs, "fecha"));
        e.setHoraInicio(getLocalDateTime(rs, "hora_inicio"));
        e.setHoraFin(getLocalDateTime(rs, "hora_fin"));
        e.setResultadoVisita(getEnum(rs, "resultado_visita", ResultadoEntrevista.class));
        e.setPartesPendientes(rs.getString("partes_pendientes"));
        e.setFechaProxima(getLocalDate(rs, "fecha_proxima"));
        e.setHoraProxima(getLocalDateTime(rs, "hora_proxima"));
        return e;
    }

    @Override
    public Visita insertar(Connection c, Visita e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO visita (id, hogar_id, numero_visita, fecha, hora_inicio, hora_fin, "
                + "resultado_visita, partes_pendientes, fecha_proxima, hora_proxima, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getHogarId(), e.getNumeroVisita(), e.getFecha(), e.getHoraInicio(),
                e.getHoraFin(), e.getResultadoVisita(), e.getPartesPendientes(), e.getFechaProxima(),
                e.getHoraProxima(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Visita e) throws SQLException {
        String sql = "UPDATE visita SET hogar_id = ?, numero_visita = ?, fecha = ?, hora_inicio = ?, "
                + "hora_fin = ?, resultado_visita = ?, partes_pendientes = ?, fecha_proxima = ?, "
                + "hora_proxima = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getHogarId(), e.getNumeroVisita(), e.getFecha(), e.getHoraInicio(),
                e.getHoraFin(), e.getResultadoVisita(), e.getPartesPendientes(), e.getFechaProxima(),
                e.getHoraProxima(), e.getId()) > 0;
    }

    public List<Visita> listarPorHogar(Connection c, long hogarId) throws SQLException {
        return consultar(c, "SELECT * FROM visita WHERE hogar_id = ? ORDER BY numero_visita", hogarId);
    }

    /** Número que le corresponde a la próxima visita del hogar. */
    public int siguienteNumero(Connection c, long hogarId) throws SQLException {
        return (int) consultarNumero(c, "SELECT NVL(MAX(numero_visita), 0) + 1 FROM visita WHERE hogar_id = ?", hogarId);
    }
}
