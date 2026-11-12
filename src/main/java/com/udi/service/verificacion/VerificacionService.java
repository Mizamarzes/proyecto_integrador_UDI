package com.udi.service.verificacion;

import com.udi.config.ConexionBD;
import com.udi.dao.catalogos.GrupoAlimentoDAO;
import com.udi.dao.catalogos.LugarCompraDAO;
import com.udi.dao.encuestas.CaracteristicaGastoDAO;
import com.udi.dao.encuestas.ComidaFueraHogarDAO;
import com.udi.dao.encuestas.EncuestaDAO;
import com.udi.dao.encuestas.GastoDiarioDAO;
import com.udi.dao.encuestas.RegistroDiarioDAO;
import com.udi.dao.encuestas.VisitaDAO;
import com.udi.dao.usuarios.EncuestadorDAO;
import com.udi.dao.usuarios.UsuarioDAO;
import com.udi.dao.verificacion.AuditoriaEncuestaDAO;
import com.udi.dao.viviendas.HogarDAO;
import com.udi.dao.viviendas.ViviendaDAO;
import com.udi.exception.DatosException;
import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.RutasModulo;
import com.udi.model.AuditoriaEncuesta;
import com.udi.model.CaracteristicaGasto;
import com.udi.model.CasoVerificacion;
import com.udi.model.ComidaFueraHogar;
import com.udi.model.Encuesta;
import com.udi.model.EncuestaResumen;
import com.udi.model.GastoDiario;
import com.udi.model.GrupoAlimento;
import com.udi.model.Hogar;
import com.udi.model.LugarCompra;
import com.udi.model.RegistroDiario;
import com.udi.model.ReglaVerificacion;
import com.udi.model.ReglaVerificacion.Nivel;
import com.udi.model.Usuario;
import com.udi.model.Visita;
import com.udi.model.Vivienda;
import com.udi.model.enums.AccionAuditoria;
import com.udi.model.enums.EstadoEncuesta;
import com.udi.model.enums.FormaAdquisicion;
import com.udi.model.enums.FormaPago;
import com.udi.model.enums.ResultadoEntrevista;
import com.udi.model.enums.TipoGasto;
import com.udi.service.encuestas.EncuestaService;
import com.udi.service.encuestas.GastoDiarioService;
import com.udi.util.Formato;
import com.udi.util.LibroExcel;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Verificación y consolidación de los casos cerrados por el crítico-digitador
 * (RF-10): validación cruzada, rechazo con observación, aprobación y
 * exportación a Excel para el traslado al sistema del DANE.
 *
 * Flujo de estados: PENDIENTE_APROBACION → RECHAZADA (rechazar) →
 * PENDIENTE_APROBACION (el encuestador la cierra de nuevo) → APROBADA
 * (aprobar, solo sin errores). El historial solo guarda las aprobaciones y
 * los rechazos.
 */
public class VerificacionService {

    /** Cuántos renglones se nombran en el detalle de una regla antes de resumir con «y N más». */
    private static final int MAXIMO_EJEMPLOS = 3;

    private final EncuestaService encuestaService = new EncuestaService();
    private final EncuestaDAO encuestaDao = new EncuestaDAO();
    private final VisitaDAO visitaDao = new VisitaDAO();
    private final CaracteristicaGastoDAO caracteristicaDao = new CaracteristicaGastoDAO();
    private final GrupoAlimentoDAO grupoDao = new GrupoAlimentoDAO();
    private final RegistroDiarioDAO registroDao = new RegistroDiarioDAO();
    private final GastoDiarioDAO gastoDao = new GastoDiarioDAO();
    private final ComidaFueraHogarDAO comidaDao = new ComidaFueraHogarDAO();
    private final LugarCompraDAO lugarDao = new LugarCompraDAO();
    private final HogarDAO hogarDao = new HogarDAO();
    private final ViviendaDAO viviendaDao = new ViviendaDAO();
    private final EncuestadorDAO encuestadorDao = new EncuestadorDAO();
    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final AuditoriaEncuestaDAO auditoriaDao = new AuditoriaEncuestaDAO();

