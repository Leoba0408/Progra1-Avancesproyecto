package com.serviciotickets.controlador;

import com.serviciotickets.modelo.FlujoTrabajo;
import com.serviciotickets.modelo.EstadoTicket;
import com.serviciotickets.persistencia.ConexionDB;
import com.serviciotickets.servicio.SesionServicio;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FlujosTrabajoController {
    @FXML
    private ListView<FlujoTrabajo> flujosListView;
    
    @FXML
    private TextField nombreFlujoField;
    
    @FXML
    private TextArea descripcionFlujoArea;
    
    @FXML
    private ListView<EstadoTicket> estadosDisponiblesListView;
    
    @FXML
    private ListView<EstadoTicket> estadosAsignadosListView;
    
    @FXML
    private Button agregarEstadoButton;
    
    @FXML
    private Button quitarEstadoButton;
    
    @FXML
    private Button nuevoFlujoButton;
    
    @FXML
    private Button guardarFlujoButton;
    
    @FXML
    private Button eliminarFlujoButton;
    
    private EntityManager entityManager;
    private FlujoTrabajo flujoSeleccionado;
    private ObservableList<EstadoTicket> estadosDisponibles;
    private ObservableList<EstadoTicket> estadosAsignados;

    @FXML
    public void initialize() {
        entityManager = ConexionDB.getEntityManagerFactory().createEntityManager();
        
        // Configurar listas
        estadosDisponibles = FXCollections.observableArrayList();
        estadosAsignados = FXCollections.observableArrayList();
        
        estadosDisponiblesListView.setItems(estadosDisponibles);
        estadosAsignadosListView.setItems(estadosAsignados);
        
        // Configurar selección de flujos
        flujosListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> seleccionarFlujo(newVal));
        
        // Configurar botones
        nuevoFlujoButton.setOnAction(e -> nuevoFlujo());
        guardarFlujoButton.setOnAction(e -> guardarFlujo());
        eliminarFlujoButton.setOnAction(e -> eliminarFlujo());
        agregarEstadoButton.setOnAction(e -> agregarEstado());
        quitarEstadoButton.setOnAction(e -> quitarEstado());
        
        // Cargar datos iniciales
        cargarFlujos();
        cargarEstados();
        
        // Configurar formato de celdas
        flujosListView.setCellFactory(lv -> new ListCell<FlujoTrabajo>() {
            @Override
            protected void updateItem(FlujoTrabajo flujo, boolean empty) {
                super.updateItem(flujo, empty);
                if (empty || flujo == null) {
                    setText(null);
                } else {
                    setText(flujo.getNombre());
                }
            }
        });
        
        estadosDisponiblesListView.setCellFactory(lv -> new ListCell<EstadoTicket>() {
            @Override
            protected void updateItem(EstadoTicket estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                } else {
                    setText(estado.getNombre());
                }
            }
        });
        
        estadosAsignadosListView.setCellFactory(lv -> new ListCell<EstadoTicket>() {
            @Override
            protected void updateItem(EstadoTicket estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                } else {
                    setText(estado.getNombre());
                }
            }
        });
    }

    private void cargarFlujos() {
        List<FlujoTrabajo> flujos = entityManager.createQuery(
            "SELECT f FROM FlujoTrabajo f", FlujoTrabajo.class)
            .getResultList();
            
        if (flujos.isEmpty()) {
            // Crear flujo por defecto si no existe ninguno
            crearFlujoInicial();
            flujos = entityManager.createQuery(
                "SELECT f FROM FlujoTrabajo f", FlujoTrabajo.class)
                .getResultList();
        }
        
        flujosListView.setItems(FXCollections.observableArrayList(flujos));
    }

    private void cargarEstados() {
        List<EstadoTicket> estados = entityManager.createQuery(
            "SELECT e FROM EstadoTicket e", EstadoTicket.class)
            .getResultList();
        estadosDisponibles.setAll(estados);
    }

    private void crearFlujoInicial() {
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            // Obtener estados existentes
            List<EstadoTicket> estados = entityManager.createQuery(
                "SELECT e FROM EstadoTicket e", EstadoTicket.class)
                .getResultList();
                
            if (!estados.isEmpty()) {
                FlujoTrabajo flujoBasico = new FlujoTrabajo(
                    "Flujo Básico", 
                    "Flujo de trabajo básico para tickets",
                    "SISTEMA"
                );
                
                // Agregar todos los estados al flujo básico
                for (EstadoTicket estado : estados) {
                    flujoBasico.agregarEstado(estado);
                }
                
                entityManager.persist(flujoBasico);
            }
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear flujo inicial", e.getMessage());
        }
    }

    private void seleccionarFlujo(FlujoTrabajo flujo) {
        flujoSeleccionado = flujo;
        if (flujo != null) {
            nombreFlujoField.setText(flujo.getNombre());
            descripcionFlujoArea.setText(flujo.getDescripcion());
            actualizarEstadosAsignados();
        } else {
            nombreFlujoField.clear();
            descripcionFlujoArea.clear();
            estadosAsignados.clear();
        }
        actualizarBotonesEstados();
    }

    private void actualizarEstadosAsignados() {
        if (flujoSeleccionado != null) {
            Set<EstadoTicket> asignados = flujoSeleccionado.getEstados();
            estadosAsignados.setAll(asignados);
            estadosDisponibles.removeAll(asignados);
        }
    }

    private void actualizarBotonesEstados() {
        boolean flujoSeleccionado = this.flujoSeleccionado != null;
        agregarEstadoButton.setDisable(!flujoSeleccionado || 
            estadosDisponiblesListView.getSelectionModel().isEmpty());
        quitarEstadoButton.setDisable(!flujoSeleccionado || 
            estadosAsignadosListView.getSelectionModel().isEmpty());
    }

    private void nuevoFlujo() {
        flujoSeleccionado = null;
        nombreFlujoField.clear();
        descripcionFlujoArea.clear();
        estadosAsignados.clear();
        cargarEstados();
        actualizarBotonesEstados();
    }

    private void guardarFlujo() {
        if (!validarDatos()) {
            return;
        }

        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            if (flujoSeleccionado == null) {
                flujoSeleccionado = new FlujoTrabajo();
                flujoSeleccionado.setCreadoPor(SesionServicio.getInstancia().getNombreUsuarioActual());
            }
            
            flujoSeleccionado.setNombre(nombreFlujoField.getText().trim());
            flujoSeleccionado.setDescripcion(descripcionFlujoArea.getText().trim());
            flujoSeleccionado.setModificadoPor(SesionServicio.getInstancia().getNombreUsuarioActual());
            flujoSeleccionado.getEstados().clear();
            flujoSeleccionado.getEstados().addAll(estadosAsignados);
            
            if (flujoSeleccionado.getId() == null) {
                entityManager.persist(flujoSeleccionado);
            } else {
                entityManager.merge(flujoSeleccionado);
            }
            
            transaction.commit();
            mostrarMensaje("Flujo guardado", "El flujo de trabajo se ha guardado exitosamente");
            cargarFlujos();
            
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al guardar", e.getMessage());
        }
    }

    private void eliminarFlujo() {
        if (flujoSeleccionado == null) {
            return;
        }

        // Verificar si hay tickets usando este flujo
        Long ticketsConFlujo = entityManager.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.flujoTrabajo = :flujo", Long.class)
            .setParameter("flujo", flujoSeleccionado)
            .getSingleResult();
            
        if (ticketsConFlujo > 0) {
            mostrarError("No se puede eliminar", 
                "Existen tickets usando este flujo de trabajo. Reasigne los tickets antes de eliminar el flujo.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea eliminar el flujo de trabajo?");

        if (confirmacion.showAndWait().get() == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                entityManager.remove(
                    entityManager.contains(flujoSeleccionado) ? 
                    flujoSeleccionado : 
                    entityManager.merge(flujoSeleccionado)
                );
                
                transaction.commit();
                mostrarMensaje("Flujo eliminado", "El flujo de trabajo se ha eliminado exitosamente");
                
                nuevoFlujo();
                cargarFlujos();
                
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar", e.getMessage());
            }
        }
    }

    private void agregarEstado() {
        EstadoTicket estado = estadosDisponiblesListView.getSelectionModel().getSelectedItem();
        if (estado != null) {
            estadosDisponibles.remove(estado);
            estadosAsignados.add(estado);
        }
    }

    private void quitarEstado() {
        EstadoTicket estado = estadosAsignadosListView.getSelectionModel().getSelectedItem();
        if (estado != null) {
            estadosAsignados.remove(estado);
            estadosDisponibles.add(estado);
        }
    }

    private boolean validarDatos() {
        if (nombreFlujoField.getText().trim().isEmpty()) {
            mostrarError("Error de validación", "El nombre del flujo es obligatorio");
            return false;
        }
        
        // Verificar si el nombre ya existe (excepto para el mismo flujo)
        FlujoTrabajo flujoExistente = entityManager.createQuery(
            "SELECT f FROM FlujoTrabajo f WHERE f.nombre = :nombre AND f.id != :id", 
            FlujoTrabajo.class)
            .setParameter("nombre", nombreFlujoField.getText().trim())
            .setParameter("id", flujoSeleccionado != null ? flujoSeleccionado.getId() : -1L)
            .getResultList()
            .stream()
            .collect(Collectors.toList())
            .stream()
            .findFirst()
            .orElse(null);
            
        if (flujoExistente != null) {
            mostrarError("Error de validación", "Ya existe un flujo con ese nombre");
            return false;
        }
        
        if (estadosAsignados.isEmpty()) {
            mostrarError("Error de validación", "Debe asignar al menos un estado al flujo");
            return false;
        }
        
        return true;
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
} 