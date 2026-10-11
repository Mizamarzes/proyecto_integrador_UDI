package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.ComidaFueraHogarDAO;
import com.udi.dao.GastoDiarioDAO;
import com.udi.dao.LugarCompraDAO;
import com.udi.dao.RegistroDiarioDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.ComidaFueraHogar;
import com.udi.model.Encuesta;
import com.udi.model.GastoDiario;
import com.udi.model.RegistroDiario;
import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FrecuenciaCompra;
import com.udi.model.enums.TipoGasto;
import com.udi.model.enums.UnidadMedida;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Capítulo D (gastos diarios) y D1 (comidas preparadas fuera del hogar):
 * la página de cada día y sus renglones (RF-03, RF-04, RF-09).
 */
public class GastoDiarioService {

    /** Instrucción 4 del cuadernillo: los gastos personales solo se registran los primeros 7 días. */
    public static final int ULTIMO_DIA_GASTOS_PERSONALES = 7;

    private final RegistroDiarioDAO registroDao = new RegistroDiarioDAO();
    private final GastoDiarioDAO gastoDao = new GastoDiarioDAO();
    private final ComidaFueraHogarDAO comidaDao = new ComidaFueraHogarDAO();
    private final LugarCompraDAO lugarCompraDao = new LugarCompraDAO();

    // ------------------------------------------------------------------
    // Registro diario (encabezado de la página del día)
    // ------------------------------------------------------------------

    public List<RegistroDiario> listarRegistros(long encuestaId) {
        return ConexionBD.consultar(c -> registroDao.listarPorEncuesta(c, encuestaId));
    }

    public Optional<RegistroDiario> buscarRegistro(long id) {
        return ConexionBD.consultar(c -> registroDao.buscarPorId(c, id));
    }

    /**
     * Crea la página de un día. La fecha debe ser fecha_desde + numero_dia - 1;
     * si no viene, se calcula. RF-07: el capítulo C debe estar completo antes.
     */
    public RegistroDiario crearRegistro(RegistroDiario registro) {
        Validar.requerido(registro.getEncuestaId(), "Encuesta");
        return ConexionBD.enTransaccion(c -> {
            Encuesta encuesta = ReglasEncuesta.exigirEditable(c, registro.getEncuestaId());
            Validar.condicion(CaracteristicaService.estaCompleto(c, encuesta.getId()),
                    "Antes de registrar los gastos diarios debe completar los 25 grupos del capítulo C.");
            registro.setId(null);
            validarRegistro(c, encuesta, registro);
            registro.setCreatedBy(Sesion.idUsuario());
            return registroDao.insertar(c, registro);
        });
    }

    public RegistroDiario actualizarRegistro(RegistroDiario registro) {
        Validar.requerido(registro.getId(), "Id del registro diario");
        return ConexionBD.enTransaccion(c -> {
            RegistroDiario actual = Validar.encontrado(registroDao.buscarPorId(c, registro.getId()),
                    "No existe el registro diario " + registro.getId() + ".");
            Encuesta encuesta = ReglasEncuesta.exigirEditable(c, actual.getEncuestaId());
            registro.setEncuestaId(actual.getEncuestaId());
            validarRegistro(c, encuesta, registro);
            registroDao.actualizar(c, registro);
            return registro;
        });
    }

    /** Elimina el día con todos sus renglones (en cascada). */
    public boolean eliminarRegistro(long registroId) {
        return ConexionBD.enTransaccion(c -> {
            RegistroDiario actual = Validar.encontrado(registroDao.buscarPorId(c, registroId),
                    "No existe el registro diario " + registroId + ".");
            ReglasEncuesta.exigirEditable(c, actual.getEncuestaId());
            return registroDao.eliminar(c, registroId);
        });
    }

    /** D.2 Total de renglones con información del día (tabla D + tabla D1). */
    public long contarRenglones(long registroId) {
        return ConexionBD.consultar(c -> gastoDao.contarPorRegistro(c, registroId) + comidaDao.contarPorRegistro(c, registroId));
    }

