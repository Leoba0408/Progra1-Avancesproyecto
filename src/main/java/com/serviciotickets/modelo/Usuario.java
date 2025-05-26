package com.serviciotickets.modelo;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;
import javax.persistence.FetchType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.PrimaryKeyJoinColumn;

@Entity
@Table(name = "usuarios")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("USUARIO")
public class Usuario extends Persona {
    @Column(name = "nombre_usuario", length = 30, unique = true, nullable = false)
    private String nombreUsuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @Column(name = "activo")
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_modificacion", nullable = false)
    private LocalDateTime ultimaModificacion;

    @OneToMany(mappedBy = "solicitante")
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "tecnicoAsignado")
    private List<Ticket> ticketsAsignados = new ArrayList<>();

    public Usuario() {
        super();
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaModificacion = LocalDateTime.now();
    }

    public Usuario(String nombre, String apellido, String email, String nombreUsuario, String password) {
        super(nombre, apellido, email, "Sin teléfono", password);
        this.nombreUsuario = nombreUsuario;
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaModificacion = LocalDateTime.now();
    }

    // Solo mantener los getters/setters específicos de Usuario
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.trim().length() < 5 || nombreUsuario.trim().length() > 30) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 5 y 30 caracteres");
        }
        this.nombreUsuario = nombreUsuario.trim();
        this.ultimaModificacion = LocalDateTime.now();
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getUltimaModificacion() {
        return ultimaModificacion;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public List<Ticket> getTicketsAsignados() {
        return ticketsAsignados;
    }

    public void setTicketsAsignados(List<Ticket> ticketsAsignados) {
        this.ticketsAsignados = ticketsAsignados;
    }

    public boolean tienePermiso(String nombrePermiso) {
        return rol != null && rol.tienePermiso(nombrePermiso);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        if (!super.equals(o)) return false;
        Usuario usuario = (Usuario) o;
        return nombreUsuario != null && nombreUsuario.equals(usuario.getNombreUsuario());
    }

    @Override
    public int hashCode() {
        return nombreUsuario != null ? nombreUsuario.hashCode() : 0;
    }
} 