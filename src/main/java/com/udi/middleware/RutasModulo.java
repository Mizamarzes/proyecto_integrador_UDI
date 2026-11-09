package com.udi.middleware;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Relación entre las vistas y los módulos de la tabla modulo (por su slug),
 * como el moduleRoutes de una API. Gana el prefijo más largo que coincida; una
 * vista pide alguno de los slugs de su prefijo.
 *
 * Las constantes son los mismos conjuntos que usan los servicios con
 * {@code Sesion.exigirModulo}, para que la vista y la operación pidan lo mismo.
 */
public final class RutasModulo {

    public static final String[] USUARIOS = {"usuarios"};
    public static final String[] ROLES = {"roles"};
    public static final String[] VIVIENDAS = {"viviendas"};
    public static final String[] HOGARES = {"hogares"};
    public static final String[] VISITAS = {"visitas"};
    public static final String[] CARACTERISTICAS = {"caracteristicas-gasto"};
    public static final String[] GASTOS_DIARIOS = {"gastos-diarios"};
    public static final String[] COMIDAS = {"comidas-fuera-hogar"};
    /** Captura de la línea de 14 días: gastos (D) y comidas fuera del hogar (D1). */
    public static final String[] LINEA_14_DIAS = {"gastos-diarios", "comidas-fuera-hogar"};
    /** La encuesta: el crítico la revisa (encuestas) y el encuestador la diligencia (captura). */
    public static final String[] ENCUESTAS = {"encuestas", "visitas", "caracteristicas-gasto",
            "gastos-diarios", "comidas-fuera-hogar"};

    private static final String ENCUESTAS_PARCIALES = "modulos/encuestas/partials/";
    private static final Map<String, String[]> RUTAS = new LinkedHashMap<>();

    static {
        ruta("modulos/usuarios/", USUARIOS);
        ruta("modulos/roles/", ROLES);

        ruta("modulos/viviendas/", "viviendas", "hogares");
        ruta("modulos/viviendas/partials/vivienda_form", VIVIENDAS);
        ruta("modulos/viviendas/partials/hogar_form", HOGARES);

        // Lista, detalle, Cap. A, cierre y nueva encuesta
        ruta("modulos/encuestas/", ENCUESTAS);
        // Pestañas: se ven con el módulo de encuestas (revisión) o con el de su captura
        ruta(ENCUESTAS_PARCIALES + "encuesta_visitas", "encuestas", "visitas");
        ruta(ENCUESTAS_PARCIALES + "encuesta_capitulo_c", "encuestas", "caracteristicas-gasto");
        ruta(ENCUESTAS_PARCIALES + "encuesta_gastos", "encuestas", "gastos-diarios", "comidas-fuera-hogar");
        // Formularios: solo con el módulo de captura
        ruta(ENCUESTAS_PARCIALES + "visita_form", VISITAS);
        ruta(ENCUESTAS_PARCIALES + "renglon_form", LINEA_14_DIAS);
    }

    private RutasModulo() {
    }

    private static void ruta(String prefijo, String... slugs) {
        RUTAS.put(prefijo, slugs);
    }

    /** Slugs que permiten abrir la vista; vacío si la vista no pertenece a ningún módulo. */
    public static String[] slugsDe(String vista) {
        String[] slugs = new String[0];
        int largo = -1;
        for (Map.Entry<String, String[]> r : RUTAS.entrySet()) {
            if (vista.startsWith(r.getKey()) && r.getKey().length() > largo) {
                slugs = r.getValue();
                largo = r.getKey().length();
            }
        }
        return slugs;
    }
}
