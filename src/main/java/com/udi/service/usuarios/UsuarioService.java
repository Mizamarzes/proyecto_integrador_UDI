package com.udi.service.usuarios;

import com.udi.config.ConexionBD;
import com.udi.dao.roles.RolDAO;
import com.udi.dao.usuarios.EncuestadorDAO;
import com.udi.dao.usuarios.UsuarioDAO;
import com.udi.exception.AccesoDenegadoException;
import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.RutasModulo;
import com.udi.model.Encuestador;
import com.udi.model.Rol;
import com.udi.model.Usuario;
import com.udi.service.roles.RolService;
import com.udi.util.Seguridad;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Gestión de usuarios y encuestadores (RF-01). Exige el módulo "usuarios"
 * (el administrador siempre puede); los roles y permisos están en {@link RolService}.
 * Solo un administrador puede crear, editar o asignar usuarios con rol ADMIN.
 */
public class UsuarioService {

    private static final int LONGITUD_MINIMA_CONTRASENA = 6;

    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final RolDAO rolDao = new RolDAO();
    private final EncuestadorDAO encuestadorDao = new EncuestadorDAO();

    // ------------------------------------------------------------------
    // Usuarios
    // ------------------------------------------------------------------

    public List<Usuario> listarUsuarios() {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        return ConexionBD.consultar(usuarioDao::listar);
    }

    /** Filtra por estado: true activos, false inactivos, null todos. */
    public List<Usuario> listarUsuarios(Boolean activo) {
        if (activo == null) {
            return listarUsuarios();
        }
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        return ConexionBD.consultar(c -> usuarioDao.listarPorActivo(c, activo));
    }

    /** Usuario completo (incluye el hash de la contraseña): solo con el módulo de usuarios. */
    public Optional<Usuario> buscarUsuario(long id) {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        return ConexionBD.consultar(c -> usuarioDao.buscarPorId(c, id));
    }

    /** Nombre completo del usuario, para mostrarlo en otras pantallas (cualquier rol con sesión). */
    public String nombreUsuario(long id) {
        Sesion.exigirSesion();
        return ConexionBD.consultar(c -> usuarioDao.buscarPorId(c, id))
                .map(Usuario::getNombreCompleto).orElse("");
    }

    /**
     * Crea el usuario guardando el hash de la contraseña. Si su rol es
     * ENCUESTADOR, también crea su registro en la tabla encuestador.
     */
    public Usuario crearUsuario(Usuario usuario, String contrasena) {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        validarContrasena(contrasena);
        return ConexionBD.enTransaccion(c -> {
            Rol rol = validarUsuario(c, usuario);
            usuario.setId(null);
            usuario.setPassword(Seguridad.hash(contrasena));
            usuario.setCreatedBy(Sesion.idUsuario());
            usuarioDao.insertar(c, usuario);
            if (Rol.ENCUESTADOR.equals(rol.getNombre())) {
                crearEncuestadorSiNoExiste(c, usuario.getId());
            }
            return usuario;
        });
    }

    /** Actualiza los datos del usuario. La contraseña se cambia con {@link #cambiarContrasena}. */
    public Usuario actualizarUsuario(Usuario usuario) {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        Validar.requerido(usuario.getId(), "Id del usuario");
        return ConexionBD.enTransaccion(c -> {
            Validar.encontrado(usuarioDao.buscarPorId(c, usuario.getId()), "No existe el usuario " + usuario.getId() + ".");
            validarNoTocaAdmin(c, usuario.getId());
            Rol rol = validarUsuario(c, usuario);
            usuarioDao.actualizar(c, usuario);
            if (Rol.ENCUESTADOR.equals(rol.getNombre())) {
                crearEncuestadorSiNoExiste(c, usuario.getId());
            }
            return usuario;
        });
    }

    /** Quien tiene el módulo de usuarios puede cambiar cualquier contraseña; los demás, solo la suya. */
    public void cambiarContrasena(long usuarioId, String nuevaContrasena) {
        Usuario actual = Sesion.exigirSesion();
        boolean gestiona = Sesion.esAdmin() || Sesion.puedeAccederAlguno(RutasModulo.USUARIOS);
        if (!gestiona && actual.getId() != usuarioId) {
            throw new ReglaNegocioException("Solo puede cambiar su propia contraseña.");
        }
        validarContrasena(nuevaContrasena);
        ConexionBD.enTransaccion(c -> {
            if (actual.getId() != usuarioId) {
                validarNoTocaAdmin(c, usuarioId);
            }
            return usuarioDao.actualizarPassword(c, usuarioId, Seguridad.hash(nuevaContrasena));
        });
    }

    /** Activa o desactiva el acceso de un usuario sin borrarlo. */
    public void cambiarActivo(long usuarioId, boolean activo) {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        if (!activo && Sesion.idUsuario() == usuarioId) {
            throw new ReglaNegocioException("No puede desactivar su propio usuario.");
        }
        ConexionBD.enTransaccion(c -> {
            validarNoTocaAdmin(c, usuarioId);
            return usuarioDao.actualizarActivo(c, usuarioId, activo);
        });
    }

