package com.udi.dao.encuestas;

import com.udi.dao.BaseDAO;
import com.udi.model.Encuesta;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.model.enums.FormaPago;
import com.udi.model.enums.ResultadoEntrevista;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla encuesta. Los cambios de estado del caso (cerrar,
 * reabrir, rechazar y aprobar) se hacen con procedimientos almacenados, que
 * validan el estado actual y registran la revisión en auditoria_encuesta.
 */
public class EncuestaDAO extends BaseDAO<Encuesta> {

    @Override
    protected String tabla() {
        return "encuesta";
    }

    @Override
    protected String secuencia() {
        return "seq_encuesta";
    }

    @Override
    protected Encuesta mapear(ResultSet rs) throws SQLException {
        Encuesta e = new Encuesta();
        mapearAuditoria(rs, e);
        e.setHogarId(getLong(rs, "hogar_id"));
        e.setEncuestadorId(getLong(rs, "encuestador_id"));
        e.setInformanteNombre(rs.getString("informante_nombre"));
        e.setInformanteOrden(getInteger(rs, "informante_orden"));
        e.setResultadoCuadernillo(getEnum(rs, "resultado_cuadernillo", ResultadoEntrevista.class));
        e.setFechaDesde(getLocalDate(rs, "fecha_desde"));
        e.setFechaHasta(getLocalDate(rs, "fecha_hasta"));
        e.setGastoFiestas(getBoolean(rs, "gasto_fiestas"));
        e.setFormaPago(getEnum(rs, "forma_pago", FormaPago.class));
        e.setFormaPagoOtra(rs.getString("forma_pago_otra"));
        e.setObservaciones(rs.getString("observaciones"));
        e.setEstado(getEnum(rs, "estado", EstadoEncuesta.class));
        e.setFechaCierre(getLocalDateTime(rs, "fecha_cierre"));
        e.setCerradaPor(getLong(rs, "cerrada_por"));
        return e;
    }

    @Override
    public Encuesta insertar(Connection c, Encuesta e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO encuesta (id, hogar_id, encuestador_id, informante_nombre, "
                + "informante_orden, resultado_cuadernillo, fecha_desde, fecha_hasta, gasto_fiestas, "
                + "forma_pago, forma_pago_otra, observaciones, estado, fecha_cierre, cerrada_por, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getHogarId(), e.getEncuestadorId(), e.getInformanteNombre(),
                e.getInformanteOrden(), e.getResultadoCuadernillo(), e.getFechaDesde(), e.getFechaHasta(),
                e.getGastoFiestas(), e.getFormaPago(), e.getFormaPagoOtra(), e.getObservaciones(),
                e.getEstado(), e.getFechaCierre(), e.getCerradaPor(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, Encuesta e) throws SQLException {
        String sql = "UPDATE encuesta SET hogar_id = ?, encuestador_id = ?, informante_nombre = ?, "
                + "informante_orden = ?, resultado_cuadernillo = ?, fecha_desde = ?, fecha_hasta = ?, "
                + "gasto_fiestas = ?, forma_pago = ?, forma_pago_otra = ?, observaciones = ?, "
                + "updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getHogarId(), e.getEncuestadorId(), e.getInformanteNombre(),
                e.getInformanteOrden(), e.getResultadoCuadernillo(), e.getFechaDesde(), e.getFechaHasta(),
                e.getGastoFiestas(), e.getFormaPago(), e.getFormaPagoOtra(), e.getObservaciones(), e.getId()) > 0;
    }

    public Optional<Encuesta> buscarPorHogar(Connection c, long hogarId) throws SQLException {
        return consultarUno(c, "SELECT * FROM encuesta WHERE hogar_id = ?", hogarId);
    }

    public List<Encuesta> listarPorEncuestador(Connection c, long encuestadorId) throws SQLException {
        return consultar(c, "SELECT * FROM encuesta WHERE encuestador_id = ? ORDER BY fecha_desde DESC", encuestadorId);
    }

    public List<Encuesta> listarPorEstado(Connection c, EstadoEncuesta estado) throws SQLException {
        return consultar(c, "SELECT * FROM encuesta WHERE estado = ? ORDER BY fecha_desde DESC", estado);
    }

    /** Encuestas en cualquiera de los estados, de la más reciente a la más antigua. */
    public List<Encuesta> listarPorEstados(Connection c, EstadoEncuesta... estados) throws SQLException {
        String marcas = String.join(", ", Collections.nCopies(estados.length, "?"));
        return consultar(c, "SELECT * FROM encuesta WHERE estado IN (" + marcas + ") ORDER BY fecha_desde DESC",
                (Object[]) estados);
    }

    /** Cierra el caso: pasa a PENDIENTE_APROBACION (RF-06). Procedimiento pr_cerrar_encuesta. */
    public void cerrar(Connection c, long id, long usuarioId, LocalDateTime fechaCierre) throws SQLException {
        llamar(c, "pr_cerrar_encuesta", id, usuarioId, fechaCierre);
    }

    /** Reabre un caso pendiente o aprobado: vuelve a EN_CURSO. Procedimiento pr_reabrir_encuesta. */
    public void reabrir(Connection c, long id) throws SQLException {
        llamar(c, "pr_reabrir_encuesta", id);
    }

    /**
     * Rechaza un caso pendiente con la observación para el encuestador y la
     * registra en auditoria_encuesta (RF-10). Procedimiento pr_rechazar_encuesta.
     */
    public void rechazar(Connection c, long id, String observacion, long usuarioId) throws SQLException {
        llamar(c, "pr_rechazar_encuesta", id, observacion, usuarioId);
    }

    /** Aprueba un caso pendiente y lo registra en auditoria_encuesta (RF-10). Procedimiento pr_aprobar_encuesta. */
    public void aprobar(Connection c, long id, long usuarioId) throws SQLException {
        llamar(c, "pr_aprobar_encuesta", id, usuarioId);
    }
}
