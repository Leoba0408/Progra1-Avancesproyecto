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
} 