package com.serviciotickets.modelo;

import jakarta.persistence.*;
import java.time.ZoneId;
import java.util.List;

@Entity
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_empresa", length = 100, nullable = false)
    private String nombreEmpresa;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "idioma_predeterminado", nullable = false)
    private String idiomaPredeterminado;

    @Column(name = "zona_horaria", nullable = false)
    private String zonaHoraria;

    @Column(name = "tiempo_vencimiento_tickets")
    private Integer tiempoVencimientoTickets;

    @ElementCollection
    @CollectionTable(name = "niveles_prioridad", 
        joinColumns = @JoinColumn(name = "configuracion_id"))
    @Column(name = "nivel")
    private List<String> nivelesPrioridad;

    @Column(name = "ultima_modificacion")
    private java.time.LocalDateTime ultimaModificacion;

    @Column(name = "modificado_por")
    private String modificadoPor;

    // Constructor por defecto
    public ConfiguracionSistema() {}

    // Constructor con parámetros principales
    public ConfiguracionSistema(String nombreEmpresa, String idiomaPredeterminado, String zonaHoraria) {
        this.nombreEmpresa = nombreEmpresa;
        this.idiomaPredeterminado = idiomaPredeterminado;
        this.zonaHoraria = zonaHoraria;
        this.tiempoVencimientoTickets = 30; // Valor por defecto: 30 días
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public void setNombreEmpresa(String nombreEmpresa) {
        if (nombreEmpresa == null || nombreEmpresa.trim().length() < 3 || nombreEmpresa.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre de la empresa debe tener entre 3 y 100 caracteres");
        }
        this.nombreEmpresa = nombreEmpresa.trim();
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getIdiomaPredeterminado() {
        return idiomaPredeterminado;
    }

    public void setIdiomaPredeterminado(String idiomaPredeterminado) {
        this.idiomaPredeterminado = idiomaPredeterminado;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public void setZonaHoraria(String zonaHoraria) {
        try {
            ZoneId.of(zonaHoraria); // Validar que sea una zona horaria válida
            this.zonaHoraria = zonaHoraria;
        } catch (Exception e) {
            throw new IllegalArgumentException("Zona horaria inválida");
        }
    }

    public Integer getTiempoVencimientoTickets() {
        return tiempoVencimientoTickets;
    }

    public void setTiempoVencimientoTickets(Integer tiempoVencimientoTickets) {
        if (tiempoVencimientoTickets < 1 || tiempoVencimientoTickets > 365) {
            throw new IllegalArgumentException("El tiempo de vencimiento debe estar entre 1 y 365 días");
        }
        this.tiempoVencimientoTickets = tiempoVencimientoTickets;
    }

    public List<String> getNivelesPrioridad() {
        return nivelesPrioridad;
    }

    public void setNivelesPrioridad(List<String> nivelesPrioridad) {
        if (nivelesPrioridad == null || nivelesPrioridad.size() < 3) {
            throw new IllegalArgumentException("Debe definir al menos tres niveles de prioridad");
        }
        this.nivelesPrioridad = nivelesPrioridad;
    }

    public java.time.LocalDateTime getUltimaModificacion() {
        return ultimaModificacion;
    }

    public void setUltimaModificacion(java.time.LocalDateTime ultimaModificacion) {
        this.ultimaModificacion = ultimaModificacion;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.ultimaModificacion = java.time.LocalDateTime.now();
    }
} 