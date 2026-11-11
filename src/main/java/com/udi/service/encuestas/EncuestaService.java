package com.udi.service.encuestas;

import com.udi.config.ConexionBD;
import com.udi.dao.encuestas.EncuestaDAO;
import com.udi.dao.encuestas.RegistroDiarioDAO;
import com.udi.dao.usuarios.EncuestadorDAO;
import com.udi.dao.usuarios.UsuarioDAO;
import com.udi.dao.verificacion.AuditoriaEncuestaDAO;
import com.udi.dao.viviendas.HogarDAO;
import com.udi.dao.viviendas.ViviendaDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.RutasModulo;
import com.udi.model.Encuesta;
import com.udi.model.AuditoriaEncuesta;
import com.udi.model.EncuestaResumen;
import com.udi.model.Encuestador;
import com.udi.model.Hogar;
import com.udi.model.Opcion;
import com.udi.model.Rol;
import com.udi.model.Usuario;
import com.udi.model.Vivienda;
import com.udi.model.enums.AccionAuditoria;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.model.enums.FormaPago;
import com.udi.util.Formato;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final ViviendaDAO viviendaDao = new ViviendaDAO();
    private final AuditoriaEncuestaDAO auditoriaDao = new AuditoriaEncuestaDAO();

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

    /**
     * Encuestas para el listado, con el nombre del encuestador. Un encuestador
     * solo ve las suyas; los demás roles ven todas.
     */
    public List<EncuestaResumen> listarResumen() {
        Usuario usuario = Sesion.exigirSesion();
        return ConexionBD.consultar(c -> {
            List<Encuesta> encuestas;
            if (Sesion.tieneRol(Rol.ENCUESTADOR)) {
                Optional<Encuestador> propio = encuestadorDao.buscarPorUsuario(c, usuario.getId());
                encuestas = propio.isPresent()
                        ? encuestaDao.listarPorEncuestador(c, propio.get().getId())
                        : Collections.emptyList();
            } else {
                encuestas = encuestaDao.listar(c);
            }
            Map<Long, String> nombres = nombresEncuestadores(c);
            Map<Long, Vivienda> viviendas = viviendasDeHogares(c);
            List<EncuestaResumen> resumen = new ArrayList<>();
            for (Encuesta e : encuestas) {
                resumen.add(new EncuestaResumen(e, nombres.getOrDefault(e.getEncuestadorId(), ""),
                        viviendas.get(e.getHogarId())));
            }
            resumen.sort(Comparator.comparing(r -> r.getEncuesta().getId()));
            return resumen;
        });
    }

    /**
     * Hogares que se pueden asignar a una encuesta: los que todavía no tienen
     * una, más {@code hogarActual} (el de la encuesta que se edita, puede ser null).
     */
    public List<Opcion> opcionesHogares(Long hogarActual) {
        return ConexionBD.consultar(c -> {
            Set<Long> conEncuesta = new HashSet<>();
            for (Encuesta e : encuestaDao.listar(c)) {
                conEncuesta.add(e.getHogarId());
            }
            Map<Long, Vivienda> viviendas = new HashMap<>();
            for (Vivienda v : viviendaDao.listar(c)) {
                viviendas.put(v.getId(), v);
            }
            List<Opcion> opciones = new ArrayList<>();
            for (Hogar h : hogarDao.listar(c)) {
                if (conEncuesta.contains(h.getId()) && !h.getId().equals(hogarActual)) {
                    continue;
                }
                Vivienda v = viviendas.get(h.getViviendaId());
                String direccion = v == null || v.getDireccion() == null ? "" : " · " + v.getDireccion();
                opciones.add(new Opcion(h.getId(),
                        Formato.codigo("H", h.getId()) + " · Hogar " + h.getHogarNumero() + direccion));
            }
            return opciones;
        });
    }

    /** Encuestadores con el nombre de su usuario. */
    public List<Opcion> opcionesEncuestadores() {
        return ConexionBD.consultar(c -> {
            Map<Long, Usuario> usuarios = new HashMap<>();
            for (Usuario u : usuarioDao.listar(c)) {
                usuarios.put(u.getId(), u);
            }
            List<Opcion> opciones = new ArrayList<>();
            for (Encuestador e : encuestadorDao.listar(c)) {
                Usuario u = usuarios.get(e.getUsuarioId());
                if (u != null) {
                    opciones.add(new Opcion(e.getId(), u.getNombreCompleto() + (u.isActivo() ? "" : " (inactivo)")));
                }
            }
            opciones.sort(Comparator.comparing(Opcion::getTexto));
            return opciones;
        });
    }

    /** Id del encuestador del usuario en sesión, si su rol es ENCUESTADOR. */
    public Optional<Long> encuestadorDeSesion() {
        Usuario usuario = Sesion.exigirSesion();
        if (!Sesion.tieneRol(Rol.ENCUESTADOR)) {
            return Optional.empty();
        }
        return ConexionBD.consultar(c -> encuestadorDao.buscarPorUsuario(c, usuario.getId()))
                .map(Encuestador::getId);
    }

    /** Crea la encuesta ABIERTA y calcula fecha_hasta. Un hogar solo tiene una encuesta. */
    public Encuesta crear(Encuesta encuesta) {
        Sesion.exigirModulo(RutasModulo.ENCUESTAS);
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
        Sesion.exigirModulo(RutasModulo.ENCUESTAS);
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

    /**
     * Marca el caso como completo (RF-06). Desde ese momento no admite cambios
     * y pasa a la bandeja de verificación (también si estaba en corrección).
     */
    public void cerrar(long encuestaId) {
        Sesion.exigirModulo(RutasModulo.ENCUESTAS);
        Usuario usuario = Sesion.exigirSesion();
        ConexionBD.enTransaccion(c -> {
            ReglasEncuesta.exigirEditable(c, encuestaId);
            encuestaDao.actualizarEstado(c, encuestaId, EstadoEncuesta.CERRADA, LocalDateTime.now(), usuario.getId());
            return registrar(c, encuestaId, AccionAuditoria.CIERRE, null);
        });
    }

    /** Solo un administrador puede reabrir un caso cerrado o aprobado (RF-06). */
    public void reabrir(long encuestaId) {
        Sesion.exigirAdmin();
        ConexionBD.enTransaccion(c -> {
            Encuesta encuesta = Validar.encontrado(encuestaDao.buscarPorId(c, encuestaId),
                    "No existe la encuesta " + encuestaId + ".");
            Validar.condicion(encuesta.estaCerrada(), "La encuesta " + encuestaId + " no está cerrada.");
            encuestaDao.actualizarEstado(c, encuestaId, EstadoEncuesta.REABIERTA,
                    encuesta.getFechaCierre(), encuesta.getCerradaPor());
            return registrar(c, encuestaId, AccionAuditoria.REAPERTURA, null);
        });
    }

    /**
     * Última devolución del crítico-digitador, para mostrarle al encuestador
     * qué debe corregir. Vacío si la encuesta no está en corrección.
     */
    public Optional<AuditoriaEncuesta> ultimaDevolucion(long encuestaId) {
        Sesion.exigirModulo(RutasModulo.ENCUESTAS);
        return ConexionBD.consultar(c -> {
            Optional<Encuesta> encuesta = encuestaDao.buscarPorId(c, encuestaId);
            if (!encuesta.isPresent() || encuesta.get().getEstado() != EstadoEncuesta.EN_CORRECCION) {
                return Optional.<AuditoriaEncuesta>empty();
            }
            return auditoriaDao.ultimaPorAccion(c, encuestaId, AccionAuditoria.DEVOLUCION);
        });
    }

    /** Agrega un registro al historial de la encuesta (tabla auditoria_encuesta). */
    private AuditoriaEncuesta registrar(Connection c, long encuestaId, AccionAuditoria accion, String observacion)
            throws SQLException {
        AuditoriaEncuesta registro = new AuditoriaEncuesta(encuestaId, accion, observacion);
        registro.setCreatedBy(Sesion.idUsuario());
        return auditoriaDao.insertar(c, registro);
    }

    /** Elimina la encuesta con su capítulo C y sus días (en cascada). No aplica a encuestas cerradas. */
    public boolean eliminar(long encuestaId) {
        Sesion.exigirModulo(RutasModulo.ENCUESTAS);
        return ConexionBD.enTransaccion(c -> {
            ReglasEncuesta.exigirEditable(c, encuestaId);
            return encuestaDao.eliminar(c, encuestaId);
        });
    }

    /** Vivienda de cada hogar (con su dirección y ubicación), por id del hogar. */
    private Map<Long, Vivienda> viviendasDeHogares(Connection c) throws SQLException {
        Map<Long, Vivienda> porId = new HashMap<>();
        for (Vivienda v : viviendaDao.listar(c)) {
            porId.put(v.getId(), v);
        }
        Map<Long, Vivienda> viviendas = new HashMap<>();
        for (Hogar h : hogarDao.listar(c)) {
            viviendas.put(h.getId(), porId.get(h.getViviendaId()));
        }
        return viviendas;
    }

    private Map<Long, String> nombresEncuestadores(Connection c) throws SQLException {
        Map<Long, String> nombresUsuarios = new HashMap<>();
        for (Usuario u : usuarioDao.listar(c)) {
            nombresUsuarios.put(u.getId(), u.getNombreCompleto());
        }
        Map<Long, String> nombres = new HashMap<>();
        for (Encuestador e : encuestadorDao.listar(c)) {
            nombres.put(e.getId(), nombresUsuarios.getOrDefault(e.getUsuarioId(), ""));
        }
        return nombres;
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
