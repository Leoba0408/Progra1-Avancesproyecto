package com.serviciotickets.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "permisos")
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    // Constructor por defecto
    public Permiso() {}

    // Constructor con parámetros
    public Permiso(String nombre, String descripcion) {
        setNombre(nombre);
        this.descripcion = descripcion;
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
            throw new IllegalArgumentException("El nombre del permiso debe tener entre 3 y 50 caracteres");
        }
        this.nombre = nombre.trim();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permiso)) return false;
        Permiso permiso = (Permiso) o;
        return nombre != null && nombre.equals(permiso.getNombre());
    }

    @Override
    public int hashCode() {
        return nombre != null ? nombre.hashCode() : 0;
    }
} 