package com.udi.dao;

import com.udi.model.CaracteristicaGasto;
import com.udi.model.enums.FrecuenciaCompra;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla caracteristica_gasto (capítulo C). */
public class CaracteristicaGastoDAO extends BaseDAO<CaracteristicaGasto> {

    @Override
    protected String tabla() {
        return "caracteristica_gasto";
    }

    @Override
    protected String secuencia() {
        return "seq_caracteristica_gasto";
    }

    @Override
    protected CaracteristicaGasto mapear(ResultSet rs) throws SQLException {
        CaracteristicaGasto e = new CaracteristicaGasto();
        mapearAuditoria(rs, e);
        e.setEncuestaId(getLong(rs, "encuesta_id"));
        e.setGrupoAlimentoId(getLong(rs, "grupo_alimento_id"));
        e.setEncuestadorId(getLong(rs, "encuestador_id"));
        e.setFrecuenciaCompra(getEnum(rs, "frecuencia_compra", FrecuenciaCompra.class));
        e.setValorUltimaCompra(getBigDecimal(rs, "valor_ultima_compra"));
        e.setCompraraEnPeriodo(getBoolean(rs, "comprara_en_periodo"));
        return e;
    }

    @Override
    public CaracteristicaGasto insertar(Connection c, CaracteristicaGasto e) throws SQLException {
        long id = asignarId(c, e);
        String sql = "INSERT INTO caracteristica_gasto (id, encuesta_id, grupo_alimento_id, encuestador_id, "
                + "frecuencia_compra, valor_ultima_compra, comprara_en_periodo, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        ejecutar(c, sql, id, e.getEncuestaId(), e.getGrupoAlimentoId(), e.getEncuestadorId(),
                e.getFrecuenciaCompra(), e.getValorUltimaCompra(), e.getCompraraEnPeriodo(), e.getCreatedBy());
        return e;
    }

    @Override
    public boolean actualizar(Connection c, CaracteristicaGasto e) throws SQLException {
        String sql = "UPDATE caracteristica_gasto SET encuesta_id = ?, grupo_alimento_id = ?, "
                + "encuestador_id = ?, frecuencia_compra = ?, valor_ultima_compra = ?, "
                + "comprara_en_periodo = ?, updated_at = SYSTIMESTAMP WHERE id = ?";
        return ejecutar(c, sql, e.getEncuestaId(), e.getGrupoAlimentoId(), e.getEncuestadorId(),
                e.getFrecuenciaCompra(), e.getValorUltimaCompra(), e.getCompraraEnPeriodo(), e.getId()) > 0;
    }

    public List<CaracteristicaGasto> listarPorEncuesta(Connection c, long encuestaId) throws SQLException {
        return consultar(c, "SELECT * FROM caracteristica_gasto WHERE encuesta_id = ? ORDER BY grupo_alimento_id", encuestaId);
    }

    public long contarPorEncuesta(Connection c, long encuestaId) throws SQLException {
        return consultarNumero(c, "SELECT COUNT(*) FROM caracteristica_gasto WHERE encuesta_id = ?", encuestaId);
    }

    public Optional<CaracteristicaGasto> buscarPorEncuestaYGrupo(Connection c, long encuestaId, long grupoAlimentoId)
            throws SQLException {
        return consultarUno(c, "SELECT * FROM caracteristica_gasto WHERE encuesta_id = ? AND grupo_alimento_id = ?",
                encuestaId, grupoAlimentoId);
    }
}
