package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.ModuloDAO;
import com.udi.dao.RolDAO;
import com.udi.dao.RolModuloDAO;
import com.udi.dao.UsuarioDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.middleware.RutasModulo;
import com.udi.model.Modulo;
import com.udi.model.Rol;
import com.udi.model.RolModulo;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Roles, módulos y permisos de cada rol sobre los módulos (tabla rol_modulo).
 * Las escrituras exigen el módulo "roles" (el administrador siempre puede).
 * El rol ADMIN tiene acceso total por el middleware de permisos, así que no se
 * edita, renombra ni elimina.
 */
public class RolService {

    private static final int LONGITUD_MAXIMA_NOMBRE = 30;

    private final RolDAO rolDao = new RolDAO();
    private final ModuloDAO moduloDao = new ModuloDAO();
    private final RolModuloDAO rolModuloDao = new RolModuloDAO();
    private final UsuarioDAO usuarioDao = new UsuarioDAO();

    // ------------------------------------------------------------------
    // Roles
    // ------------------------------------------------------------------

    /** Cualquier sesión puede listarlos (el formulario de usuarios los muestra). */
    public List<Rol> listarRoles() {
        Sesion.exigirSesion();
        return ConexionBD.consultar(rolDao::listar);
    }

    public Optional<Rol> buscarRol(long id) {
        Sesion.exigirSesion();
        return ConexionBD.consultar(c -> rolDao.buscarPorId(c, id));
    }

    /** Crea el rol sin módulos; los permisos se asignan con {@link #guardarPermisos}. */
    public Rol crearRol(Rol rol) {
        Sesion.exigirModulo(RutasModulo.ROLES);
        return ConexionBD.enTransaccion(c -> {
            validarRol(c, rol);
            rol.setId(null);
            rol.setCreatedBy(Sesion.idUsuario());
            return rolDao.insertar(c, rol);
        });
    }

    public Rol actualizarRol(Rol rol) {
        Sesion.exigirModulo(RutasModulo.ROLES);
        Validar.requerido(rol.getId(), "Id del rol");
        return ConexionBD.enTransaccion(c -> {
            Rol actual = Validar.encontrado(rolDao.buscarPorId(c, rol.getId()), "No existe el rol " + rol.getId() + ".");
            validarNoEsAdmin(actual);
            validarRol(c, rol);
            rolDao.actualizar(c, rol);
            return rol;
        });
    }

    /** Elimina el rol y sus permisos (en cascada). Falla si algún usuario tiene ese rol. */
    public boolean eliminarRol(long rolId) {
        Sesion.exigirModulo(RutasModulo.ROLES);
        return ConexionBD.enTransaccion(c -> {
            Rol actual = Validar.encontrado(rolDao.buscarPorId(c, rolId), "No existe el rol " + rolId + ".");
            validarNoEsAdmin(actual);
            Validar.condicion(usuarioDao.listarPorRol(c, rolId).isEmpty(),
                    "No se puede eliminar el rol porque hay usuarios que lo tienen asignado.");
            return rolDao.eliminar(c, rolId);
        });
    }

    private void validarRol(Connection c, Rol rol) throws SQLException {
        Validar.requerido(rol.getNombre(), "Nombre del rol");
        rol.setNombre(rol.getNombre().trim().toUpperCase().replaceAll("\\s+", "_"));
        Validar.condicion(rol.getNombre().length() <= LONGITUD_MAXIMA_NOMBRE,
                "El nombre del rol puede tener máximo " + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        Validar.condicion(rol.getNombre().matches("^[A-ZÁÉÍÓÚÑ0-9_-]+$"),
                "El nombre del rol solo puede tener letras, números, guiones y guiones bajos.");
        Optional<Rol> mismoNombre = rolDao.buscarPorNombre(c, rol.getNombre());
        if (mismoNombre.isPresent() && !mismoNombre.get().getId().equals(rol.getId())) {
            throw new ReglaNegocioException("Ya existe el rol " + rol.getNombre() + ".");
        }
    }

    private static void validarNoEsAdmin(Rol rol) {
        if (Rol.ADMIN.equals(rol.getNombre())) {
            throw new ReglaNegocioException("El rol " + Rol.ADMIN + " tiene acceso total y no se puede modificar.");
        }
    }

    // ------------------------------------------------------------------
    // Módulos
    // ------------------------------------------------------------------

    public List<Modulo> listarModulos() {
        Sesion.exigirSesion();
        return ConexionBD.consultar(moduloDao::listar);
    }

    public Optional<Modulo> buscarModulo(long id) {
        Sesion.exigirSesion();
        return ConexionBD.consultar(c -> moduloDao.buscarPorId(c, id));
    }

    public List<Modulo> listarModulosDeRol(long rolId) {
        Sesion.exigirSesion();
        return ConexionBD.consultar(c -> moduloDao.listarPorRol(c, rolId));
    }

    /** Los módulos los usa el código (los slugs de RutasModulo), así que solo el administrador los cambia. */
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
    // Permisos (rol_modulo)
    // ------------------------------------------------------------------

    /** Ids de los módulos de cada rol, para pintar la matriz de permisos. */
    public Map<Long, Set<Long>> matrizPermisos() {
        Sesion.exigirModulo(RutasModulo.ROLES);
        return ConexionBD.consultar(c -> {
            Map<Long, Set<Long>> matriz = new HashMap<>();
            for (RolModulo permiso : rolModuloDao.listar(c)) {
                matriz.computeIfAbsent(permiso.getRolId(), k -> new HashSet<>()).add(permiso.getModuloId());
            }
            return matriz;
        });
    }

    /**
     * Deja a cada rol con exactamente los módulos indicados: inserta los que
     * faltan y borra los que sobran, todo en una transacción. El rol ADMIN se
     * ignora. Si cambian los permisos del rol de la sesión, se recargan.
     */
    public void guardarPermisos(Map<Long, Set<Long>> permisos) {
        Sesion.exigirModulo(RutasModulo.ROLES);
        List<Modulo> modulosSesion = ConexionBD.enTransaccion(c -> {
            for (Map.Entry<Long, Set<Long>> entrada : permisos.entrySet()) {
                long rolId = entrada.getKey();
                Rol rol = Validar.encontrado(rolDao.buscarPorId(c, rolId), "No existe el rol " + rolId + ".");
                if (Rol.ADMIN.equals(rol.getNombre())) {
                    continue;
                }
                Set<Long> nuevos = entrada.getValue();
                Set<Long> actuales = new HashSet<>();
                for (RolModulo permiso : rolModuloDao.listarPorRol(c, rolId)) {
                    actuales.add(permiso.getModuloId());
                }
                for (Long moduloId : nuevos) {
                    if (!actuales.contains(moduloId)) {
                        Validar.condicion(moduloDao.existe(c, moduloId), "No existe el módulo " + moduloId + ".");
                        RolModulo permiso = new RolModulo();
                        permiso.setRolId(rolId);
                        permiso.setModuloId(moduloId);
                        permiso.setCreatedBy(Sesion.idUsuario());
                        rolModuloDao.insertar(c, permiso);
                    }
                }
                for (Long moduloId : actuales) {
                    if (!nuevos.contains(moduloId)) {
                        rolModuloDao.eliminarPorRolYModulo(c, rolId, moduloId);
                    }
                }
            }
            Long rolSesion = Sesion.getRol().getId();
            return permisos.containsKey(rolSesion) ? moduloDao.listarPorRol(c, rolSesion) : null;
        });
        if (modulosSesion != null) {
            Sesion.recargarModulos(modulosSesion);
        }
    }
}
