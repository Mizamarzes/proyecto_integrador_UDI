package com.udi.middleware;

import com.udi.exception.AccesoDenegadoException;
import com.udi.util.Sesion;

/**
 * Exige que el rol de la sesión tenga permiso sobre el módulo de la vista
 * (equivale a ModulePermissionMiddleware en una API). El módulo se busca por
 * la ruta en {@link RutasModulo}; los permisos de cada rol son las filas de
 * rol_modulo, cargadas en {@link Sesion} al iniciar sesión.
 *
 * El administrador entra a todo, como el super-admin de una API, para que
 * nunca quede sin acceso a la pantalla de roles y permisos.
 * Debe ir después de {@link AutenticacionMiddleware} en la cadena.
 */
public final class PermisoModuloMiddleware implements Middleware {

    @Override
    public void manejar(String vista) {
        if (Sesion.esAdmin()) {
            return;
        }
        String[] slugs = RutasModulo.slugsDe(vista);
        if (slugs.length == 0 || !Sesion.tieneModulo(slugs)) {
            throw new AccesoDenegadoException("Su rol no tiene permiso para acceder a esta sección.");
        }
    }
}
