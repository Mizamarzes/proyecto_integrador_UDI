package com.udi.service.auth;

import com.udi.config.ConexionBD;
import com.udi.dao.roles.ModuloDAO;
import com.udi.dao.roles.RolDAO;
import com.udi.dao.usuarios.UsuarioDAO;
import com.udi.exception.ReglaNegocioException;
import com.udi.model.Modulo;
import com.udi.model.Rol;
import com.udi.model.Usuario;
import com.udi.util.Seguridad;
import com.udi.util.Sesion;
import com.udi.util.Validar;

import java.util.List;

/** Inicio y cierre de sesión (RF-01). */
public class AuthService {

    public static final String MSG_CREDENCIALES = "Correo o contraseña incorrectos.";
    public static final String MSG_INACTIVO = "El usuario está inactivo. Comuníquese con el administrador.";

    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final RolDAO rolDao = new RolDAO();
    private final ModuloDAO moduloDao = new ModuloDAO();

    /**
     * Valida el correo y la contraseña, revisa que el usuario esté activo,
     * registra el último acceso y deja los datos en {@link Sesion}. También
     * identifica al usuario en la sesión de Oracle (pr_iniciar_sesion) para
     * que la auditoría registre quién hace cada cambio.
     */
    public Usuario iniciarSesion(String email, String contrasena) {
        Validar.requerido(email, "Correo");
        Validar.requerido(contrasena, "Contraseña");
        DatosSesion datos = ConexionBD.enTransaccion(c -> {
            Usuario usuario = usuarioDao.buscarPorEmail(c, email.trim()).orElse(null);
            if (usuario == null || !usuario.getPassword().equals(Seguridad.hash(contrasena))) {
                throw new ReglaNegocioException(MSG_CREDENCIALES);
            }
            if (!usuario.isActivo()) {
                throw new ReglaNegocioException(MSG_INACTIVO);
            }
            Rol rol = Validar.encontrado(rolDao.buscarPorId(c, usuario.getRolId()),
                    "El usuario no tiene un rol válido.");
            List<Modulo> modulos = moduloDao.listarPorRol(c, rol.getId());
            usuarioDao.actualizarUltimoAcceso(c, usuario.getId());
            return new DatosSesion(usuario, rol, modulos);
        });
        ConexionBD.identificarUsuario(datos.usuario.getId());
        Sesion.iniciar(datos.usuario, datos.rol, datos.modulos);
        return datos.usuario;
    }

    public void cerrarSesion() {
        Sesion.cerrar();
        ConexionBD.identificarUsuario(null);
    }

    private static final class DatosSesion {
        private final Usuario usuario;
        private final Rol rol;
        private final List<Modulo> modulos;

        private DatosSesion(Usuario usuario, Rol rol, List<Modulo> modulos) {
            this.usuario = usuario;
            this.rol = rol;
            this.modulos = modulos;
        }
    }
}
