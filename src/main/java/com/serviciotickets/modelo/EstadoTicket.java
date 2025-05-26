package com.serviciotickets.modelo;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "estados_ticket")
public class EstadoTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "es_estado_final")
    private boolean esEstadoFinal;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "transiciones_estado",
        joinColumns = @JoinColumn(name = "estado_actual_id"),
        inverseJoinColumns = @JoinColumn(name = "estado_siguiente_id")
    )
    private Set<EstadoTicket> estadosSiguientes = new HashSet<>();

    // Constructor por defecto
    public EstadoTicket() {}

    // Constructor con parámetros principales
    public EstadoTicket(String nombre, String descripcion, boolean esEstadoFinal) {
        setNombre(nombre);
        this.descripcion = descripcion;
        this.esEstadoFinal = esEstadoFinal;
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
        if (nombre == null || nombre.trim().length() < 3 || nombre.trim().length() > 50) {
            throw new IllegalArgumentException("El nombre del estado debe tener entre 3 y 50 caracteres");
        }
        this.nombre = nombre.trim();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isEstadoFinal() {
        return esEstadoFinal;
    }

    public void setEstadoFinal(boolean esEstadoFinal) {
        this.esEstadoFinal = esEstadoFinal;
    }

    public Set<EstadoTicket> getEstadosSiguientes() {
        return estadosSiguientes;
    }

    public void setEstadosSiguientes(Set<EstadoTicket> estadosSiguientes) {
        this.estadosSiguientes = estadosSiguientes;
    }

    // Métodos de utilidad
    public void agregarEstadoSiguiente(EstadoTicket estado) {
        this.estadosSiguientes.add(estado);
    }

    public void removerEstadoSiguiente(EstadoTicket estado) {
        this.estadosSiguientes.remove(estado);
    }

    public boolean puedeTransicionarA(EstadoTicket estado) {
        return this.estadosSiguientes.contains(estado);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EstadoTicket)) return false;
        EstadoTicket estado = (EstadoTicket) o;
        return nombre != null && nombre.equals(estado.getNombre());
    }

    @Override
    public int hashCode() {
        return nombre != null ? nombre.hashCode() : 0;
    }
} 