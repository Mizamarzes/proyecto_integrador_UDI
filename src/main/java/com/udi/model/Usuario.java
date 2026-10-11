package com.udi.model;

import java.time.LocalDateTime;

/** Usuario del sistema (RF-01). */
public class Usuario extends EntidadAuditable {

    private String email;
    /** Hash SHA-256 de la contraseña, nunca el texto plano */
    private String password;
    private String nombre;
    private String apellido;
    private String telefono;
    private Long rolId;
    /** Si es false, el usuario no puede iniciar sesión */
    private boolean activo = true;
    /** Fecha y hora del último inicio de sesión */
    private LocalDateTime ultimoAcceso;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Long getRolId() {
        return rolId;
    }

    public void setRolId(Long rolId) {
        this.rolId = rolId;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Override
    public String toString() {
        return nombre + " " + apellido;
    }
}
