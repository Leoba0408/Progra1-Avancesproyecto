package com.serviciotickets.servicio;

import com.serviciotickets.modelo.Usuario;

public class SesionServicio {
    private static SesionServicio instancia;
    private Usuario usuarioActual;

    private SesionServicio() {}

    public static SesionServicio getInstancia() {
        if (instancia == null) {
            instancia = new SesionServicio();
        }
        return instancia;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public String getNombreUsuarioActual() {
        return usuarioActual != null ? usuarioActual.getNombreUsuario() : "SISTEMA";
    }

    public boolean isUsuarioAutenticado() {
        return usuarioActual != null;
    }

    public void cerrarSesion() {
        usuarioActual = null;
    }

    public boolean tieneRolAdministrador() {
        if (usuarioActual == null) return false;
        return usuarioActual instanceof com.serviciotickets.modelo.Administrador ||
               (usuarioActual.getRol() != null && 
                "ADMINISTRADOR".equals(usuarioActual.getRol().getNombre()));
    }
    
    public boolean tieneRolTecnico() {
        if (usuarioActual == null) return false;
        return usuarioActual instanceof com.serviciotickets.modelo.Tecnico ||
               (usuarioActual.getRol() != null && 
                "TECNICO".equals(usuarioActual.getRol().getNombre())) ||
               tieneRolAdministrador(); // Los administradores también tienen permisos de técnicos
    }
    
    public boolean tieneRolUsuario() {
        return isUsuarioAutenticado(); // Cualquier usuario autenticado tiene rol básico
    }
} 