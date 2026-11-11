package com.udi.service.reportes;

import com.udi.config.ConexionBD;
import com.udi.dao.reportes.ReporteDAO;
import com.udi.dao.usuarios.EncuestadorDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.RutasModulo;
import com.udi.model.Entidad;
import com.udi.model.FiltroReporte;
import com.udi.model.Reporte;
import com.udi.model.ValorReporte;
import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.TipoGasto;
import com.udi.model.Rol;
import com.udi.util.Sesion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes del operativo de campo y del gasto registrado en el Cuadernillo 2.
 * Todas las operaciones piden el módulo «reportes». Un encuestador con ese
 * permiso solo ve sus propias encuestas, sin importar el filtro que envíe.
 */
public class ReporteService {

    /** Id que no existe: el usuario es encuestador pero no tiene registro en la tabla encuestador. */
    private static final long SIN_ENCUESTADOR = -1L;

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final EncuestadorDAO encuestadorDAO = new EncuestadorDAO();

    public Reporte generar(FiltroReporte filtro) {
        Sesion.exigirModulo(RutasModulo.REPORTES);
        return ConexionBD.consultar(c -> {
            Long propio = encuestadorPropio(c);
            if (filtro.getDesde() != null && filtro.getHasta() != null && filtro.getHasta().isBefore(filtro.getDesde())) {
                throw new ReglaNegocioException("La fecha «hasta» no puede ser anterior a la fecha «desde».");
            }
            FiltroReporte f = new FiltroReporte(filtro.getDesde(), filtro.getHasta(), filtro.getRegionId(),
                    filtro.getDepartamentoId(), filtro.getMunicipioId());
            f.setEncuestadorId(propio);
            long[] renglones = reporteDAO.contarRenglones(c, f);
            return new Reporte(
                    reporteDAO.porEncuestador(c, f),
                    reporteDAO.hogaresPorGrupo(c, f),
                    formas(reporteDAO.gastoPorForma(c, f)),
                    reporteDAO.gastoPorLugar(c, f),
                    tipos(reporteDAO.gastoPorTipo(c, f)),
                    renglones[0], renglones[1], renglones[2]);
        });
    }

    /** Las siete formas del cuadernillo en orden, con 0 las que no tienen gasto. */
    private static List<ValorReporte> formas(List<ValorReporte> filas) {
        Map<String, BigDecimal> valores = porCodigo(filas);
        List<ValorReporte> resultado = new ArrayList<>();
        for (FormaAdquisicion forma : FormaAdquisicion.values()) {
            resultado.add(new ValorReporte(String.valueOf(forma.getCodigo()), forma.getDescripcion(),
                    valores.get(forma.name())));
        }
        return resultado;
    }

    private static List<ValorReporte> tipos(List<ValorReporte> filas) {
        Map<String, BigDecimal> valores = porCodigo(filas);
        List<ValorReporte> resultado = new ArrayList<>();
        for (TipoGasto tipo : TipoGasto.values()) {
            resultado.add(new ValorReporte(String.valueOf(tipo.getCodigo()), tipo.getDescripcion(),
                    valores.get(tipo.name())));
        }
        return resultado;
    }

    private static Map<String, BigDecimal> porCodigo(List<ValorReporte> filas) {
        Map<String, BigDecimal> valores = new HashMap<>();
        for (ValorReporte fila : filas) {
            valores.put(fila.getCodigo(), fila.getValor());
        }
        return valores;
    }

    /** Id del encuestador de la sesión si su rol es ENCUESTADOR; null para los demás roles. */
    private Long encuestadorPropio(Connection c) throws SQLException {
        if (!Sesion.tieneRol(Rol.ENCUESTADOR)) {
            return null;
        }
        return encuestadorDAO.buscarPorUsuario(c, Sesion.idUsuario()).map(Entidad::getId).orElse(SIN_ENCUESTADOR);
    }
}
