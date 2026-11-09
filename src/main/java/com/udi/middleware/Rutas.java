package com.udi.middleware;

import com.udi.exception.AccesoDenegadoException;
import javafx.scene.Node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "Router" de la aplicación: a cada prefijo de vista le asigna su cadena de
 * middlewares, igual que una API registra
 * {@code router.get('/chats', isAuth, ModulePermissionMiddleware, controller)}.
 * Gana el prefijo más largo; "" es la regla por defecto.
 *
 * {@code App.cargar} llama a {@link #exigir} antes de cargar cualquier vista,
 * así que pantallas, pestañas, formularios y modales quedan protegidos sin
 * validar en cada controlador. Los controladores usan {@link #permitir} para
 * mostrar solo los botones cuyas vistas se pueden abrir. Los servicios siguen
 * validando por su cuenta con {@code Sesion.exigirModulo}, igual que una API
 * valida aunque el frontend oculte el botón.
 */
public final class Rutas {

    private static final Middleware AUTENTICACION = new AutenticacionMiddleware();
    private static final Middleware PERMISO_MODULO = new PermisoModuloMiddleware();

    private static final Map<String, Middleware[]> CADENAS = new LinkedHashMap<>();

    static {
        ruta("modulos/auth/");
        ruta("modulos/usuarios/", AUTENTICACION, PERMISO_MODULO);
        ruta("modulos/roles/", AUTENTICACION, PERMISO_MODULO);
        ruta("modulos/viviendas/", AUTENTICACION, PERMISO_MODULO);
        ruta("modulos/encuestas/", AUTENTICACION, PERMISO_MODULO);
        // Cualquier otra vista (navbar, pendiente…) solo pide sesión iniciada
        ruta("", AUTENTICACION);
    }

    private Rutas() {
    }

    private static void ruta(String prefijo, Middleware... cadena) {
        CADENAS.put(prefijo, cadena);
    }

    private static Middleware[] cadenaDe(String vista) {
        Middleware[] cadena = null;
        int largo = -1;
        for (Map.Entry<String, Middleware[]> r : CADENAS.entrySet()) {
            if (vista.startsWith(r.getKey()) && r.getKey().length() > largo) {
                cadena = r.getValue();
                largo = r.getKey().length();
            }
        }
        return cadena;
    }

    /** Ejecuta la cadena de la vista; lanza {@link AccesoDenegadoException} en el primer middleware que falle. */
    public static void exigir(String vista) {
        Middleware[] cadena = cadenaDe(vista);
        if (cadena == null) {
            throw new AccesoDenegadoException("La sección " + vista + " no existe.");
        }
        for (Middleware middleware : cadena) {
            middleware.manejar(vista);
        }
    }

    public static boolean puedeAbrir(String vista) {
        try {
            exigir(vista);
            return true;
        } catch (AccesoDenegadoException e) {
            return false;
        }
    }

    /** Muestra el nodo (botón, pestaña…) solo si la sesión puede abrir la vista a la que lleva. */
    public static boolean permitir(Node nodo, String vista) {
        boolean visible = puedeAbrir(vista);
        nodo.setVisible(visible);
        nodo.setManaged(visible);
        return visible;
    }
}
