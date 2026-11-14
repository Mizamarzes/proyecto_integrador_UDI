package com.udi.config;

import com.udi.exception.DatosException;
import com.udi.exception.ReglaNegocioException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

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
 *
 * Usuario de Oracle: la aplicación puede conectarse con un usuario que no es
 * el dueño de las tablas (enph_app, ver docs/db/seguridad/usuarios_roles.sql).
 * En ese caso {@code db.esquema} indica el dueño y cada conexión nueva hace
 * {@code ALTER SESSION SET CURRENT_SCHEMA}, así los DAO no cambian.
 *
 * Errores de la base: los que lanzan los procedimientos y disparadores con
 * RAISE_APPLICATION_ERROR (ORA-20000 a ORA-20999) son reglas de negocio y se
 * convierten en {@link ReglaNegocioException} con su mensaje; los demás
 * llegan como {@link DatosException}.
 */
public final class ConexionBD {

    private static final String ARCHIVO = "/com/udi/config/db.properties";
    private static final int SEGUNDOS_VALIDACION = 2;
    /** Nombre de usuario de Oracle válido, para armar el ALTER SESSION sin riesgo de inyección. */
    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]{0,29}");
    private static final Properties PROPIEDADES = cargarPropiedades();

    private static Connection conexion;
    /** Usuario de la aplicación que se deja en CLIENT_IDENTIFIER (para la auditoría). */
    private static Long usuarioSesion;

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
            throw traducir(e, "Error al consultar la base de datos: ");
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
            throw traducir(e, "Error al guardar en la base de datos: ");
        }
    }

    /**
     * Identifica en Oracle al usuario de la aplicación que inició sesión, con
     * el procedimiento {@code pr_iniciar_sesion} (DBMS_SESSION.SET_IDENTIFIER).
     * Así la función {@code fn_usuario_sesion} y los disparadores de auditoría
     * saben quién modificó o eliminó cada fila. Con {@code null} (cierre de
     * sesión) se llama a {@code pr_cerrar_sesion}. Si la conexión se vuelve a
     * abrir, el usuario se identifica de nuevo.
     */
    public static synchronized void identificarUsuario(Long usuarioId) {
        usuarioSesion = usuarioId;
        try {
            if (usuarioId != null) {
                aplicarIdentificador(obtener());
            } else if (conexion != null && !conexion.isClosed()) {
                try (CallableStatement cs = conexion.prepareCall("{call pr_cerrar_sesion}")) {
                    cs.execute();
                }
            }
        } catch (SQLException e) {
            if (usuarioId != null) {
                throw traducir(e, "No se pudo identificar la sesión en la base de datos: ");
            }
            // Al cerrar sesión no importa: una conexión nueva empieza sin identificador
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
            Connection nueva = DriverManager.getConnection(valor("db.url"), valor("db.usuario"), valor("db.contrasena"));
            try {
                prepararSesion(nueva);
            } catch (SQLException e) {
                nueva.close();
                throw e;
            }
            conexion = nueva;
        }
        return conexion;
    }

    /** Esquema por defecto (si se configuró) y usuario de la aplicación. */
    private static void prepararSesion(Connection c) throws SQLException {
        String esquema = valor("db.esquema");
        if (esquema != null && !esquema.trim().isEmpty()) {
            if (!IDENTIFICADOR.matcher(esquema.trim()).matches()) {
                throw new IllegalStateException("db.esquema no es un nombre de usuario de Oracle válido: " + esquema);
            }
            try (Statement st = c.createStatement()) {
                st.execute("ALTER SESSION SET CURRENT_SCHEMA = " + esquema.trim().toUpperCase());
            }
        }
        if (usuarioSesion != null) {
            aplicarIdentificador(c);
        }
    }

    private static void aplicarIdentificador(Connection c) throws SQLException {
        try (CallableStatement cs = c.prepareCall("{call pr_iniciar_sesion(?)}")) {
            cs.setLong(1, usuarioSesion);
            cs.execute();
        }
    }

    /**
     * ORA-20000 a ORA-20999 (RAISE_APPLICATION_ERROR de un procedimiento o
     * disparador) es una regla de negocio: se muestra solo la primera línea
     * del mensaje, sin el código ni la pila ORA-06512. Lo demás es un error
     * de datos.
     */
    private static RuntimeException traducir(SQLException e, String prefijo) {
        int codigo = e.getErrorCode();
        if (codigo >= 20000 && codigo <= 20999 && e.getMessage() != null) {
            String mensaje = e.getMessage().split("\\R", 2)[0]
                    .replaceFirst("^ORA-\\d{5}:\\s*", "").trim();
            return new ReglaNegocioException(mensaje);
        }
        return new DatosException(prefijo + e.getMessage(), e);
    }

    private static String valor(String clave) {
        return System.getProperty(clave, PROPIEDADES.getProperty(clave));
    }

    private static Properties cargarPropiedades() {
        Properties propiedades = new Properties();
        try (InputStream entrada = ConexionBD.class.getResourceAsStream(ARCHIVO)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró el archivo " + ARCHIVO
                        + ". Copie db.properties.example como db.properties en src/main/resources/com/udi/config/.");
            }
            propiedades.load(entrada);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo " + ARCHIVO, e);
        }
        return propiedades;
    }
}
