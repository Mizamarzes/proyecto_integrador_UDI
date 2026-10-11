package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.EncuestadorDAO;
import com.udi.dao.ModuloDAO;
import com.udi.dao.RolDAO;
import com.udi.dao.RolModuloDAO;
import com.udi.dao.UsuarioDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.Encuestador;
import com.udi.model.Modulo;
import com.udi.model.Rol;
import com.udi.model.RolModulo;
import com.udi.model.Usuario;
import com.udi.util.Seguridad;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Gestión de usuarios, roles, módulos, permisos y encuestadores (RF-01).
 * Las operaciones que modifican datos solo las puede hacer un administrador.
 */
public class UsuarioService {

    private static final int LONGITUD_MINIMA_CONTRASENA = 6;

    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final RolDAO rolDao = new RolDAO();
    private final ModuloDAO moduloDao = new ModuloDAO();
    private final RolModuloDAO rolModuloDao = new RolModuloDAO();
    private final EncuestadorDAO encuestadorDao = new EncuestadorDAO();

    // ------------------------------------------------------------------
    // Usuarios
    // ------------------------------------------------------------------

    public List<Usuario> listarUsuarios() {
        return ConexionBD.consultar(usuarioDao::listar);
    }

    public Optional<Usuario> buscarUsuario(long id) {
        return ConexionBD.consultar(c -> usuarioDao.buscarPorId(c, id));
    }

    /**
     * Crea el usuario guardando el hash de la contraseña. Si su rol es
     * ENCUESTADOR, también crea su registro en la tabla encuestador.
     */
    public Usuario crearUsuario(Usuario usuario, String contrasena) {
        Sesion.exigirAdmin();
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
        Sesion.exigirAdmin();
        Validar.requerido(usuario.getId(), "Id del usuario");
        return ConexionBD.enTransaccion(c -> {
            Validar.encontrado(usuarioDao.buscarPorId(c, usuario.getId()), "No existe el usuario " + usuario.getId() + ".");
            Rol rol = validarUsuario(c, usuario);
            usuarioDao.actualizar(c, usuario);
            if (Rol.ENCUESTADOR.equals(rol.getNombre())) {
                crearEncuestadorSiNoExiste(c, usuario.getId());
            }
            return usuario;
        });
    }

    /** Un administrador puede cambiar cualquier contraseña; los demás, solo la suya. */
    public void cambiarContrasena(long usuarioId, String nuevaContrasena) {
        Usuario actual = Sesion.exigirSesion();
        if (!Sesion.esAdmin() && actual.getId() != usuarioId) {
            throw new ReglaNegocioException("Solo puede cambiar su propia contraseña.");
        }
        validarContrasena(nuevaContrasena);
        ConexionBD.enTransaccion(c -> usuarioDao.actualizarPassword(c, usuarioId, Seguridad.hash(nuevaContrasena)));
    }

    /** Activa o desactiva el acceso de un usuario sin borrarlo. */
    public void cambiarActivo(long usuarioId, boolean activo) {
        Sesion.exigirAdmin();
        if (!activo && Sesion.idUsuario() == usuarioId) {
            throw new ReglaNegocioException("No puede desactivar su propio usuario.");
        }
        ConexionBD.enTransaccion(c -> usuarioDao.actualizarActivo(c, usuarioId, activo));
    }

    /**
     * Elimina el usuario. Si ya registró datos, la base de datos no lo permite
     * por las llaves foráneas: en ese caso se debe desactivar con {@link #cambiarActivo}.
     */
    public boolean eliminarUsuario(long usuarioId) {
        Sesion.exigirAdmin();
        if (Sesion.idUsuario() == usuarioId) {
            throw new ReglaNegocioException("No puede eliminar su propio usuario.");
        }
        return ConexionBD.enTransaccion(c -> {
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
        return Validar.encontrado(rolDao.buscarPorId(c, usuario.getRolId()), "No existe el rol " + usuario.getRolId() + ".");
    }

    private static void validarContrasena(String contrasena) {
        Validar.requerido(contrasena, "Contraseña");
        Validar.condicion(contrasena.length() >= LONGITUD_MINIMA_CONTRASENA,
                "La contraseña debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres.");
    }

    // ------------------------------------------------------------------
    // Roles
    // ------------------------------------------------------------------

    public List<Rol> listarRoles() {
        return ConexionBD.consultar(rolDao::listar);
    }

    public Optional<Rol> buscarRol(long id) {
        return ConexionBD.consultar(c -> rolDao.buscarPorId(c, id));
    }

    public Rol crearRol(Rol rol) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> {
            validarRol(c, rol);
            rol.setId(null);
            rol.setCreatedBy(Sesion.idUsuario());
            return rolDao.insertar(c, rol);
        });
    }

