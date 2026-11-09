package com.udi.middleware;

import com.udi.exception.AccesoDenegadoException;
import com.udi.util.Sesion;

/** Exige que haya una sesión iniciada (equivale a isAuth en una API). */
public final class AutenticacionMiddleware implements Middleware {

    @Override
    public void manejar(String vista) {
        if (!Sesion.activa()) {
            throw new AccesoDenegadoException("Debe iniciar sesión para continuar.");
        }
    }
}
