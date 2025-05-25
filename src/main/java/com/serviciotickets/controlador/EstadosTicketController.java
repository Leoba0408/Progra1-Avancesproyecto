package com.serviciotickets.controlador;

import com.serviciotickets.modelo.EstadoTicket;
import com.serviciotickets.persistencia.ConexionDB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EstadosTicketController {
    @FXML
    private ListView<EstadoTicket> estadosListView;
    
    @FXML
    private TextField nombreEstadoField;
    
    @FXML
    private TextArea descripcionEstadoArea;
    
    @FXML
    private CheckBox estadoFinalCheckBox;
    
    @FXML
    private ListView<EstadoTicket> estadosSiguientesDisponiblesListView;
    
    @FXML
    private ListView<EstadoTicket> estadosSiguientesAsignadosListView;
    
    @FXML
    private Button agregarEstadoSiguienteButton;
    
    @FXML
    private Button quitarEstadoSiguienteButton;
    
    @FXML
    private Button nuevoEstadoButton;
    
    @FXML
    private Button guardarEstadoButton;
    
    @FXML
    private Button eliminarEstadoButton;
    
    private EntityManager entityManager;
    private EstadoTicket estadoSeleccionado;
    private ObservableList<EstadoTicket> estadosSiguientesDisponibles;
    private ObservableList<EstadoTicket> estadosSiguientesAsignados;

    @FXML
    public void initialize() {
        entityManager = ConexionDB.getEntityManagerFactory().createEntityManager();
        
        // Configurar listas
        estadosSiguientesDisponibles = FXCollections.observableArrayList();
        estadosSiguientesAsignados = FXCollections.observableArrayList();
        
        estadosSiguientesDisponiblesListView.setItems(estadosSiguientesDisponibles);
        estadosSiguientesAsignadosListView.setItems(estadosSiguientesAsignados);
        
        // Configurar selección de estados
        estadosListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> seleccionarEstado(newVal));
        
        // Configurar botones
        nuevoEstadoButton.setOnAction(e -> nuevoEstado());
        guardarEstadoButton.setOnAction(e -> guardarEstado());
        eliminarEstadoButton.setOnAction(e -> eliminarEstado());
        agregarEstadoSiguienteButton.setOnAction(e -> agregarEstadoSiguiente());
        quitarEstadoSiguienteButton.setOnAction(e -> quitarEstadoSiguiente());
        
        // Cargar datos iniciales
        cargarEstados();
        
        // Configurar formato de celdas
        estadosListView.setCellFactory(lv -> new ListCell<EstadoTicket>() {
            @Override
            protected void updateItem(EstadoTicket estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                } else {
                    setText(estado.getNombre() + (estado.isEstadoFinal() ? " (Final)" : ""));
                }
            }
        });
        
        estadosSiguientesDisponiblesListView.setCellFactory(lv -> new ListCell<EstadoTicket>() {
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
        
        estadosSiguientesAsignadosListView.setCellFactory(lv -> new ListCell<EstadoTicket>() {
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

    private void cargarEstados() {
        List<EstadoTicket> estados = entityManager.createQuery(
            "SELECT e FROM EstadoTicket e", EstadoTicket.class)
            .getResultList();
            
        if (estados.isEmpty()) {
            // Crear estados por defecto si no existen
            crearEstadosIniciales();
            estados = entityManager.createQuery(
                "SELECT e FROM EstadoTicket e", EstadoTicket.class)
                .getResultList();
        }
        
        estadosListView.setItems(FXCollections.observableArrayList(estados));
    }

    private void crearEstadosIniciales() {
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            EstadoTicket pendiente = new EstadoTicket("Pendiente", "Ticket recién creado", false);
            EstadoTicket enProceso = new EstadoTicket("En Proceso", "Ticket siendo atendido", false);
            EstadoTicket escalado = new EstadoTicket("Escalado", "Ticket escalado a otro nivel", false);
            EstadoTicket resuelto = new EstadoTicket("Resuelto", "Ticket resuelto", true);
            EstadoTicket cerrado = new EstadoTicket("Cerrado", "Ticket cerrado", true);
            
            entityManager.persist(pendiente);
            entityManager.persist(enProceso);
            entityManager.persist(escalado);
            entityManager.persist(resuelto);
            entityManager.persist(cerrado);
            
            // Configurar transiciones permitidas
            pendiente.agregarEstadoSiguiente(enProceso);
            pendiente.agregarEstadoSiguiente(escalado);
            
            enProceso.agregarEstadoSiguiente(escalado);
            enProceso.agregarEstadoSiguiente(resuelto);
            
            escalado.agregarEstadoSiguiente(enProceso);
            escalado.agregarEstadoSiguiente(resuelto);
            
            resuelto.agregarEstadoSiguiente(cerrado);
            resuelto.agregarEstadoSiguiente(enProceso); // Reapertura
            
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear estados", e.getMessage());
        }
    }

    private void seleccionarEstado(EstadoTicket estado) {
        estadoSeleccionado = estado;
        if (estado != null) {
            nombreEstadoField.setText(estado.getNombre());
            descripcionEstadoArea.setText(estado.getDescripcion());
            estadoFinalCheckBox.setSelected(estado.isEstadoFinal());
            actualizarEstadosSiguientes();
        } else {
            nombreEstadoField.clear();
            descripcionEstadoArea.clear();
            estadoFinalCheckBox.setSelected(false);
            estadosSiguientesAsignados.clear();
            estadosSiguientesDisponibles.clear();
        }
        actualizarBotonesEstadosSiguientes();
    }

    private void actualizarEstadosSiguientes() {
        if (estadoSeleccionado != null) {
            Set<EstadoTicket> asignados = estadoSeleccionado.getEstadosSiguientes();
            estadosSiguientesAsignados.setAll(asignados);
            
            List<EstadoTicket> disponibles = entityManager.createQuery(
                "SELECT e FROM EstadoTicket e WHERE e != :estado", EstadoTicket.class)
                .setParameter("estado", estadoSeleccionado)
                .getResultList();
            disponibles.removeAll(asignados);
            estadosSiguientesDisponibles.setAll(disponibles);
        }
    }

    private void actualizarBotonesEstadosSiguientes() {
        boolean estadoSeleccionado = this.estadoSeleccionado != null;
        agregarEstadoSiguienteButton.setDisable(!estadoSeleccionado || 
            estadosSiguientesDisponiblesListView.getSelectionModel().isEmpty());
        quitarEstadoSiguienteButton.setDisable(!estadoSeleccionado || 
            estadosSiguientesAsignadosListView.getSelectionModel().isEmpty());
    }

    private void nuevoEstado() {
        estadoSeleccionado = null;
        nombreEstadoField.clear();
        descripcionEstadoArea.clear();
        estadoFinalCheckBox.setSelected(false);
        estadosSiguientesAsignados.clear();
        actualizarEstadosSiguientes();
        actualizarBotonesEstadosSiguientes();
    }

    private void guardarEstado() {
        if (!validarDatos()) {
            return;
        }

        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            if (estadoSeleccionado == null) {
                estadoSeleccionado = new EstadoTicket();
            }
            
            estadoSeleccionado.setNombre(nombreEstadoField.getText().trim());
            estadoSeleccionado.setDescripcion(descripcionEstadoArea.getText().trim());
            estadoSeleccionado.setEstadoFinal(estadoFinalCheckBox.isSelected());
            estadoSeleccionado.getEstadosSiguientes().clear();
            estadoSeleccionado.getEstadosSiguientes().addAll(estadosSiguientesAsignados);
            
            if (estadoSeleccionado.getId() == null) {
                entityManager.persist(estadoSeleccionado);
            } else {
                entityManager.merge(estadoSeleccionado);
            }
            
            transaction.commit();
            mostrarMensaje("Estado guardado", "El estado se ha guardado exitosamente");
            cargarEstados();
            
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al guardar", e.getMessage());
        }
    }

    private void eliminarEstado() {
        if (estadoSeleccionado == null) {
            return;
        }

        // Verificar si hay tickets con este estado
        Long ticketsConEstado = entityManager.createQuery(
            "SELECT COUNT(t) FROM Ticket t WHERE t.estado = :estado", Long.class)
            .setParameter("estado", estadoSeleccionado)
            .getSingleResult();
            
        if (ticketsConEstado > 0) {
            mostrarError("No se puede eliminar", 
                "Existen tickets con este estado. Reasigne los tickets antes de eliminar el estado.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea eliminar el estado?");

        if (confirmacion.showAndWait().get() == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Eliminar referencias a este estado en otros estados
                entityManager.createQuery(
                    "UPDATE EstadoTicket e SET e.estadosSiguientes = " +
                    "SELECT es FROM e.estadosSiguientes es WHERE es != :estado")
                    .setParameter("estado", estadoSeleccionado)
                    .executeUpdate();
                
                entityManager.remove(
                    entityManager.contains(estadoSeleccionado) ? 
                    estadoSeleccionado : 
                    entityManager.merge(estadoSeleccionado)
                );
                
                transaction.commit();
                mostrarMensaje("Estado eliminado", "El estado se ha eliminado exitosamente");
                
                nuevoEstado();
                cargarEstados();
                
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar", e.getMessage());
            }
        }
    }

    private void agregarEstadoSiguiente() {
        EstadoTicket estado = estadosSiguientesDisponiblesListView.getSelectionModel().getSelectedItem();
        if (estado != null) {
            estadosSiguientesDisponibles.remove(estado);
            estadosSiguientesAsignados.add(estado);
        }
    }

    private void quitarEstadoSiguiente() {
        EstadoTicket estado = estadosSiguientesAsignadosListView.getSelectionModel().getSelectedItem();
        if (estado != null) {
            estadosSiguientesAsignados.remove(estado);
            estadosSiguientesDisponibles.add(estado);
        }
    }

    private boolean validarDatos() {
        if (nombreEstadoField.getText().trim().isEmpty()) {
            mostrarError("Error de validación", "El nombre del estado es obligatorio");
            return false;
        }
        
        // Verificar si el nombre ya existe (excepto para el mismo estado)
        EstadoTicket estadoExistente = entityManager.createQuery(
            "SELECT e FROM EstadoTicket e WHERE e.nombre = :nombre AND e.id != :id", 
            EstadoTicket.class)
            .setParameter("nombre", nombreEstadoField.getText().trim())
            .setParameter("id", estadoSeleccionado != null ? estadoSeleccionado.getId() : -1L)
            .getResultList()
            .stream()
            .collect(Collectors.toList())
            .stream()
            .findFirst()
            .orElse(null);
            
        if (estadoExistente != null) {
            mostrarError("Error de validación", "Ya existe un estado con ese nombre");
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