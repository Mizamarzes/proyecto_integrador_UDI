package com.udi.dao;

import com.udi.model.ComidaFueraHogar;
import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.model.enums.TipoComida;
import com.udi.model.enums.TipoGasto;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Acceso a la tabla comida_fuera_hogar (capítulo D1). */
public class ComidaFueraHogarDAO extends BaseDAO<ComidaFueraHogar> {

    @Override
    protected String tabla() {
        return "comida_fuera_hogar";
    }

    @Override
    protected String secuencia() {
        return "seq_comida_fuera_hogar";
    }

    @Override
    protected ComidaFueraHogar mapear(ResultSet rs) throws SQLException {
        ComidaFueraHogar e = new ComidaFueraHogar();
        mapearAuditoria(rs, e);
        e.setRegistroDiarioId(getLong(rs, "registro_diario_id"));
        e.setEncuestadorId(getLong(rs, "encuestador_id"));
        e.setLugarCompraId(getLong(rs, "lugar_compra_id"));
        e.setNumeroOrden(getInteger(rs, "numero_orden"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setTipoComida(getEnum(rs, "tipo_comida", TipoComida.class));
        e.setCantidad(getBigDecimal(rs, "cantidad"));
        e.setFormaAdquisicion(getEnum(rs, "forma_adquisicion", FormaAdquisicion.class));
        e.setFormaAdquisicionOtra(rs.getString("forma_adquisicion_otra"));
        e.setValor(getBigDecimal(rs, "valor"));
        e.setFrecuenciaCompra(getEnum(rs, "frecuencia_compra", FrecuenciaCompra.class));
        e.setTipoGasto(getEnum(rs, "tipo_gasto", TipoGasto.class));
        e.setADomicilio(getBoolean(rs, "a_domicilio"));
        return e;
    }

    @Override
    public ComidaFueraHogar insertar(Connection c, ComidaFueraHogar e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO comida_fuera_hogar (id, registro_diario_id, encuestador_id, "
                + "lugar_compra_id, numero_orden, descripcion, tipo_comida, cantidad, forma_adquisicion, "
                + "forma_adquisicion_otra, valor, frecuencia_compra, tipo_gasto, a_domicilio, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getRegistroDiarioId(), e.getEncuestadorId(), e.getLugarCompraId(),
                e.getNumeroOrden(), e.getDescripcion(), e.getTipoComida(), e.getCantidad(),
                e.getFormaAdquisicion(), e.getFormaAdquisicionOtra(), e.getValor(), e.getFrecuenciaCompra(),
                e.getTipoGasto(), e.isADomicilio(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, ComidaFueraHogar e) throws SQLException {
        String sql = "UPDATE comida_fuera_hogar SET registro_diario_id = ?, encuestador_id = ?, "
                + "lugar_compra_id = ?, numero_orden = ?, descripcion = ?, tipo_comida = ?, cantidad = ?, "
                + "forma_adquisicion = ?, forma_adquisicion_otra = ?, valor = ?, frecuencia_compra = ?, "
                + "tipo_gasto = ?, a_domicilio = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getRegistroDiarioId(), e.getEncuestadorId(), e.getLugarCompraId(),
                e.getNumeroOrden(), e.getDescripcion(), e.getTipoComida(), e.getCantidad(),
                e.getFormaAdquisicion(), e.getFormaAdquisicionOtra(), e.getValor(), e.getFrecuenciaCompra(),
                e.getTipoGasto(), e.isADomicilio(), e.getId()) > 0;
    }

    public List<ComidaFueraHogar> listarPorRegistro(Connection c, long registroDiarioId) throws SQLException {
        return consultar(c, "SELECT * FROM comida_fuera_hogar WHERE registro_diario_id = ? ORDER BY numero_orden",
                registroDiarioId);
    }

    public long contarPorRegistro(Connection c, long registroDiarioId) throws SQLException {
        return consultarNumero(c, "SELECT COUNT(*) FROM comida_fuera_hogar WHERE registro_diario_id = ?", registroDiarioId);
    }

    /** Número de orden que le corresponde al próximo renglón del día. */
    public int siguienteNumeroOrden(Connection c, long registroDiarioId) throws SQLException {
        return (int) consultarNumero(c, "SELECT NVL(MAX(numero_orden), 0) + 1 FROM comida_fuera_hogar"
                + " WHERE registro_diario_id = ?", registroDiarioId);
    }
}
