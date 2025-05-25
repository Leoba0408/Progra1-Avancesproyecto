package com.serviciotickets.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "flujos_trabajo")
public class FlujoTrabajo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 100, nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "flujo_estados",
        joinColumns = @JoinColumn(name = "flujo_id"),
        inverseJoinColumns = @JoinColumn(name = "estado_id")
    )
    private Set<EstadoTicket> estados = new HashSet<>();

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion;

    @Column(name = "creado_por")
    private String creadoPor;

    @Column(name = "modificado_por")
    private String modificadoPor;

    // Constructor por defecto
    public FlujoTrabajo() {
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaModificacion = LocalDateTime.now();
    }

    // Constructor con parámetros principales
    public FlujoTrabajo(String nombre, String descripcion, String creadoPor) {
        this();
        setNombre(nombre);
        this.descripcion = descripcion;
        this.creadoPor = creadoPor;
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
        if (nombre == null || nombre.trim().length() < 3 || nombre.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre del flujo debe tener entre 3 y 100 caracteres");
        }
        this.nombre = nombre.trim();
        this.ultimaModificacion = LocalDateTime.now();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public Set<EstadoTicket> getEstados() {
        return estados;
    }

    public void setEstados(Set<EstadoTicket> estados) {
        this.estados = estados;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getUltimaModificacion() {
        return ultimaModificacion;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
        this.ultimaModificacion = LocalDateTime.now();
    }

    // Métodos de utilidad
    public void agregarEstado(EstadoTicket estado) {
        this.estados.add(estado);
        this.ultimaModificacion = LocalDateTime.now();
    }

    public void removerEstado(EstadoTicket estado) {
        this.estados.remove(estado);
        this.ultimaModificacion = LocalDateTime.now();
    }

    public boolean contieneEstado(EstadoTicket estado) {
        return this.estados.contains(estado);
    }

    public boolean validarTransicion(EstadoTicket estadoActual, EstadoTicket estadoSiguiente) {
        return contieneEstado(estadoActual) && 
               contieneEstado(estadoSiguiente) && 
               estadoActual.puedeTransicionarA(estadoSiguiente);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlujoTrabajo)) return false;
        FlujoTrabajo flujo = (FlujoTrabajo) o;
        return nombre != null && nombre.equals(flujo.getNombre());
    }

    @Override
    public int hashCode() {
        return nombre != null ? nombre.hashCode() : 0;
    }
} 