    /**
     * Elimina el usuario. Si ya registró datos, la base de datos no lo permite
     * por las llaves foráneas: en ese caso se debe desactivar con {@link #cambiarActivo}.
     */
    public boolean eliminarUsuario(long usuarioId) {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        if (Sesion.idUsuario() == usuarioId) {
            throw new ReglaNegocioException("No puede eliminar su propio usuario.");
        }
        return ConexionBD.enTransaccion(c -> {
            validarNoTocaAdmin(c, usuarioId);
            Optional<Encuestador> encuestador = encuestadorDao.buscarPorUsuario(c, usuarioId);
            if (encuestador.isPresent()) {
                encuestadorDao.eliminar(c, encuestador.get().getId());
            }
            return usuarioDao.eliminar(c, usuarioId);
        });
    }

    private Rol validarUsuario(Connection c, Usuario usuario) throws SQLException {
        Validar.requerido(usuario.getEmail(), "Correo");
        Validar.condicion(usuario.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), "El correo no es válido.");
        Validar.requerido(usuario.getNombre(), "Nombre");
        Validar.requerido(usuario.getApellido(), "Apellido");
        Validar.requerido(usuario.getRolId(), "Rol");
        usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        Optional<Usuario> mismoCorreo = usuarioDao.buscarPorEmail(c, usuario.getEmail());
        if (mismoCorreo.isPresent() && !mismoCorreo.get().getId().equals(usuario.getId())) {
            throw new ReglaNegocioException("Ya existe un usuario con el correo " + usuario.getEmail() + ".");
        }
        Rol rol = Validar.encontrado(rolDao.buscarPorId(c, usuario.getRolId()), "No existe el rol " + usuario.getRolId() + ".");
        if (Rol.ADMIN.equals(rol.getNombre()) && !Sesion.esAdmin()) {
            throw new AccesoDenegadoException("Solo un administrador puede asignar el rol " + Rol.ADMIN + ".");
        }
        return rol;
    }

    /**
     * Quien tiene el módulo de usuarios sin ser administrador no puede cambiar
     * a un administrador; si pudiera, podría darse acceso total.
     */
    private void validarNoTocaAdmin(Connection c, long usuarioId) throws SQLException {
        if (Sesion.esAdmin()) {
            return;
        }
        Optional<Usuario> usuario = usuarioDao.buscarPorId(c, usuarioId);
        if (usuario.isPresent()) {
            Optional<Rol> rol = rolDao.buscarPorId(c, usuario.get().getRolId());
            if (rol.isPresent() && Rol.ADMIN.equals(rol.get().getNombre())) {
                throw new AccesoDenegadoException("Solo un administrador puede modificar a otro administrador.");
            }
        }
    }

    private static void validarContrasena(String contrasena) {
        Validar.requerido(contrasena, "Contraseña");
        Validar.condicion(contrasena.length() >= LONGITUD_MINIMA_CONTRASENA,
                "La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres.");
    }

    // ------------------------------------------------------------------
    // Encuestadores
    // ------------------------------------------------------------------

    public List<Encuestador> listarEncuestadores() {
        return ConexionBD.consultar(encuestadorDao::listar);
    }

    public Optional<Encuestador> buscarEncuestador(long id) {
        return ConexionBD.consultar(c -> encuestadorDao.buscarPorId(c, id));
    }

    public Optional<Encuestador> buscarEncuestadorPorUsuario(long usuarioId) {
        return ConexionBD.consultar(c -> encuestadorDao.buscarPorUsuario(c, usuarioId));
    }

    /** Registra como encuestador a un usuario existente. */
    public Encuestador crearEncuestador(long usuarioId) {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(usuarioDao.existe(c, usuarioId), "No existe el usuario " + usuarioId + ".");
            return crearEncuestadorSiNoExiste(c, usuarioId);
        });
    }

    /** Elimina el encuestador. Falla si ya tiene encuestas o registros asociados. */
    public boolean eliminarEncuestador(long encuestadorId) {
        Sesion.exigirModulo(RutasModulo.USUARIOS);
        return ConexionBD.enTransaccion(c -> encuestadorDao.eliminar(c, encuestadorId));
    }

    private Encuestador crearEncuestadorSiNoExiste(Connection c, long usuarioId) throws SQLException {
        Optional<Encuestador> existente = encuestadorDao.buscarPorUsuario(c, usuarioId);
        if (existente.isPresent()) {
            return existente.get();
        }
        Encuestador encuestador = new Encuestador();
        encuestador.setUsuarioId(usuarioId);
        encuestador.setCreatedBy(Sesion.idUsuario());
        return encuestadorDao.insertar(c, encuestador);
    }
}
