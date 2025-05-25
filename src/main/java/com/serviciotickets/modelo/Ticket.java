package com.serviciotickets.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Stack;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "tickets")
public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "prioridad", nullable = false)
    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;

    @Column(name = "estado", nullable = false)
    private String estado;

    @ManyToOne
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @Column(name = "fechacreacion", nullable = false)
    private LocalDateTime fechaCreacionInicial;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_vencimiento")
    private LocalDateTime fechaVencimiento;

    @Column(name = "fechaactualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotaTicket> notas = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Persona solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Persona tecnicoAsignado;

    @Transient
    private Stack<String> historialEstados;

    public enum Prioridad {
        ALTA,
        MEDIA,
        BAJA
    }

    public Ticket() {
        this.fechaCreacionInicial = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaModificacion = LocalDateTime.now();
        this.historialEstados = new Stack<>();
    }

    public Ticket(String titulo, String descripcion, Prioridad prioridad, Departamento departamento) {
        this();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.departamento = departamento;
        this.estado = "NUEVO"; // Estado inicial por defecto
    }

    public void cambiarEstado(String nuevoEstado) {
        this.estado = nuevoEstado;
        this.historialEstados.push(nuevoEstado);
        this.fechaActualizacion = LocalDateTime.now();
        this.ultimaModificacion = LocalDateTime.now();
    }

    public boolean deshacerUltimoEstado() {
        if (historialEstados.size() > 1) {
            historialEstados.pop(); // Elimina el estado actual
            String estadoAnterior = historialEstados.peek(); // Obtiene el estado anterior
            this.estado = estadoAnterior;
            this.fechaActualizacion = LocalDateTime.now();
            this.ultimaModificacion = LocalDateTime.now();
            return true;
        }
        return false;
    }

    // Getters y setters actualizados
    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
        if (historialEstados == null) {
            historialEstados = new Stack<>();
        }
        historialEstados.push(estado);
        this.fechaActualizacion = LocalDateTime.now();
        this.ultimaModificacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        this.titulo = titulo.trim();
        this.ultimaModificacion = LocalDateTime.now();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía");
        }
        this.descripcion = descripcion.trim();
        this.ultimaModificacion = LocalDateTime.now();
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public LocalDateTime getFechaCreacionInicial() {
        return fechaCreacionInicial;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
        this.ultimaModificacion = LocalDateTime.now();
    }

    public LocalDateTime getUltimaModificacion() {
        return ultimaModificacion;
    }

    public List<NotaTicket> getNotas() {
        return notas;
    }

    public void agregarNota(NotaTicket nota) {
        notas.add(nota);
        nota.setTicket(this);
        this.ultimaModificacion = LocalDateTime.now();
    }

    public void removerNota(NotaTicket nota) {
        notas.remove(nota);
        nota.setTicket(null);
        this.ultimaModificacion = LocalDateTime.now();
    }

    public Persona getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Persona solicitante) {
        this.solicitante = solicitante;
    }

    public Persona getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public void setTecnicoAsignado(Persona tecnicoAsignado) {
        this.tecnicoAsignado = tecnicoAsignado;
    }

    public Stack<String> getHistorialEstados() {
        return historialEstados;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket)) return false;
        Ticket ticket = (Ticket) o;
        return id != null && id.equals(ticket.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 