    private void validarRegistro(Connection c, Encuesta encuesta, RegistroDiario registro) throws SQLException {
        Validar.rango(registro.getNumeroDia(), 1, EncuestaService.DIAS_PERIODO, "Número de día");
        LocalDate fechaEsperada = encuesta.getFechaDesde().plusDays(registro.getNumeroDia() - 1);
        if (registro.getFecha() == null) {
            registro.setFecha(fechaEsperada);
        }
        Validar.condicion(registro.getFecha().equals(fechaEsperada),
                "El día " + registro.getNumeroDia() + " de la encuesta corresponde a la fecha " + fechaEsperada + ".");
        Optional<RegistroDiario> mismoDia = registroDao.buscarPorEncuestaYDia(c, registro.getEncuestaId(), registro.getNumeroDia());
        if (mismoDia.isPresent() && !mismoDia.get().getId().equals(registro.getId())) {
            throw new ReglaNegocioException("El día " + registro.getNumeroDia() + " ya está registrado en esta encuesta.");
        }
    }

    // ------------------------------------------------------------------
    // Capítulo D: gastos diarios
    // ------------------------------------------------------------------

    public List<GastoDiario> listarGastos(long registroId) {
        return ConexionBD.consultar(c -> gastoDao.listarPorRegistro(c, registroId));
    }

    public Optional<GastoDiario> buscarGasto(long id) {
        return ConexionBD.consultar(c -> gastoDao.buscarPorId(c, id));
    }

    /** Si no trae número de orden, se le asigna el siguiente del día. Si no trae encuestador, el de la encuesta. */
    public GastoDiario crearGasto(GastoDiario gasto) {
        Validar.requerido(gasto.getRegistroDiarioId(), "Registro diario");
        return ConexionBD.enTransaccion(c -> {
            Dia dia = obtenerDiaEditable(c, gasto.getRegistroDiarioId());
            if (gasto.getNumeroOrden() == null) {
                gasto.setNumeroOrden(gastoDao.siguienteNumeroOrden(c, gasto.getRegistroDiarioId()));
            }
            if (gasto.getEncuestadorId() == null) {
                gasto.setEncuestadorId(dia.encuesta.getEncuestadorId());
            }
            gasto.setId(null);
            validarGasto(c, dia, gasto);
            gasto.setCreatedBy(Sesion.idUsuario());
            return gastoDao.insertar(c, gasto);
        });
    }

    public GastoDiario actualizarGasto(GastoDiario gasto) {
        Validar.requerido(gasto.getId(), "Id del gasto");
        return ConexionBD.enTransaccion(c -> {
            GastoDiario actual = Validar.encontrado(gastoDao.buscarPorId(c, gasto.getId()),
                    "No existe el gasto " + gasto.getId() + ".");
            Dia dia = obtenerDiaEditable(c, actual.getRegistroDiarioId());
            gasto.setRegistroDiarioId(actual.getRegistroDiarioId());
            if (gasto.getEncuestadorId() == null) {
                gasto.setEncuestadorId(actual.getEncuestadorId());
            }
            validarGasto(c, dia, gasto);
            gastoDao.actualizar(c, gasto);
            return gasto;
        });
    }

    public boolean eliminarGasto(long gastoId) {
        return ConexionBD.enTransaccion(c -> {
            GastoDiario actual = Validar.encontrado(gastoDao.buscarPorId(c, gastoId), "No existe el gasto " + gastoId + ".");
            obtenerDiaEditable(c, actual.getRegistroDiarioId());
            return gastoDao.eliminar(c, gastoId);
        });
    }

