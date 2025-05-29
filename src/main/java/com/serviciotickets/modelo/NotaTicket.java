package com.serviciotickets.modelo;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notas_ticket")
public class NotaTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contenido", nullable = false)
    private String contenido;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "creado_por_id")
    private Persona creadoPor;

    @ElementCollection
    @CollectionTable(name = "adjuntos_nota",
        joinColumns = @JoinColumn(name = "nota_id"))
    @Column(name = "ruta_archivo")
    private List<String> adjuntos = new ArrayList<>();

    // Constructor por defecto
    public NotaTicket() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor con parámetros principales
    public NotaTicket(String contenido, Ticket ticket, Persona creadoPor) {
        this();
        this.contenido = contenido;
        this.ticket = ticket;
        this.creadoPor = creadoPor;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        if (contenido == null || contenido.trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido de la nota no puede estar vacío");
        }
        this.contenido = contenido.trim();
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Persona getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Persona creadoPor) {
        this.creadoPor = creadoPor;
    }

    public List<String> getAdjuntos() {
        return adjuntos;
    }

    public void setAdjuntos(List<String> adjuntos) {
        this.adjuntos = adjuntos;
    }

    // Métodos de utilidad
    public void agregarAdjunto(String rutaArchivo) {
        this.adjuntos.add(rutaArchivo);
    }

    public void removerAdjunto(String rutaArchivo) {
        this.adjuntos.remove(rutaArchivo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotaTicket)) return false;
        NotaTicket nota = (NotaTicket) o;
        return id != null && id.equals(nota.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 