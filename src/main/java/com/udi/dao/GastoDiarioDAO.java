package com.udi.dao;

import com.udi.model.GastoDiario;
import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.model.enums.TipoGasto;
import com.udi.model.enums.UnidadMedida;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Acceso a la tabla gasto_diario (capítulo D). */
public class GastoDiarioDAO extends BaseDAO<GastoDiario> {

    @Override
    protected String tabla() {
        return "gasto_diario";
    }

    @Override
    protected String secuencia() {
        return "seq_gasto_diario";
    }

    @Override
    protected GastoDiario mapear(ResultSet rs) throws SQLException {
        GastoDiario e = new GastoDiario();
        mapearAuditoria(rs, e);
        e.setRegistroDiarioId(getLong(rs, "registro_diario_id"));
        e.setEncuestadorId(getLong(rs, "encuestador_id"));
        e.setLugarCompraId(getLong(rs, "lugar_compra_id"));
        e.setNumeroOrden(getInteger(rs, "numero_orden"));
        e.setNombreArticulo(rs.getString("nombre_articulo"));
        e.setCantidad(getBigDecimal(rs, "cantidad"));
        e.setUnidadMedida(getEnum(rs, "unidad_medida", UnidadMedida.class));
        e.setUnidadMedidaOtra(rs.getString("unidad_medida_otra"));
        e.setEquivalenciaGr(getBigDecimal(rs, "equivalencia_gr"));
        e.setEquivalenciaCm3(getBigDecimal(rs, "equivalencia_cm3"));
        e.setFormaAdquisicion(getEnum(rs, "forma_adquisicion", FormaAdquisicion.class));
        e.setFormaAdquisicionOtra(rs.getString("forma_adquisicion_otra"));
        e.setValor(getBigDecimal(rs, "valor"));
        e.setFrecuenciaCompra(getEnum(rs, "frecuencia_compra", FrecuenciaCompra.class));
        e.setTipoGasto(getEnum(rs, "tipo_gasto", TipoGasto.class));
        e.setBeneficiarioMesada(rs.getString("beneficiario_mesada"));
        return e;
    }

    @Override
    public GastoDiario insertar(Connection c, GastoDiario e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO gasto_diario (id, registro_diario_id, encuestador_id, lugar_compra_id, "
                + "numero_orden, nombre_articulo, cantidad, unidad_medida, unidad_medida_otra, "
                + "equivalencia_gr, equivalencia_cm3, forma_adquisicion, forma_adquisicion_otra, valor, "
                + "frecuencia_compra, tipo_gasto, beneficiario_mesada, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getRegistroDiarioId(), e.getEncuestadorId(), e.getLugarCompraId(),
                e.getNumeroOrden(), e.getNombreArticulo(), e.getCantidad(), e.getUnidadMedida(),
                e.getUnidadMedidaOtra(), e.getEquivalenciaGr(), e.getEquivalenciaCm3(),
                e.getFormaAdquisicion(), e.getFormaAdquisicionOtra(), e.getValor(), e.getFrecuenciaCompra(),
                e.getTipoGasto(), e.getBeneficiarioMesada(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, GastoDiario e) throws SQLException {
        String sql = "UPDATE gasto_diario SET registro_diario_id = ?, encuestador_id = ?, "
                + "lugar_compra_id = ?, numero_orden = ?, nombre_articulo = ?, cantidad = ?, "
                + "unidad_medida = ?, unidad_medida_otra = ?, equivalencia_gr = ?, equivalencia_cm3 = ?, "
                + "forma_adquisicion = ?, forma_adquisicion_otra = ?, valor = ?, frecuencia_compra = ?, "
                + "tipo_gasto = ?, beneficiario_mesada = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getRegistroDiarioId(), e.getEncuestadorId(), e.getLugarCompraId(),
                e.getNumeroOrden(), e.getNombreArticulo(), e.getCantidad(), e.getUnidadMedida(),
                e.getUnidadMedidaOtra(), e.getEquivalenciaGr(), e.getEquivalenciaCm3(),
                e.getFormaAdquisicion(), e.getFormaAdquisicionOtra(), e.getValor(), e.getFrecuenciaCompra(),
                e.getTipoGasto(), e.getBeneficiarioMesada(), e.getId()) > 0;
    }

    public List<GastoDiario> listarPorRegistro(Connection c, long registroDiarioId) throws SQLException {
        return consultar(c, "SELECT * FROM gasto_diario WHERE registro_diario_id = ? ORDER BY numero_orden", registroDiarioId);
    }

    public long contarPorRegistro(Connection c, long registroDiarioId) throws SQLException {
        return consultarNumero(c, "SELECT COUNT(*) FROM gasto_diario WHERE registro_diario_id = ?", registroDiarioId);
    }

    /** Número de orden que le corresponde al próximo renglón del día. */
    public int siguienteNumeroOrden(Connection c, long registroDiarioId) throws SQLException {
        return (int) consultarNumero(c, "SELECT NVL(MAX(numero_orden), 0) + 1 FROM gasto_diario WHERE registro_diario_id = ?",
                registroDiarioId);
    }
}