    private void validarGasto(Connection c, Dia dia, GastoDiario gasto) throws SQLException {
        Validar.requerido(gasto.getNombreArticulo(), "Nombre del artículo o servicio");
        Validar.positivo(gasto.getCantidad(), "Cantidad adquirida");
        Validar.requerido(gasto.getUnidadMedida(), "Unidad de medida");
        Validar.otraCual(gasto.getUnidadMedida() == UnidadMedida.OTRA, gasto.getUnidadMedidaOtra(), "Unidad de medida");
        if (gasto.getUnidadMedida().esTradicional()) {
            String motivo = "para unidades tradicionales (libra, kilo, litro...)";
            Validar.vacio(gasto.getEquivalenciaGr(), "Equivalencia en gramos", motivo);
            Validar.vacio(gasto.getEquivalenciaCm3(), "Equivalencia en centímetros cúbicos", motivo);
        }
        Validar.condicion(gasto.getEquivalenciaGr() == null || gasto.getEquivalenciaCm3() == null,
                "La equivalencia se registra en gramos o en centímetros cúbicos, no en ambos.");
        Validar.noNegativo(gasto.getEquivalenciaGr(), "Equivalencia en gramos");
        Validar.noNegativo(gasto.getEquivalenciaCm3(), "Equivalencia en centímetros cúbicos");
        validarRenglon(c, dia, gasto.getNumeroOrden(), gasto.getFormaAdquisicion(), gasto.getFormaAdquisicionOtra(),
                gasto.getLugarCompraId(), gasto.getFrecuenciaCompra(), gasto.getValor(), gasto.getTipoGasto());
        for (GastoDiario otro : gastoDao.listarPorRegistro(c, gasto.getRegistroDiarioId())) {
            if (otro.getNumeroOrden().equals(gasto.getNumeroOrden()) && !otro.getId().equals(gasto.getId())) {
                throw new ReglaNegocioException("Ya existe el renglón " + gasto.getNumeroOrden() + " en la tabla D de este día.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Capítulo D1: comidas preparadas fuera del hogar
    // ------------------------------------------------------------------

    public List<ComidaFueraHogar> listarComidas(long registroId) {
        return ConexionBD.consultar(c -> comidaDao.listarPorRegistro(c, registroId));
    }

    public Optional<ComidaFueraHogar> buscarComida(long id) {
        return ConexionBD.consultar(c -> comidaDao.buscarPorId(c, id));
    }

    /** Si no trae número de orden, se le asigna el siguiente del día. Si no trae encuestador, el de la encuesta. */
    public ComidaFueraHogar crearComida(ComidaFueraHogar comida) {
        Validar.requerido(comida.getRegistroDiarioId(), "Registro diario");
        return ConexionBD.enTransaccion(c -> {
            Dia dia = obtenerDiaEditable(c, comida.getRegistroDiarioId());
            if (comida.getNumeroOrden() == null) {
                comida.setNumeroOrden(comidaDao.siguienteNumeroOrden(c, comida.getRegistroDiarioId()));
            }
            if (comida.getEncuestadorId() == null) {
                comida.setEncuestadorId(dia.encuesta.getEncuestadorId());
            }
            comida.setId(null);
            validarComida(c, dia, comida);
            comida.setCreatedBy(Sesion.idUsuario());
            return comidaDao.insertar(c, comida);
        });
    }

    public ComidaFueraHogar actualizarComida(ComidaFueraHogar comida) {
        Validar.requerido(comida.getId(), "Id de la comida");
        return ConexionBD.enTransaccion(c -> {
            ComidaFueraHogar actual = Validar.encontrado(comidaDao.buscarPorId(c, comida.getId()),
                    "No existe la comida " + comida.getId() + ".");
            Dia dia = obtenerDiaEditable(c, actual.getRegistroDiarioId());
            comida.setRegistroDiarioId(actual.getRegistroDiarioId());
            if (comida.getEncuestadorId() == null) {
                comida.setEncuestadorId(actual.getEncuestadorId());
            }
            validarComida(c, dia, comida);
            comidaDao.actualizar(c, comida);
            return comida;
        });
    }

    public boolean eliminarComida(long comidaId) {
        return ConexionBD.enTransaccion(c -> {
            ComidaFueraHogar actual = Validar.encontrado(comidaDao.buscarPorId(c, comidaId),
                    "No existe la comida " + comidaId + ".");
            obtenerDiaEditable(c, actual.getRegistroDiarioId());
            return comidaDao.eliminar(c, comidaId);
        });
    }

    private void validarComida(Connection c, Dia dia, ComidaFueraHogar comida) throws SQLException {
        Validar.requerido(comida.getDescripcion(), "Descripción de la comida");
        Validar.requerido(comida.getTipoComida(), "Tipo de comida");
        Validar.positivo(comida.getCantidad(), "Cantidad adquirida");
        validarRenglon(c, dia, comida.getNumeroOrden(), comida.getFormaAdquisicion(), comida.getFormaAdquisicionOtra(),
                comida.getLugarCompraId(), comida.getFrecuenciaCompra(), comida.getValor(), comida.getTipoGasto());
        for (ComidaFueraHogar otra : comidaDao.listarPorRegistro(c, comida.getRegistroDiarioId())) {
            if (otra.getNumeroOrden().equals(comida.getNumeroOrden()) && !otra.getId().equals(comida.getId())) {
                throw new ReglaNegocioException("Ya existe el renglón " + comida.getNumeroOrden() + " en la tabla D1 de este día.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Reglas comunes a los renglones de D y D1
    // ------------------------------------------------------------------

    /**
     * - Si no fue compra, el lugar y la frecuencia de compra van vacíos.
     * - Si fue compra, la frecuencia es obligatoria. El lugar puede faltar
     *   (en el ejemplo del cuadernillo, el pasaje de bus no tiene lugar).
     * - La frecuencia NUNCA solo aplica en el capítulo C.
     * - Los gastos personales solo se registran los primeros 7 días.
     */
    private void validarRenglon(Connection c, Dia dia, Integer numeroOrden, FormaAdquisicion forma, String formaOtra,
                                Long lugarCompraId, FrecuenciaCompra frecuencia, BigDecimal valor, TipoGasto tipoGasto)
            throws SQLException {
        Validar.requerido(numeroOrden, "Número de orden");
        Validar.condicion(numeroOrden >= 1, "El número de orden debe ser mayor que cero.");
        Validar.requerido(forma, "Forma de adquisición");
        Validar.otraCual(forma == FormaAdquisicion.OTRA, formaOtra, "Forma de adquisición");
        if (forma.esCompra()) {
            Validar.requerido(frecuencia, "Frecuencia de compra");
        } else {
            String motivo = "si la forma de adquisición no es compra";
            Validar.vacio(lugarCompraId, "Lugar de compra", motivo);
            Validar.vacio(frecuencia, "Frecuencia de compra", motivo);
        }
        Validar.condicion(frecuencia != FrecuenciaCompra.NUNCA,
                "La frecuencia «Nunca» solo aplica en el capítulo C.");
        if (lugarCompraId != null) {
            Validar.condicion(lugarCompraDao.existe(c, lugarCompraId), "No existe el lugar de compra " + lugarCompraId + ".");
        }
        Validar.requerido(valor, "Valor pagado o estimado");
        Validar.noNegativo(valor, "Valor pagado o estimado");
        Validar.requerido(tipoGasto, "Gasto del hogar o personal");
        if (tipoGasto == TipoGasto.PERSONAL && dia.registro.getNumeroDia() > ULTIMO_DIA_GASTOS_PERSONALES) {
            throw new ReglaNegocioException("Los gastos personales solo se registran los primeros "
                    + ULTIMO_DIA_GASTOS_PERSONALES + " días (este es el día " + dia.registro.getNumeroDia() + ").");
        }
    }

    /** Día y encuesta a los que pertenece un renglón. Falla si la encuesta está cerrada. */
    private Dia obtenerDiaEditable(Connection c, long registroId) throws SQLException {
        RegistroDiario registro = Validar.encontrado(registroDao.buscarPorId(c, registroId),
                "No existe el registro diario " + registroId + ".");
        Encuesta encuesta = ReglasEncuesta.exigirEditable(c, registro.getEncuestaId());
        return new Dia(registro, encuesta);
    }

    private static final class Dia {
        private final RegistroDiario registro;
        private final Encuesta encuesta;

        private Dia(RegistroDiario registro, Encuesta encuesta) {
            this.registro = registro;
            this.encuesta = encuesta;
        }
    }
}