    public Rol actualizarRol(Rol rol) {
        Sesion.exigirAdmin();
        Validar.requerido(rol.getId(), "Id del rol");
        return ConexionBD.enTransaccion(c -> {
            validarRol(c, rol);
            Validar.condicion(rolDao.actualizar(c, rol), "No existe el rol " + rol.getId() + ".");
            return rol;
        });
    }

    /** Elimina el rol y sus permisos. Falla si algún usuario tiene ese rol. */
    public boolean eliminarRol(long rolId) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(usuarioDao.listarPorRol(c, rolId).isEmpty(),
                    "No se puede eliminar el rol porque hay usuarios que lo tienen asignado.");
            return rolDao.eliminar(c, rolId);
        });
    }

    private void validarRol(Connection c, Rol rol) throws SQLException {
        Validar.requerido(rol.getNombre(), "Nombre del rol");
        rol.setNombre(rol.getNombre().trim().toUpperCase());
        Optional<Rol> mismoNombre = rolDao.buscarPorNombre(c, rol.getNombre());
        if (mismoNombre.isPresent() && !mismoNombre.get().getId().equals(rol.getId())) {
            throw new ReglaNegocioException("Ya existe el rol " + rol.getNombre() + ".");
        }
    }

    // ------------------------------------------------------------------
    // Módulos y permisos
    // ------------------------------------------------------------------

    public List<Modulo> listarModulos() {
        return ConexionBD.consultar(moduloDao::listar);
    }

    public Optional<Modulo> buscarModulo(long id) {
        return ConexionBD.consultar(c -> moduloDao.buscarPorId(c, id));
    }

    public Modulo crearModulo(Modulo modulo) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> {
            validarModulo(c, modulo);
            modulo.setId(null);
            modulo.setCreatedBy(Sesion.idUsuario());
            return moduloDao.insertar(c, modulo);
        });
    }

    public Modulo actualizarModulo(Modulo modulo) {
        Sesion.exigirAdmin();
        Validar.requerido(modulo.getId(), "Id del módulo");
        return ConexionBD.enTransaccion(c -> {
            validarModulo(c, modulo);
            Validar.condicion(moduloDao.actualizar(c, modulo), "No existe el módulo " + modulo.getId() + ".");
            return modulo;
        });
    }

    /** Elimina el módulo; sus permisos se borran en cascada. */
    public boolean eliminarModulo(long moduloId) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> moduloDao.eliminar(c, moduloId));
    }

    public List<Modulo> listarModulosDeRol(long rolId) {
        return ConexionBD.consultar(c -> moduloDao.listarPorRol(c, rolId));
    }

    /** Da permiso a un rol sobre un módulo. Si ya lo tenía, no hace nada. */
    public void asignarModulo(long rolId, long moduloId) {
        Sesion.exigirAdmin();
        ConexionBD.enTransaccion(c -> {
            Validar.condicion(rolDao.existe(c, rolId), "No existe el rol " + rolId + ".");
            Validar.condicion(moduloDao.existe(c, moduloId), "No existe el módulo " + moduloId + ".");
            if (rolModuloDao.buscarPorRolYModulo(c, rolId, moduloId).isPresent()) {
                return null;
            }
            RolModulo permiso = new RolModulo();
            permiso.setRolId(rolId);
            permiso.setModuloId(moduloId);
            permiso.setCreatedBy(Sesion.idUsuario());
            return rolModuloDao.insertar(c, permiso);
        });
    }

    public boolean quitarModulo(long rolId, long moduloId) {
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> rolModuloDao.eliminarPorRolYModulo(c, rolId, moduloId));
    }

    private void validarModulo(Connection c, Modulo modulo) throws SQLException {
        Validar.requerido(modulo.getNombre(), "Nombre del módulo");
        Validar.requerido(modulo.getSlug(), "Slug del módulo");
        modulo.setSlug(modulo.getSlug().trim().toLowerCase());
        Validar.condicion(modulo.getSlug().matches("^[a-z0-9-]+$"),
                "El slug solo puede tener letras minúsculas, números y guiones.");
        Optional<Modulo> mismoSlug = moduloDao.buscarPorSlug(c, modulo.getSlug());
        if (mismoSlug.isPresent() && !mismoSlug.get().getId().equals(modulo.getId())) {
            throw new ReglaNegocioException("Ya existe un módulo con el slug " + modulo.getSlug() + ".");
        }
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
        Sesion.exigirAdmin();
        return ConexionBD.enTransaccion(c -> {
            Validar.condicion(usuarioDao.existe(c, usuarioId), "No existe el usuario " + usuarioId + ".");
            return crearEncuestadorSiNoExiste(c, usuarioId);
        });
    }

    /** Elimina el encuestador. Falla si ya tiene encuestas o registros asociados. */
    public boolean eliminarEncuestador(long encuestadorId) {
        Sesion.exigirAdmin();
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