    /**
     * Casos de la bandeja (pendientes de aprobación, rechazados y aprobados)
     * con su validación: primero los pendientes, luego los rechazados y al
     * final los aprobados; dentro de cada grupo, el cierre más
     * reciente primero.
     */
    public List<CasoVerificacion> listarCasos() {
        Sesion.exigirModulo(RutasModulo.VERIFICACION);
        List<EncuestaResumen> resumenes = encuestaService.listarResumen().stream()
                .filter(r -> r.getEncuesta().getEstado().enVerificacion())
                .collect(Collectors.toList());
        List<CasoVerificacion> casos = ConexionBD.consultar(c -> {
            long totalGrupos = grupoDao.contar(c);
            List<CasoVerificacion> lista = new ArrayList<>();
            for (EncuestaResumen r : resumenes) {
                lista.add(new CasoVerificacion(r, validar(c, r.getEncuesta(), totalGrupos)));
            }
            return lista;
        });
        casos.sort(Comparator.comparing((CasoVerificacion caso) -> orden(caso.getEncuesta().getEstado()))
                .thenComparing(caso -> caso.getEncuesta().getFechaCierre(),
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return casos;
    }

    /** Historial del caso, del más reciente al más antiguo. */
    public List<AuditoriaEncuesta> historial(long encuestaId) {
        Sesion.exigirModulo(RutasModulo.VERIFICACION);
        return ConexionBD.consultar(c -> auditoriaDao.listarPorEncuesta(c, encuestaId));
    }

    /**
     * Rechaza un caso pendiente de aprobación con una observación para el
     * encuestador. Vuelve a admitir cambios hasta que se cierre de nuevo.
     */
    public void rechazar(long encuestaId, String observacion) {
        Sesion.exigirModulo(RutasModulo.VERIFICACION);
        String texto = Formato.texto(observacion);
        Validar.requerido(texto, "Observación para el encuestador");
        Validar.condicion(texto.length() <= 1000, "La observación no puede tener más de 1000 caracteres.");
        ConexionBD.enTransaccion(c -> {
            Encuesta e = exigirPendiente(c, encuestaId, "rechazar");
            encuestaDao.actualizarEstado(c, encuestaId, EstadoEncuesta.RECHAZADA, e.getFechaCierre(), e.getCerradaPor());
            return registrar(c, encuestaId, AccionAuditoria.RECHAZADA, texto);
        });
    }

    /** Aprueba un caso pendiente cuya validación cruzada no tiene errores (los avisos no bloquean). */
    public void aprobar(long encuestaId) {
        Sesion.exigirModulo(RutasModulo.VERIFICACION);
        ConexionBD.enTransaccion(c -> {
            Encuesta e = exigirPendiente(c, encuestaId, "aprobar");
            long errores = validar(c, e, grupoDao.contar(c)).stream()
                    .filter(r -> r.getNivel() == Nivel.ERROR).count();
            if (errores > 0) {
                throw new ReglaNegocioException("No se puede aprobar el caso " + Formato.codigo("E", encuestaId)
                        + ": la validación tiene " + plural(errores, "error", "errores")
                        + ". Recháselo con una observación.");
            }
            encuestaDao.actualizarEstado(c, encuestaId, EstadoEncuesta.APROBADA, e.getFechaCierre(), e.getCerradaPor());
            return registrar(c, encuestaId, AccionAuditoria.APROBADA, null);
        });
    }

    /**
     * Escribe el libro de Excel del caso aprobado (capítulos A, B, C, D, D1 y
     * D2, una hoja por capítulo).
     */
    public void exportar(long encuestaId, Path archivo) {
        Sesion.exigirModulo(RutasModulo.VERIFICACION);
        LibroExcel libro = ConexionBD.consultar(c -> {
            Encuesta e = Validar.encontrado(encuestaDao.buscarPorId(c, encuestaId),
                    "No existe la encuesta " + encuestaId + ".");
            Validar.condicion(e.getEstado() == EstadoEncuesta.APROBADA,
                    "Solo se exportan casos aprobados. Apruebe primero el caso " + Formato.codigo("E", encuestaId) + ".");
            return libroExcel(c, e);
        });
        try {
            libro.guardar(archivo);
        } catch (IOException ex) {
            throw new ReglaNegocioException("No se pudo escribir el archivo: " + ex.getMessage());
        }
    }

    /** Nombre sugerido del archivo de exportación. */
    public static String nombreArchivo(long encuestaId) {
        return "ENPH_C2_" + Formato.codigo("E", encuestaId) + ".xlsx";
    }

    // ------------------------------------------------------------------
    // Validación cruzada
    // ------------------------------------------------------------------

    private List<ReglaVerificacion> validar(Connection c, Encuesta e, long totalGrupos) throws SQLException {
        List<Visita> visitas = visitaDao.listarPorHogar(c, e.getHogarId());
        List<CaracteristicaGasto> caracteristicas = caracteristicaDao.listarPorEncuesta(c, e.getId());
        Set<Long> grupos = new TreeSet<>();
        for (GrupoAlimento g : grupoDao.listar(c)) {
            grupos.add(g.getId());
        }
        Map<Long, Integer> diaDeRegistro = new HashMap<>();
        for (RegistroDiario r : registroDao.listarPorEncuesta(c, e.getId())) {
            diaDeRegistro.put(r.getId(), r.getNumeroDia());
        }
        List<Renglon> renglones = new ArrayList<>();
        for (GastoDiario g : gastoDao.listarPorEncuesta(c, e.getId())) {
            renglones.add(new Renglon("D", diaDeRegistro.get(g.getRegistroDiarioId()), g.getNumeroOrden(),
                    g.getFormaAdquisicion(), g.getValor(), g.getTipoGasto(), g.getLugarCompraId()));
        }
        for (ComidaFueraHogar f : comidaDao.listarPorEncuesta(c, e.getId())) {
            renglones.add(new Renglon("D1", diaDeRegistro.get(f.getRegistroDiarioId()), f.getNumeroOrden(),
                    f.getFormaAdquisicion(), f.getValor(), f.getTipoGasto(), f.getLugarCompraId()));
        }

        List<ReglaVerificacion> reglas = new ArrayList<>();
        reglas.add(reglaIdentificacion(e));
        reglas.add(reglaVisitas(visitas));
        reglas.add(reglaGrupos(caracteristicas, grupos, totalGrupos));
        reglas.add(reglaUltimaCompra(caracteristicas));
        reglas.add(reglaDias(renglones));
        reglas.add(reglaGastosPersonales(renglones));
        reglas.add(reglaValores(renglones));
        reglas.add(reglaLugares(renglones));
        reglas.add(reglaCierre(e));
        return reglas;
    }

    private static ReglaVerificacion reglaIdentificacion(Encuesta e) {
        String titulo = "Cap. A · Informante y resultado";
        List<String> faltan = new ArrayList<>();
        if (Formato.texto(e.getInformanteNombre()) == null) {
            faltan.add("el nombre de quien responde");
        }
        if (e.getInformanteOrden() == null) {
            faltan.add("su número de orden");
        }
        if (e.getResultadoCuadernillo() == null) {
            faltan.add("el resultado del cuadernillo (A.18)");
        }
        if (!faltan.isEmpty()) {
            return new ReglaVerificacion(titulo, "Falta " + String.join(", ", faltan) + ".", Nivel.ERROR);
        }
        String informante = e.getInformanteNombre() + " (orden " + e.getInformanteOrden() + ")";
        if (e.getResultadoCuadernillo() != ResultadoEntrevista.EC) {
            return new ReglaVerificacion(titulo, informante + " · resultado " + e.getResultadoCuadernillo()
                    + ". Confirme el resultado contra el cuadernillo físico.", Nivel.AVISO);
        }
        return new ReglaVerificacion(titulo, informante + " · " + e.getResultadoCuadernillo() + ".", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaVisitas(List<Visita> visitas) {
        String titulo = "Control de visitas (B)";
        if (visitas.isEmpty()) {
            return new ReglaVerificacion(titulo, "No hay visitas registradas.", Nivel.ERROR);
        }
        List<String> sinFin = visitas.stream().filter(v -> v.getHoraFin() == null)
                .map(v -> String.valueOf(v.getNumeroVisita())).collect(Collectors.toList());
        if (sinFin.size() == 1) {
            return new ReglaVerificacion(titulo, "La visita " + sinFin.get(0) + " no tiene hora de terminación.", Nivel.ERROR);
        }
        if (!sinFin.isEmpty()) {
            return new ReglaVerificacion(titulo, "Las visitas " + lista(sinFin) + " no tienen hora de terminación.", Nivel.ERROR);
        }
        return new ReglaVerificacion(titulo, plural(visitas.size(), "visita registrada", "visitas registradas")
                + ", todas con hora de inicio, de terminación y resultado.", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaGrupos(List<CaracteristicaGasto> caracteristicas, Set<Long> grupos, long total) {
        String titulo = "Cap. C · Grupos de alimentos";
        Set<Long> registrados = caracteristicas.stream().map(CaracteristicaGasto::getGrupoAlimentoId).collect(Collectors.toSet());
        List<String> faltan = grupos.stream().filter(g -> !registrados.contains(g))
                .map(String::valueOf).collect(Collectors.toList());
        if (!faltan.isEmpty()) {
            return new ReglaVerificacion(titulo, registrados.size() + " de " + total + " grupos con frecuencia de compra: "
                    + (faltan.size() == 1 ? "falta el grupo " : "faltan los grupos ") + ejemplos(faltan) + ".", Nivel.ERROR);
        }
        return new ReglaVerificacion(titulo, total + " de " + total + " grupos con frecuencia de compra.", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaUltimaCompra(List<CaracteristicaGasto> caracteristicas) {
        String titulo = "Cap. C · Última compra (C.3)";
        List<CaracteristicaGasto> mayores = caracteristicas.stream()
                .filter(g -> g.getFrecuenciaCompra() != null && g.getFrecuenciaCompra().superaBisemana())
                .collect(Collectors.toList());
        List<String> incompletos = mayores.stream()
                .filter(g -> g.getValorUltimaCompra() == null || g.getCompraraEnPeriodo() == null)
                .map(g -> String.valueOf(g.getGrupoAlimentoId())).collect(Collectors.toList());
        if (!incompletos.isEmpty()) {
            return new ReglaVerificacion(titulo, (incompletos.size() == 1 ? "El grupo " : "Los grupos ") + ejemplos(incompletos)
                    + " compra(n) con menos frecuencia que la bisemana y no tiene(n) el valor de la última compra"
                    + " o si comprará en el periodo.", Nivel.ERROR);
        }
        if (mayores.isEmpty()) {
            return new ReglaVerificacion(titulo, "Ningún grupo se compra con menos frecuencia que la bisemana.", Nivel.CORRECTA);
        }
        return new ReglaVerificacion(titulo, "Registrada en " + plural(mayores.size(), "grupo", "grupos")
                + " con frecuencia mayor a la bisemana.", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaDias(List<Renglon> renglones) {
        String titulo = "Línea de 14 días (D y D1)";
        Set<Integer> conInformacion = new HashSet<>();
        for (Renglon r : renglones) {
            conInformacion.add(r.dia);
        }
        List<String> faltan = new ArrayList<>();
        for (int dia = 1; dia <= EncuestaService.DIAS_PERIODO; dia++) {
            if (!conInformacion.contains(dia)) {
                faltan.add(String.valueOf(dia));
            }
        }
        int dias = EncuestaService.DIAS_PERIODO - faltan.size();
        if (dias == 0) {
            return new ReglaVerificacion(titulo, "Ningún día tiene renglones de gastos (D) ni de comidas fuera del hogar (D1).",
                    Nivel.ERROR);
        }
        if (!faltan.isEmpty()) {
            return new ReglaVerificacion(titulo, dias + " de " + EncuestaService.DIAS_PERIODO + " días con información: "
                    + (faltan.size() == 1 ? "falta el día " : "faltan los días ") + lista(faltan) + ".", Nivel.ERROR);
        }
        return new ReglaVerificacion(titulo, dias + " de " + EncuestaService.DIAS_PERIODO + " días con información.", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaGastosPersonales(List<Renglon> renglones) {
        int ultimo = GastoDiarioService.ULTIMO_DIA_GASTOS_PERSONALES;
        String titulo = "Gastos personales solo en los días 1 a " + ultimo;
        List<String> fuera = renglones.stream()
                .filter(r -> r.tipoGasto == TipoGasto.PERSONAL && r.dia != null && r.dia > ultimo)
                .map(Renglon::toString).collect(Collectors.toList());
        if (!fuera.isEmpty()) {
            return new ReglaVerificacion(titulo, plural(fuera.size(), "gasto personal", "gastos personales")
                    + " después del día " + ultimo + ": " + ejemplos(fuera) + ".", Nivel.ERROR);
        }
        return new ReglaVerificacion(titulo, "Ningún gasto personal después del día " + ultimo + ".", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaValores(List<Renglon> renglones) {
        String titulo = "Valores según la forma de adquisición";
        List<String> comprasEnCero = renglones.stream()
                .filter(r -> r.forma == FormaAdquisicion.COMPRA && esCero(r.valor))
                .map(Renglon::toString).collect(Collectors.toList());
        List<String> sinEstimado = renglones.stream()
                .filter(r -> r.forma != null && !r.forma.esCompra() && esCero(r.valor))
                .map(Renglon::toString).collect(Collectors.toList());
        if (!comprasEnCero.isEmpty()) {
            return new ReglaVerificacion(titulo, plural(comprasEnCero.size(), "compra", "compras") + " con valor pagado de $0: "
                    + ejemplos(comprasEnCero) + ".", Nivel.ERROR);
        }
        if (!sinEstimado.isEmpty()) {
            return new ReglaVerificacion(titulo, plural(sinEstimado.size(), "renglón", "renglones")
                    + " recibido(s) sin compra con valor estimado de $0: " + ejemplos(sinEstimado)
                    + ". Confírmelo contra el cuadernillo físico.", Nivel.AVISO);
        }
        return new ReglaVerificacion(titulo, "Compras con valor pagado; pago, regalo, trueque y producción propia"
                + " con valor estimado.", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaLugares(List<Renglon> renglones) {
        String titulo = "Lugar de compra";
        List<String> sinLugar = renglones.stream()
                .filter(r -> r.forma == FormaAdquisicion.COMPRA && r.lugarCompraId == null)
                .map(Renglon::toString).collect(Collectors.toList());
        if (!sinLugar.isEmpty()) {
            return new ReglaVerificacion(titulo, plural(sinLugar.size(), "compra", "compras") + " sin lugar de compra: "
                    + ejemplos(sinLugar) + ".", Nivel.AVISO);
        }
        return new ReglaVerificacion(titulo, "Todas las compras tienen lugar de compra.", Nivel.CORRECTA);
    }

    private static ReglaVerificacion reglaCierre(Encuesta e) {
        String titulo = "D2 · Cierre del caso";
        List<String> faltan = new ArrayList<>();
        if (e.getGastoFiestas() == null) {
            faltan.add("la pregunta D2.1");
        }
        if (e.getFormaPago() == null || (e.getFormaPago() == FormaPago.OTRA && Formato.texto(e.getFormaPagoOtra()) == null)) {
            faltan.add("la pregunta D2.2");
        }
        if (!faltan.isEmpty()) {
            return new ReglaVerificacion(titulo, "Falta responder " + String.join(" y ", faltan) + ".", Nivel.ERROR);
        }
        LocalDateTime cierre = e.getFechaCierre();
        if (cierre == null) {
            return new ReglaVerificacion(titulo, "El caso no tiene fecha de cierre.", Nivel.ERROR);
        }
        if (!cierre.toLocalDate().isAfter(e.getFechaHasta())) {
            return new ReglaVerificacion(titulo, "Se cerró el " + Formato.fechaHora(cierre)
                    + ", antes de terminar el día 14 (" + Formato.fecha(e.getFechaHasta()) + ").", Nivel.ERROR);
        }
        return new ReglaVerificacion(titulo, "Cerrado el " + Formato.fechaHora(cierre)
                + ", después del día 14, con D2 respondido.", Nivel.CORRECTA);
    }

    // ------------------------------------------------------------------
    // Exportación
    // ------------------------------------------------------------------

    private LibroExcel libroExcel(Connection c, Encuesta e) throws SQLException {
        Hogar hogar = hogarDao.buscarPorId(c, e.getHogarId()).orElse(null);
        Vivienda v = hogar == null ? null : viviendaDao.buscarPorId(c, hogar.getViviendaId()).orElse(null);
        String encuestador = encuestadorDao.buscarPorId(c, e.getEncuestadorId())
                .flatMap(en -> buscarUsuario(c, en.getUsuarioId())).orElse("");
        String cerradaPor = e.getCerradaPor() == null ? "" : buscarUsuario(c, e.getCerradaPor()).orElse("");
        AuditoriaEncuesta aprobacion = auditoriaDao.ultimaPorAccion(c, e.getId(), AccionAuditoria.APROBADA).orElse(null);

        LibroExcel libro = new LibroExcel();
        LibroExcel.Hoja a = libro.hoja("A y D2 Encuesta").anchos(34, 56).encabezado("Campo", "Valor")
                .fila("Encuesta", Formato.codigo("E", e.getId()))
                .fila("Estado", e.getEstado().getDescripcion())
                .fila("Hogar", Formato.codigo("H", e.getHogarId()))
                .fila("A.13 Hogar número", hogar == null ? null : hogar.getHogarNumero())
                .fila("A.14 Total de personas", hogar == null ? null : hogar.getTotalPersonas())
                .fila("A.9 Ciclo", hogar == null ? null : hogar.getCiclo());
        if (v != null) {
            a.fila("Región", v.getRegionNombre())
                    .fila("A.2 Departamento", v.getDepartamentoNombre())
                    .fila("A.3 Municipio", v.getMunicipioNombre())
                    .fila("A.4 Clase", v.getClase())
                    .fila("A.5 Sector", v.getSector())
                    .fila("A.6 Sección", v.getSeccion())
                    .fila("A.7 Manzana", v.getManzana())
                    .fila("A.8 Segmento", v.getSegmento())
                    .fila("A.10 Edificación número", v.getEdificacionNumero())
                    .fila("A.11 Vivienda número", v.getViviendaNumero())
                    .fila("A.12 Total de hogares", v.getTotalHogares())
                    .fila("A.15 Barrio", v.getBarrio())
                    .fila("A.16 Dirección", v.getDireccion())
                    .fila("A.17 Teléfono", v.getTelefono());
        }
        a.fila("Encuestador", encuestador)
                .fila("Informante", e.getInformanteNombre())
                .fila("Número de orden del informante", e.getInformanteOrden())
                .fila("A.18 Resultado del cuadernillo", e.getResultadoCuadernillo() == null ? null : e.getResultadoCuadernillo().toString())
                .fila("A.21 Desde el día", Formato.fecha(e.getFechaDesde()))
                .fila("A.22 Hasta el día", Formato.fecha(e.getFechaHasta()))
                .fila("D2.1 Gasto por fiestas o invitados", e.getGastoFiestas() == null ? null : (e.getGastoFiestas() ? "Sí" : "No"))
                .fila("D2.2 Forma de pago", e.getFormaPago() == null ? null
                        : e.getFormaPago() == FormaPago.OTRA ? "Otra: " + e.getFormaPagoOtra() : e.getFormaPago().getDescripcion())
                .fila("Observaciones", e.getObservaciones())
                .fila("Cierre del caso", Formato.fechaHora(e.getFechaCierre()))
                .fila("Cerrado por", cerradaPor)
                .fila("Aprobado por", aprobacion == null ? null : aprobacion.getUsuarioNombre())
                .fila("Fecha de aprobación", aprobacion == null ? null : Formato.fechaHora(aprobacion.getCreatedAt()))
                .fila("Exportado", Formato.fechaHora(LocalDateTime.now()));

        LibroExcel.Hoja b = libro.hoja("B Visitas").anchos(8, 12, 12, 12, 26, 30, 18)
                .encabezado("Visita", "Fecha", "Hora inicio", "Hora fin", "Resultado", "Partes pendientes", "Próxima visita");
        for (Visita vi : visitaDao.listarPorHogar(c, e.getHogarId())) {
            b.fila(vi.getNumeroVisita(), Formato.fecha(vi.getFecha()), Formato.hora(vi.getHoraInicio()),
                    Formato.hora(vi.getHoraFin()), vi.getResultadoVisita() == null ? null : vi.getResultadoVisita().toString(),
                    vi.getPartesPendientes(),
                    vi.getFechaProxima() == null ? null : (Formato.fecha(vi.getFechaProxima()) + " " + Formato.hora(vi.getHoraProxima())).trim());
        }

        Map<Long, String> grupos = new HashMap<>();
        for (GrupoAlimento g : grupoDao.listar(c)) {
            grupos.put(g.getId(), g.getNombre());
        }
        LibroExcel.Hoja cap = libro.hoja("C Características").anchos(8, 60, 12, 24, 18, 14)
                .encabezado("Grupo", "Nombre", "C.2 Código", "C.2 Frecuencia", "C.3.1 Última compra", "C.3.2 Comprará");
        for (CaracteristicaGasto g : caracteristicaDao.listarPorEncuesta(c, e.getId())) {
            cap.fila(g.getGrupoAlimentoId(), grupos.get(g.getGrupoAlimentoId()),
                    g.getFrecuenciaCompra() == null ? null : g.getFrecuenciaCompra().getCodigo(),
                    g.getFrecuenciaCompra() == null ? null : g.getFrecuenciaCompra().getDescripcion(),
                    g.getValorUltimaCompra() == null ? null : LibroExcel.pesos(g.getValorUltimaCompra()),
                    g.getCompraraEnPeriodo() == null ? null : (g.getCompraraEnPeriodo() ? "Sí" : "No"));
        }

        Map<Long, RegistroDiario> registros = new HashMap<>();
        for (RegistroDiario r : registroDao.listarPorEncuesta(c, e.getId())) {
            registros.put(r.getId(), r);
        }
        Map<Long, String> lugares = new HashMap<>();
        for (LugarCompra l : lugarDao.listar(c)) {
            lugares.put(l.getId(), l.getNombre());
        }

        LibroExcel.Hoja d = libro.hoja("D Gastos diarios").anchos(6, 12, 7, 32, 10, 20, 12, 8, 34, 8, 30, 14, 10, 10, 24)
                .encabezado("Día", "Fecha", "Orden", "Artículo", "Cantidad", "Unidad", "Equivalencia", "Forma",
                        "Forma de adquisición", "Lugar", "Lugar de compra", "Valor", "Frecuencia", "Tipo de gasto",
                        "Beneficiario de la mesada");
        for (GastoDiario g : gastoDao.listarPorEncuesta(c, e.getId())) {
            RegistroDiario r = registros.get(g.getRegistroDiarioId());
            BigDecimal equivalencia = g.getEquivalenciaGr() != null ? g.getEquivalenciaGr() : g.getEquivalenciaCm3();
            d.fila(r == null ? null : r.getNumeroDia(), r == null ? null : Formato.fecha(r.getFecha()), g.getNumeroOrden(),
                    g.getNombreArticulo(), g.getCantidad(),
                    g.getUnidadMedidaOtra() != null ? g.getUnidadMedidaOtra()
                            : g.getUnidadMedida() == null ? null : g.getUnidadMedida().getDescripcion(),
                    equivalencia == null ? null : equivalencia.toPlainString() + (g.getEquivalenciaGr() != null ? " g" : " cm³"),
                    g.getFormaAdquisicion() == null ? null : g.getFormaAdquisicion().getCodigo(),
                    forma(g.getFormaAdquisicion(), g.getFormaAdquisicionOtra()),
                    g.getLugarCompraId(), g.getLugarCompraId() == null ? null : lugares.get(g.getLugarCompraId()),
                    LibroExcel.pesos(g.getValor()),
                    g.getFrecuenciaCompra() == null ? null : g.getFrecuenciaCompra().getCodigo(),
                    g.getTipoGasto() == null ? null : g.getTipoGasto().getCodigo(),
                    g.getBeneficiarioMesada());
        }

        LibroExcel.Hoja d1 = libro.hoja("D1 Comidas fuera del hogar").anchos(6, 12, 7, 32, 26, 10, 8, 34, 8, 30, 14, 10, 10, 12)
                .encabezado("Día", "Fecha", "Orden", "Descripción", "Tipo de comida", "Cantidad", "Forma",
                        "Forma de adquisición", "Lugar", "Lugar de compra", "Valor", "Frecuencia", "Tipo de gasto",
                        "A domicilio");
        for (ComidaFueraHogar f : comidaDao.listarPorEncuesta(c, e.getId())) {
            RegistroDiario r = registros.get(f.getRegistroDiarioId());
            d1.fila(r == null ? null : r.getNumeroDia(), r == null ? null : Formato.fecha(r.getFecha()), f.getNumeroOrden(),
                    f.getDescripcion(), f.getTipoComida() == null ? null : f.getTipoComida().getDescripcion(),
                    f.getCantidad(),
                    f.getFormaAdquisicion() == null ? null : f.getFormaAdquisicion().getCodigo(),
                    forma(f.getFormaAdquisicion(), f.getFormaAdquisicionOtra()),
                    f.getLugarCompraId(), f.getLugarCompraId() == null ? null : lugares.get(f.getLugarCompraId()),
                    LibroExcel.pesos(f.getValor()),
                    f.getFrecuenciaCompra() == null ? null : f.getFrecuenciaCompra().getCodigo(),
                    f.getTipoGasto() == null ? null : f.getTipoGasto().getCodigo(),
                    f.isADomicilio() ? "Sí" : "No");
        }
        return libro;
    }

    private Optional<String> buscarUsuario(Connection c, Long usuarioId) {
        try {
            return usuarioId == null ? Optional.empty()
                    : usuarioDao.buscarPorId(c, usuarioId).map(Usuario::getNombreCompleto);
        } catch (SQLException ex) {
            throw new DatosException("Error al consultar el usuario: " + ex.getMessage(), ex);
        }
    }

    private static String forma(FormaAdquisicion forma, String otra) {
        if (forma == null) {
            return null;
        }
        return forma == FormaAdquisicion.OTRA && otra != null ? "Otra: " + otra : forma.getDescripcion();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Encuesta exigirPendiente(Connection c, long encuestaId, String accion) throws SQLException {
        Encuesta e = Validar.encontrado(encuestaDao.buscarPorId(c, encuestaId), "No existe la encuesta " + encuestaId + ".");
        if (e.getEstado() != EstadoEncuesta.PENDIENTE_APROBACION) {
            throw new ReglaNegocioException("Solo se puede " + accion + " un caso pendiente de aprobación; "
                    + Formato.codigo("E", encuestaId) + " está «" + e.getEstado().getDescripcion() + "».");
        }
        return e;
    }

    private AuditoriaEncuesta registrar(Connection c, long encuestaId, AccionAuditoria accion, String observacion)
            throws SQLException {
        AuditoriaEncuesta registro = new AuditoriaEncuesta(encuestaId, accion, observacion);
        registro.setCreatedBy(Sesion.idUsuario());
        return auditoriaDao.insertar(c, registro);
    }

    /** Orden de la bandeja: pendientes de aprobación, rechazadas, aprobadas. */
    private static int orden(EstadoEncuesta estado) {
        return estado == EstadoEncuesta.PENDIENTE_APROBACION ? 0 : estado == EstadoEncuesta.RECHAZADA ? 1 : 2;
    }

    private static boolean esCero(BigDecimal valor) {
        return valor == null || valor.signum() == 0;
    }

    private static String plural(long n, String uno, String varios) {
        return n + " " + (n == 1 ? uno : varios);
    }

    /** 3, 9 y 12 */
    private static String lista(List<String> elementos) {
        if (elementos.size() == 1) {
            return elementos.get(0);
        }
        return String.join(", ", elementos.subList(0, elementos.size() - 1)) + " y " + elementos.get(elementos.size() - 1);
    }

    /** Los primeros elementos y, si hay más, «y N más». */
    private static String ejemplos(List<String> elementos) {
        if (elementos.size() <= MAXIMO_EJEMPLOS) {
            return lista(elementos);
        }
        return String.join(", ", elementos.subList(0, MAXIMO_EJEMPLOS)) + " y " + (elementos.size() - MAXIMO_EJEMPLOS) + " más";
    }

    /** Renglón del capítulo D o D1, con lo que necesitan las reglas. */
    private static final class Renglon {
        private final String capitulo;
        private final Integer dia;
        private final Integer orden;
        private final FormaAdquisicion forma;
        private final BigDecimal valor;
        private final TipoGasto tipoGasto;
        private final Long lugarCompraId;

        private Renglon(String capitulo, Integer dia, Integer orden, FormaAdquisicion forma, BigDecimal valor,
                        TipoGasto tipoGasto, Long lugarCompraId) {
            this.capitulo = capitulo;
            this.dia = dia;
            this.orden = orden;
            this.forma = forma;
            this.valor = valor;
            this.tipoGasto = tipoGasto;
            this.lugarCompraId = lugarCompraId;
        }

        /** día 10, D renglón 4 */
        @Override
        public String toString() {
            return "día " + dia + ", " + capitulo + " renglón " + orden;
        }
    }
}
