package com.serviciotickets.modelo;

import java.util.ArrayList;
import java.util.List;

public class Administrador extends Persona {
    private static final long serialVersionUID = 1L;
    
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
} 