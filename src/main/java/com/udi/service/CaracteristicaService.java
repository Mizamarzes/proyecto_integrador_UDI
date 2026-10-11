package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.CaracteristicaGastoDAO;
import com.udi.dao.GrupoAlimentoDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.CaracteristicaGasto;
import com.udi.model.Encuesta;
import com.udi.model.GrupoAlimento;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Capítulo C. Características del gasto en alimentos (RF-07). */
public class CaracteristicaService {

    private final CaracteristicaGastoDAO caracteristicaDao = new CaracteristicaGastoDAO();
    private final GrupoAlimentoDAO grupoAlimentoDao = new GrupoAlimentoDAO();

    public List<CaracteristicaGasto> listarPorEncuesta(long encuestaId) {
        return ConexionBD.consultar(c -> caracteristicaDao.listarPorEncuesta(c, encuestaId));
    }

    public Optional<CaracteristicaGasto> buscar(long id) {
        return ConexionBD.consultar(c -> caracteristicaDao.buscarPorId(c, id));
    }

    /** Si no trae encuestador, se toma el de la encuesta. */
    public CaracteristicaGasto crear(CaracteristicaGasto caracteristica) {
        Validar.requerido(caracteristica.getEncuestaId(), "Encuesta");
        return ConexionBD.enTransaccion(c -> {
            Encuesta encuesta = ReglasEncuesta.exigirEditable(c, caracteristica.getEncuestaId());
            if (caracteristica.getEncuestadorId() == null) {
                caracteristica.setEncuestadorId(encuesta.getEncuestadorId());
            }
            caracteristica.setId(null);
            validar(c, caracteristica);
            caracteristica.setCreatedBy(Sesion.idUsuario());
            return caracteristicaDao.insertar(c, caracteristica);
        });
    }

    public CaracteristicaGasto actualizar(CaracteristicaGasto caracteristica) {
        Validar.requerido(caracteristica.getId(), "Id del registro");
        return ConexionBD.enTransaccion(c -> {
            CaracteristicaGasto actual = Validar.encontrado(caracteristicaDao.buscarPorId(c, caracteristica.getId()),
                    "No existe el registro " + caracteristica.getId() + " del capítulo C.");
            ReglasEncuesta.exigirEditable(c, actual.getEncuestaId());
            caracteristica.setEncuestaId(actual.getEncuestaId());
            if (caracteristica.getEncuestadorId() == null) {
                caracteristica.setEncuestadorId(actual.getEncuestadorId());
            }
            validar(c, caracteristica);
            caracteristicaDao.actualizar(c, caracteristica);
            return caracteristica;
        });
    }

    public boolean eliminar(long id) {
        return ConexionBD.enTransaccion(c -> {
            CaracteristicaGasto actual = Validar.encontrado(caracteristicaDao.buscarPorId(c, id),
                    "No existe el registro " + id + " del capítulo C.");
            ReglasEncuesta.exigirEditable(c, actual.getEncuestaId());
            return caracteristicaDao.eliminar(c, id);
        });
    }

    /** RF-07: el capítulo C está completo cuando tiene un renglón por cada grupo de alimentos. */
    public boolean estaCompleto(long encuestaId) {
        return ConexionBD.consultar(c -> estaCompleto(c, encuestaId));
    }

    /** Grupos de alimentos que todavía no se han diligenciado en la encuesta. */
    public List<GrupoAlimento> gruposPendientes(long encuestaId) {
        return ConexionBD.consultar(c -> {
            Set<Long> diligenciados = caracteristicaDao.listarPorEncuesta(c, encuestaId).stream()
                    .map(CaracteristicaGasto::getGrupoAlimentoId)
                    .collect(Collectors.toSet());
            return grupoAlimentoDao.listar(c).stream()
                    .filter(g -> !diligenciados.contains(g.getId()))
                    .collect(Collectors.toList());
        });
    }

    static boolean estaCompleto(Connection c, long encuestaId) throws SQLException {
        return new CaracteristicaGastoDAO().contarPorEncuesta(c, encuestaId) >= new GrupoAlimentoDAO().contar(c);
    }

    /** C.3.1 y C.3.2 solo se responden si la frecuencia supera la bisemana. */
    private void validar(Connection c, CaracteristicaGasto caracteristica) throws SQLException {
        Validar.requerido(caracteristica.getGrupoAlimentoId(), "Grupo de alimentos");
        Validar.requerido(caracteristica.getFrecuenciaCompra(), "Frecuencia de compra");
        Validar.condicion(grupoAlimentoDao.existe(c, caracteristica.getGrupoAlimentoId()),
                "No existe el grupo de alimentos " + caracteristica.getGrupoAlimentoId() + ".");
        if (!caracteristica.getFrecuenciaCompra().superaBisemana()) {
            String motivo = "si la frecuencia de compra no supera la bisemana";
            Validar.vacio(caracteristica.getValorUltimaCompra(), "Valor de la última compra", motivo);
            Validar.vacio(caracteristica.getCompraraEnPeriodo(), "¿Comprará durante los 14 días?", motivo);
        }
        Validar.noNegativo(caracteristica.getValorUltimaCompra(), "Valor de la última compra");
        Optional<CaracteristicaGasto> mismoGrupo = caracteristicaDao.buscarPorEncuestaYGrupo(c,
                caracteristica.getEncuestaId(), caracteristica.getGrupoAlimentoId());
        if (mismoGrupo.isPresent() && !mismoGrupo.get().getId().equals(caracteristica.getId())) {
            throw new ReglaNegocioException("El grupo de alimentos " + caracteristica.getGrupoAlimentoId()
                    + " ya está diligenciado en esta encuesta.");
        }
    }
}
