package com.udi.util;

import com.udi.exception.ReglaNegocioException;
import com.udi.model.Modulo;
import com.udi.model.Rol;
import com.udi.model.Usuario;

import java.util.Collections;
import java.util.List;

/**
 * Usuario que inició sesión en la aplicación. Lo llena {@code AuthService} al
 * hacer login y lo usan los servicios para el campo created_by y los permisos.
 */
public final class Sesion {

    private static Usuario usuario;
    private static Rol rol;
    private static List<Modulo> modulos = Collections.emptyList();

    private Sesion() {
    }

    public static void iniciar(Usuario usuarioActual, Rol rolActual, List<Modulo> modulosPermitidos) {
        usuario = usuarioActual;
        rol = rolActual;
        modulos = Collections.unmodifiableList(modulosPermitidos);
    }

    public static void cerrar() {
        usuario = null;
        rol = null;
        modulos = Collections.emptyList();
    }

    public static boolean activa() {
        return usuario != null;
    }

    public static Usuario getUsuario() {
        return usuario;
    }

    public static Rol getRol() {
        return rol;
    }

    public static List<Modulo> getModulos() {
        return modulos;
    }

    /** Id del usuario actual, o null si no hay sesión. Se usa para created_by. */
    public static Long idUsuario() {
        return usuario == null ? null : usuario.getId();
    }

    public static boolean tieneRol(String nombreRol) {
        return rol != null && rol.getNombre().equals(nombreRol);
    }

    public static boolean esAdmin() {
        return tieneRol(Rol.ADMIN);
    }

    /** true si el rol del usuario tiene permiso sobre el módulo (por su slug). */
    public static boolean puedeAcceder(String slugModulo) {
        return modulos.stream().anyMatch(m -> m.getSlug().equals(slugModulo));
    }

    public static Usuario exigirSesion() {
        if (usuario == null) {
            throw new ReglaNegocioException("Debe iniciar sesión para realizar esta operación.");
        }
        return usuario;
    }

    public static void exigirAdmin() {
        exigirSesion();
        if (!esAdmin()) {
            throw new ReglaNegocioException("Solo un administrador puede realizar esta operación.");
        }
    }
}
