package com.udi.config;

import com.udi.exception.DatosException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Punto único de acceso a la base de datos Oracle.
 *
 * La aplicación es de escritorio y la usa una persona a la vez, así que se
 * mantiene una sola conexión abierta durante toda la sesión (se vuelve a
 * abrir si se cae). Abrir una conexión por cada operación agota los procesos
 * de Oracle XE (error ORA-12519).
 *
 * Los servicios no usan la conexión directamente: usan {@link #consultar}
 * para lecturas y {@link #enTransaccion} para escrituras, que se encargan del
 * commit y el rollback. Al salir de la aplicación se llama a {@link #cerrar}.
 */
public final class ConexionBD {

    private static final String ARCHIVO = "/com/udi/config/db.properties";
    private static final int SEGUNDOS_VALIDACION = 2;
    private static final Properties PROPIEDADES = cargarPropiedades();

    private static Connection conexion;

    private ConexionBD() {
    }

    /** Operación que recibe la conexión abierta y devuelve un resultado. */
    @FunctionalInterface
    public interface OperacionBD<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    /** Ejecuta una operación de solo lectura. */
    public static synchronized <T> T consultar(OperacionBD<T> operacion) {
        try {
            return operacion.ejecutar(obtener());
        } catch (SQLException e) {
            throw new DatosException("Error al consultar la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Ejecuta una operación dentro de una transacción. Si la operación termina
     * bien se hace commit; si lanza cualquier excepción se hace rollback y la
     * excepción se propaga (las {@code ReglaNegocioException} llegan intactas).
     */
    public static synchronized <T> T enTransaccion(OperacionBD<T> operacion) {
        try {
            Connection c = obtener();
            c.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatosException("Error al guardar en la base de datos: " + e.getMessage(), e);
        }
    }

    public static synchronized void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                // La aplicación se está cerrando: no hay nada más que hacer
            }
            conexion = null;
        }
    }

    private static Connection obtener() throws SQLException {
        if (conexion == null || conexion.isClosed() || !conexion.isValid(SEGUNDOS_VALIDACION)) {
            cerrar();
            conexion = DriverManager.getConnection(valor("db.url"), valor("db.usuario"), valor("db.contrasena"));
        }
        return conexion;
    }

    private static String valor(String clave) {
        return System.getProperty(clave, PROPIEDADES.getProperty(clave));
    }

    private static Properties cargarPropiedades() {
        Properties propiedades = new Properties();
        try (InputStream entrada = ConexionBD.class.getResourceAsStream(ARCHIVO)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró el archivo " + ARCHIVO);
            }
            propiedades.load(entrada);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo " + ARCHIVO, e);
        }
        return propiedades;
    }
}
