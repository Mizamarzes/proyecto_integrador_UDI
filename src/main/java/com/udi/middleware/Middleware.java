package com.udi.middleware;

/**
 * Un paso de la cadena que protege una vista, como un middleware de rutas en
 * una API: recibe la vista que se quiere abrir (su ruta dentro de ui, por
 * ejemplo "modulos/usuarios/usuario") y, si no se cumple su condición, lanza
 * {@link com.udi.exception.AccesoDenegadoException} y corta la cadena.
 *
 * Las cadenas se registran por ruta en {@link Rutas}.
 */
@FunctionalInterface
public interface Middleware {

    void manejar(String vista);
}
