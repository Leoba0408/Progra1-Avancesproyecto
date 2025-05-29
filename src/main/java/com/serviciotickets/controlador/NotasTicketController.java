package com.serviciotickets.controlador;

import com.serviciotickets.modelo.NotaTicket;
import com.serviciotickets.modelo.Ticket;
import com.serviciotickets.persistencia.ConexionDB;
import com.serviciotickets.servicio.SesionServicio;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

public class NotasTicketController {
    @FXML
    private ListView<NotaTicket> notasListView;
    
    @FXML
    private TextArea contenidoNotaArea;
    
    @FXML
    private ListView<String> adjuntosListView;
    
    @FXML
    private Button agregarAdjuntoButton;
    
    @FXML
    private Button eliminarAdjuntoButton;
    
    @FXML
    private Button guardarNotaButton;
    
    @FXML
    private Button eliminarNotaButton;
    
    private EntityManager entityManager;
    private Ticket ticketActual;
    private NotaTicket notaSeleccionada;
    private ObservableList<String> adjuntos;

    public void setTicket(Ticket ticket) {
        this.ticketActual = ticket;
        cargarNotas();
    }

    @FXML
    public void initialize() {
        entityManager = ConexionDB.getEntityManagerFactory().createEntityManager();
        
        // Configurar lista de adjuntos
        adjuntos = FXCollections.observableArrayList();
        adjuntosListView.setItems(adjuntos);
        
        // Configurar selección de notas
        notasListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> seleccionarNota(newVal));
        
        // Configurar botones
        agregarAdjuntoButton.setOnAction(e -> agregarAdjunto());
        eliminarAdjuntoButton.setOnAction(e -> eliminarAdjunto());
        guardarNotaButton.setOnAction(e -> guardarNota());
        eliminarNotaButton.setOnAction(e -> eliminarNota());
        
        // Configurar formato de celdas
        notasListView.setCellFactory(lv -> new ListCell<NotaTicket>() {
            @Override
            protected void updateItem(NotaTicket nota, boolean empty) {
                super.updateItem(nota, empty);
                if (empty || nota == null) {
                    setText(null);
                } else {
                    setText(nota.getCreadoPor() + " - " + 
                           nota.getFechaCreacion().toString());
                }
            }
        });
    }

    private void cargarNotas() {
        if (ticketActual != null) {
            List<NotaTicket> notas = entityManager.createQuery(
                "SELECT n FROM NotaTicket n WHERE n.ticket = :ticket ORDER BY n.fechaCreacion DESC", 
                NotaTicket.class)
                .setParameter("ticket", ticketActual)
                .getResultList();
            notasListView.setItems(FXCollections.observableArrayList(notas));
        }
    }

    private void seleccionarNota(NotaTicket nota) {
        notaSeleccionada = nota;
        if (nota != null) {
            contenidoNotaArea.setText(nota.getContenido());
            adjuntos.setAll(nota.getAdjuntos());
        } else {
            contenidoNotaArea.clear();
            adjuntos.clear();
        }
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean notaSeleccionada = this.notaSeleccionada != null;
        eliminarNotaButton.setDisable(!notaSeleccionada);
        eliminarAdjuntoButton.setDisable(!notaSeleccionada || 
            adjuntosListView.getSelectionModel().isEmpty());
    }

    private void agregarAdjunto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Adjunto");
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                // Validar tamaño (máximo 10MB)
                if (file.length() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("El archivo no debe superar los 10MB");
                }
                
                // Crear directorio de adjuntos si no existe
                Path adjuntosDir = Paths.get("adjuntos");
                if (!Files.exists(adjuntosDir)) {
                    Files.createDirectory(adjuntosDir);
                }
                
                // Copiar archivo al directorio de adjuntos
                String fileName = System.currentTimeMillis() + "_" + file.getName();
                Path destino = adjuntosDir.resolve(fileName);
                Files.copy(file.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
                
                adjuntos.add(destino.toString());
            } catch (Exception e) {
                mostrarError("Error al agregar adjunto", e.getMessage());
            }
        }
    }

    private void eliminarAdjunto() {
        String adjunto = adjuntosListView.getSelectionModel().getSelectedItem();
        if (adjunto != null) {
            try {
                Files.deleteIfExists(Paths.get(adjunto));
                adjuntos.remove(adjunto);
            } catch (Exception e) {
                mostrarError("Error al eliminar adjunto", e.getMessage());
            }
        }
    }

    private void guardarNota() {
        if (!validarDatos()) {
            return;
        }

        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            if (notaSeleccionada == null) {
                notaSeleccionada = new NotaTicket();
                notaSeleccionada.setTicket(ticketActual);
                notaSeleccionada.setCreadoPor(SesionServicio.getInstancia().getUsuarioActual());
                notaSeleccionada.setFechaCreacion(LocalDateTime.now());
            }
            
            notaSeleccionada.setContenido(contenidoNotaArea.getText().trim());
            notaSeleccionada.getAdjuntos().clear();
            notaSeleccionada.getAdjuntos().addAll(adjuntos);
            
            if (notaSeleccionada.getId() == null) {
                entityManager.persist(notaSeleccionada);
            } else {
                entityManager.merge(notaSeleccionada);
            }
            
            transaction.commit();
            mostrarMensaje("Nota guardada", "La nota se ha guardado exitosamente");
            cargarNotas();
            
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al guardar", e.getMessage());
        }
    }

    private void eliminarNota() {
        if (notaSeleccionada == null) {
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea eliminar la nota?");

        if (confirmacion.showAndWait().get() == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Eliminar adjuntos físicos
                for (String adjunto : notaSeleccionada.getAdjuntos()) {
                    Files.deleteIfExists(Paths.get(adjunto));
                }
                
                entityManager.remove(
                    entityManager.contains(notaSeleccionada) ? 
                    notaSeleccionada : 
                    entityManager.merge(notaSeleccionada)
                );
                
                transaction.commit();
                mostrarMensaje("Nota eliminada", "La nota se ha eliminado exitosamente");
                
                notaSeleccionada = null;
                contenidoNotaArea.clear();
                adjuntos.clear();
                cargarNotas();
                
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar", e.getMessage());
            }
        }
    }

    private boolean validarDatos() {
        if (ticketActual == null) {
            mostrarError("Error de validación", "No hay un ticket seleccionado");
            return false;
        }
        
        if (contenidoNotaArea.getText().trim().isEmpty()) {
            mostrarError("Error de validación", "El contenido de la nota es obligatorio");
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