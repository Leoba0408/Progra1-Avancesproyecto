package com.serviciotickets.modelo;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Column;
import javax.persistence.ManyToMany;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;

@Entity
@Table(name = "administradores")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Usuario {
    private static final long serialVersionUID = 1L;
    
    @Column(name = "cargo")
    private String cargo;
    
    @ManyToMany
    @JoinTable(
        name = "administrador_departamento",
        joinColumns = @JoinColumn(name = "administrador_id"),
        inverseJoinColumns = @JoinColumn(name = "departamento_id")
    )
    private List<Departamento> departamentosGestionados;

    public Administrador() {
        super();
        this.departamentosGestionados = new ArrayList<>();
    }

    public Administrador(String nombre, String apellido, String email, String telefono, String password) {
        super(nombre, apellido, email, telefono, password);
        this.departamentosGestionados = new ArrayList<>();
    }

    public void agregarDepartamento(Departamento departamento) {
        departamentosGestionados.add(departamento);
    }

    public void eliminarDepartamento(Departamento departamento) {
        departamentosGestionados.remove(departamento);
    }

    public void asignarTecnicoADepartamento(Tecnico tecnico, Departamento departamento) {
        if (departamentosGestionados.contains(departamento)) {
            tecnico.setDepartamento(departamento);
        }
    }

    public List<Departamento> getDepartamentosGestionados() {
        return departamentosGestionados;
    }

    public void setDepartamentosGestionados(List<Departamento> departamentosGestionados) {
        this.departamentosGestionados = departamentosGestionados;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
} 