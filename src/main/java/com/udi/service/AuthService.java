package com.udi.service;

import com.udi.config.ConexionBD;
import com.udi.dao.ModuloDAO;
import com.udi.dao.RolDAO;
import com.udi.dao.UsuarioDAO;
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

    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final RolDAO rolDao = new RolDAO();
    private final ModuloDAO moduloDao = new ModuloDAO();

    /**
     * Valida el correo y la contraseña, revisa que el usuario esté activo,
     * registra el último acceso y deja los datos en {@link Sesion}.
     */
    public Usuario iniciarSesion(String email, String contrasena) {
        Validar.requerido(email, "Correo");
        Validar.requerido(contrasena, "Contraseña");
        DatosSesion datos = ConexionBD.enTransaccion(c -> {
            Usuario usuario = usuarioDao.buscarPorEmail(c, email.trim()).orElse(null);
            if (usuario == null || !usuario.getPassword().equals(Seguridad.hash(contrasena))) {
                throw new ReglaNegocioException("Correo o contraseña incorrectos.");
            }
            if (!usuario.isActivo()) {
                throw new ReglaNegocioException("El usuario está inactivo. Comuníquese con el administrador.");
            }
            Rol rol = Validar.encontrado(rolDao.buscarPorId(c, usuario.getRolId()),
                    "El usuario no tiene un rol válido.");
            List<Modulo> modulos = moduloDao.listarPorRol(c, rol.getId());
            usuarioDao.actualizarUltimoAcceso(c, usuario.getId());
            return new DatosSesion(usuario, rol, modulos);
        });
        Sesion.iniciar(datos.usuario, datos.rol, datos.modulos);
        return datos.usuario;
    }

    public void cerrarSesion() {
        Sesion.cerrar();
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
