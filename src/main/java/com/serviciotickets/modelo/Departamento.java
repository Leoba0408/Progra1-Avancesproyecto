package com.serviciotickets.modelo;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import javax.persistence.*;

@Entity
@Table(name = "departamentos")
public class Departamento implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @OneToMany(mappedBy = "departamento")
    private List<Tecnico> tecnicos = new ArrayList<>();

    @Transient
    private Queue<Ticket> colaTickets;

    public Departamento() {
        this.colaTickets = new LinkedList<>();
    }

    public Departamento(String nombre, String descripcion) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public void agregarTicket(Ticket ticket) {
        colaTickets.offer(ticket);
    }

    public Ticket obtenerSiguienteTicket() {
        return colaTickets.poll();
    }

    public boolean hayTicketsPendientes() {
        return !colaTickets.isEmpty();
    }

    public int cantidadTicketsPendientes() {
        return colaTickets.size();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Queue<Ticket> getColaTickets() {
        return colaTickets;
    }

    public List<Tecnico> getTecnicos() {
        return tecnicos;
    }

    public void setTecnicos(List<Tecnico> tecnicos) {
        this.tecnicos = tecnicos;
    }

    @Override
    public String toString() {
        return nombre;
    }
} 