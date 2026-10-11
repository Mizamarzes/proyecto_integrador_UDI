package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.EncuestaDAO;
import com.udi.dao.EncuestadorDAO;
import com.udi.dao.HogarDAO;
import com.udi.dao.RegistroDiarioDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.Encuesta;
import com.udi.model.Usuario;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.model.enums.FormaPago;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Encuesta de un hogar: periodo de 14 días, preguntas del cierre (D2) y
 * estado del caso (RF-02, RF-06).
 */
public class EncuestaService {

    /** El periodo es de 14 días consecutivos: fecha_hasta = fecha_desde + 13. */
    public static final int DIAS_PERIODO = 14;

    private final EncuestaDAO encuestaDao = new EncuestaDAO();
    private final HogarDAO hogarDao = new HogarDAO();
    private final EncuestadorDAO encuestadorDao = new EncuestadorDAO();
    private final RegistroDiarioDAO registroDiarioDao = new RegistroDiarioDAO();

    public List<Encuesta> listar() {
        return ConexionBD.consultar(encuestaDao::listar);
    }

    public Optional<Encuesta> buscar(long id) {
        return ConexionBD.consultar(c -> encuestaDao.buscarPorId(c, id));
    }

    public Optional<Encuesta> buscarPorHogar(long hogarId) {
        return ConexionBD.consultar(c -> encuestaDao.buscarPorHogar(c, hogarId));
    }

    public List<Encuesta> listarPorEncuestador(long encuestadorId) {
        return ConexionBD.consultar(c -> encuestaDao.listarPorEncuestador(c, encuestadorId));
    }

    public List<Encuesta> listarPorEstado(EstadoEncuesta estado) {
        return ConexionBD.consultar(c -> encuestaDao.listarPorEstado(c, estado));
    }

    /** Crea la encuesta ABIERTA y calcula fecha_hasta. Un hogar solo tiene una encuesta. */
    public Encuesta crear(Encuesta encuesta) {
        Validar.requerido(encuesta.getHogarId(), "Hogar");
        Validar.requerido(encuesta.getEncuestadorId(), "Encuestador");
        Validar.requerido(encuesta.getFechaDesde(), "Fecha de inicio (desde el día)");
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(hogarDao.existe(c, encuesta.getHogarId()), "No existe el hogar " + encuesta.getHogarId() + ".");
            Validar.condicion(!encuestaDao.buscarPorHogar(c, encuesta.getHogarId()).isPresent(),
                    "El hogar " + encuesta.getHogarId() + " ya tiene una encuesta.");
            validarDatos(c, encuesta);
            encuesta.setId(null);
            encuesta.setEstado(EstadoEncuesta.ABIERTA);
            encuesta.setFechaCierre(null);
            encuesta.setCerradaPor(null);
            encuesta.setCreatedBy(Sesion.idUsuario());
            return encuestaDao.insertar(c, encuesta);
        });
    }

    /**
     * Actualiza los datos de la encuesta. El hogar y el estado no se cambian
     * aquí (el estado se cambia con {@link #cerrar} y {@link #reabrir}).
     */
    public Encuesta actualizar(Encuesta encuesta) {
        Validar.requerido(encuesta.getId(), "Id de la encuesta");
        Validar.requerido(encuesta.getEncuestadorId(), "Encuestador");
        Validar.requerido(encuesta.getFechaDesde(), "Fecha de inicio (desde el día)");
        return ConexionBD.enTransaccion(c -> {
            Encuesta actual = ReglasEncuesta.exigirEditable(c, encuesta.getId());
            if (!actual.getFechaDesde().equals(encuesta.getFechaDesde())
                    && !registroDiarioDao.listarPorEncuesta(c, encuesta.getId()).isEmpty()) {
                throw new ReglaNegocioException(
                        "No se puede cambiar la fecha de inicio porque la encuesta ya tiene días registrados.");
            }
            validarDatos(c, encuesta);
            encuesta.setHogarId(actual.getHogarId());
            encuesta.setEstado(actual.getEstado());
            encuesta.setFechaCierre(actual.getFechaCierre());
            encuesta.setCerradaPor(actual.getCerradaPor());
            encuestaDao.actualizar(c, encuesta);
            return encuesta;
        });
    }

    /** Marca el caso como completo (RF-06). Desde ese momento no admite cambios. */
    public void cerrar(long encuestaId) {
        Usuario usuario = Sesion.exigirSesion();
        ConexionBD.enTransaccion(c -> {
            ReglasEncuesta.exigirEditable(c, encuestaId);
            return encuestaDao.actualizarEstado(c, encuestaId, EstadoEncuesta.CERRADA, LocalDateTime.now(), usuario.getId());
        });
    }

    /** Solo un administrador puede reabrir un caso cerrado (RF-06). */
    public void reabrir(long encuestaId) {
        Sesion.exigirAdmin();
        ConexionBD.enTransaccion(c -> {
            Encuesta encuesta = Validar.encontrado(encuestaDao.buscarPorId(c, encuestaId),
                    "No existe la encuesta " + encuestaId + ".");
            Validar.condicion(encuesta.estaCerrada(), "La encuesta " + encuestaId + " no está cerrada.");
            return encuestaDao.actualizarEstado(c, encuestaId, EstadoEncuesta.REABIERTA,
                    encuesta.getFechaCierre(), encuesta.getCerradaPor());
        });
    }

    /** Elimina la encuesta con su capítulo C y sus días (en cascada). No aplica a encuestas cerradas. */
    public boolean eliminar(long encuestaId) {
        return ConexionBD.enTransaccion(c -> {
            ReglasEncuesta.exigirEditable(c, encuestaId);
            return encuestaDao.eliminar(c, encuestaId);
        });
    }

    private void validarDatos(Connection c, Encuesta encuesta) throws SQLException {
        Validar.condicion(encuestadorDao.existe(c, encuesta.getEncuestadorId()),
                "No existe el encuestador " + encuesta.getEncuestadorId() + ".");
        if (encuesta.getInformanteOrden() != null) {
            Validar.rango(encuesta.getInformanteOrden(), 1, 99, "Número de orden de quien responde");
        }
        encuesta.setFechaHasta(encuesta.getFechaDesde().plusDays(DIAS_PERIODO - 1));
        Validar.otraCual(encuesta.getFormaPago() == FormaPago.OTRA, encuesta.getFormaPagoOtra(), "Forma de pago");
    }
}
