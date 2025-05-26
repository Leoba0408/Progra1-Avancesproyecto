package com.serviciotickets.modelo;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tecnicos")
public class Tecnico extends Persona {
    private static final long serialVersionUID = 1L;
    
    @OneToMany(mappedBy = "tecnicoAsignado")
    private List<Ticket> ticketsAsignados;

    @ManyToOne
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @Column(name = "disponible")
    private boolean disponible;

    public Tecnico() {
        super();
        this.ticketsAsignados = new ArrayList<>();
        this.disponible = true;
    }

    public Tecnico(String nombre, String apellido, String email, String telefono, String password, Departamento departamento) {
        super(nombre, apellido, email, telefono, password);
        this.ticketsAsignados = new ArrayList<>();
        this.departamento = departamento;
        this.disponible = true;
    }

    public void asignarTicket(Ticket ticket, String estadoAsignado) {
        if (estadoAsignado == null || estadoAsignado.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado asignado no puede ser nulo o vacío");
        }
        ticketsAsignados.add(ticket);
        ticket.setTecnicoAsignado(this);
        ticket.cambiarEstado(estadoAsignado);
    }

    public void resolverTicket(Ticket ticket, String estadoResuelto) {
        if (ticketsAsignados.contains(ticket)) {
            if (estadoResuelto == null || estadoResuelto.trim().isEmpty()) {
                throw new IllegalArgumentException("El estado resuelto no puede ser nulo o vacío");
            }
            ticket.cambiarEstado(estadoResuelto);
        } else {
            throw new IllegalStateException("El ticket no está asignado a este técnico");
        }
    }

    public List<Ticket> getTicketsAsignados() {
        return ticketsAsignados;
    }

    public void setTicketsAsignados(List<Ticket> ticketsAsignados) {
        this.ticketsAsignados = ticketsAsignados;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
} 