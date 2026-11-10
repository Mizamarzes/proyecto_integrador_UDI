package com.udi.middleware;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Relación entre las vistas y los módulos de la tabla modulo (por su slug),
 * como el moduleRoutes de una API. Gana el prefijo más largo que coincida.
 *
 * Cada módulo agrupa todas sus pantallas: «viviendas» incluye las viviendas y
 * sus hogares, y «encuestas» incluye la encuesta con todas sus pestañas
 * (control de visitas, capítulo C, gastos diarios y comidas fuera del hogar).
 *
 * Las constantes son las mismas que usan los servicios con
 * {@code Sesion.exigirModulo}, para que la vista y la operación pidan lo mismo.
 */
public final class RutasModulo {

    public static final String[] USUARIOS = {"usuarios"};
    public static final String[] ROLES = {"roles"};
    /** Viviendas y hogares. */
    public static final String[] VIVIENDAS = {"viviendas"};
    /** La encuesta y su captura: visitas, capítulo C, gastos diarios (D) y comidas fuera del hogar (D1). */
    public static final String[] ENCUESTAS = {"encuestas"};
    public static final String[] REPORTES = {"reportes"};

    private static final Map<String, String[]> RUTAS = new LinkedHashMap<>();

    static {
        ruta("modulos/usuarios/", USUARIOS);
        ruta("modulos/roles/", ROLES);
        ruta("modulos/viviendas/", VIVIENDAS);
        ruta("modulos/encuestas/", ENCUESTAS);
        ruta("modulos/reportes/", REPORTES);
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
