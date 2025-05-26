package com.serviciotickets.controlador;

import com.serviciotickets.modelo.ConfiguracionSistema;
import com.serviciotickets.persistencia.ConexionDB;
import javax.persistence.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfiguracionSistemaController {
    private static final Logger logger = Logger.getLogger(ConfiguracionSistemaController.class.getName());
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String LOGOS_DIR = "logos";
    
    @FXML
    private TextField nombreEmpresaField;
    
    @FXML
    private ImageView logoImageView;
    
    @FXML
    private ComboBox<String> idiomaComboBox;
    
    @FXML
    private ComboBox<String> zonaHorariaComboBox;
    
    @FXML
    private Spinner<Integer> tiempoVencimientoSpinner;
    
    @FXML
    private ListView<String> nivelesPrioridadListView;
    
    @FXML
    private Button agregarPrioridadButton;
    
    @FXML
    private Button eliminarPrioridadButton;
    
    @FXML
    private Button guardarButton;
    
    private ConfiguracionSistema configuracion;
    private String logoPath;
    private EntityManager entityManager;
    private Path logosDirectorio;

    @FXML
    public void initialize() {
        try {
            entityManager = ConexionDB.getEntityManagerFactory().createEntityManager();
            inicializarDirectorioLogos();
            configurarComponentes();
            cargarConfiguracion();
            configurarEventos();
        } catch (Exception e) {
            logger.severe("Error al inicializar el controlador: " + e.getMessage());
            mostrarError("Error de Inicialización", 
                "No se pudo inicializar el controlador de configuración");
        }
    }

    private void inicializarDirectorioLogos() throws IOException {
        logosDirectorio = Paths.get(System.getProperty("user.dir"), LOGOS_DIR);
        if (!Files.exists(logosDirectorio)) {
            Files.createDirectories(logosDirectorio);
        }
    }

    private void configurarComponentes() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 30);
        tiempoVencimientoSpinner.setValueFactory(valueFactory);
        
        idiomaComboBox.getItems().addAll("Español", "Inglés");
        
        zonaHorariaComboBox.getItems().addAll(
            ZoneId.getAvailableZoneIds().stream()
                .sorted()
                .collect(Collectors.toList())
        );
    }

    private void configurarEventos() {
        agregarPrioridadButton.setOnAction(e -> agregarNivelPrioridad());
        eliminarPrioridadButton.setOnAction(e -> eliminarNivelPrioridad());
        guardarButton.setOnAction(e -> guardarConfiguracion());
        logoImageView.setOnMouseClicked(e -> seleccionarLogo());
    }

    private void cargarConfiguracion() {
        try {
            configuracion = entityManager.createQuery(
                "SELECT c FROM ConfiguracionSistema c", ConfiguracionSistema.class)
                .getSingleResult();
            
            actualizarInterfazConConfiguracion();
            
        } catch (NoResultException nre) {
            logger.info("No existe configuración previa, creando configuración por defecto");
            crearConfiguracionPorDefecto();
        } catch (PersistenceException pe) {
            logger.severe("Error al cargar la configuración: " + pe.getMessage());
            mostrarError("Error de Base de Datos", 
                "No se pudo cargar la configuración del sistema");
        }
    }

    private void actualizarInterfazConConfiguracion() {
        nombreEmpresaField.setText(configuracion.getNombreEmpresa());
        idiomaComboBox.setValue(configuracion.getIdiomaPredeterminado());
        zonaHorariaComboBox.setValue(configuracion.getZonaHoraria());
        tiempoVencimientoSpinner.getValueFactory().setValue(
            configuracion.getTiempoVencimientoTickets());
        
        nivelesPrioridadListView.getItems().clear();
        if (configuracion.getNivelesPrioridad() != null) {
            nivelesPrioridadListView.getItems().addAll(configuracion.getNivelesPrioridad());
        }
        
        if (configuracion.getLogoPath() != null) {
            cargarLogo(configuracion.getLogoPath());
        }
    }

    private void crearConfiguracionPorDefecto() {
        configuracion = new ConfiguracionSistema();
        configuracion.setNivelesPrioridad(Arrays.asList("Alta", "Media", "Baja"));
        nivelesPrioridadListView.getItems().addAll(configuracion.getNivelesPrioridad());
    }

    private void agregarNivelPrioridad() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Nivel de Prioridad");
        dialog.setHeaderText("Ingrese el nombre del nuevo nivel de prioridad");
        dialog.setContentText("Nombre:");

        dialog.showAndWait().ifPresent(nombre -> {
            if (esNombrePrioridadValido(nombre)) {
                nivelesPrioridadListView.getItems().add(nombre.trim());
            }
        });
    }

    private boolean esNombrePrioridadValido(String nombre) {
        String nombreLimpio = nombre.trim();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > 50) {
            mostrarError("Error de validación", 
                "El nombre debe tener entre 1 y 50 caracteres");
            return false;
        }
        if (!nombreLimpio.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
            mostrarError("Error de validación", 
                "El nombre solo puede contener letras y espacios");
            return false;
        }
        if (nivelesPrioridadListView.getItems().contains(nombreLimpio)) {
            mostrarError("Error de validación", 
                "Este nivel de prioridad ya existe");
            return false;
        }
        return true;
    }

    private void eliminarNivelPrioridad() {
        String seleccionado = nivelesPrioridadListView.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            if (nivelesPrioridadListView.getItems().size() > 3) {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar eliminación");
                confirmacion.setHeaderText("¿Está seguro de eliminar este nivel de prioridad?");
                confirmacion.setContentText("Esta acción no se puede deshacer.");

                confirmacion.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        nivelesPrioridadListView.getItems().remove(seleccionado);
                    }
                });
            } else {
                mostrarError("No se puede eliminar", 
                    "Debe mantener al menos tres niveles de prioridad");
            }
        }
    }

    private void seleccionarLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Logo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                validarArchivo(file);
                guardarNuevoLogo(file);
            } catch (Exception e) {
                logger.warning("Error al seleccionar logo: " + e.getMessage());
                mostrarError("Error al cargar logo", e.getMessage());
            }
        }
    }

    private void validarArchivo(File file) throws IOException {
        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo no debe superar los 2MB");
        }
        
        String mimeType = Files.probeContentType(file.toPath());
        if (!Arrays.asList("image/jpeg", "image/png").contains(mimeType)) {
            throw new IllegalArgumentException("El archivo debe ser una imagen JPG o PNG");
        }
    }

    private void guardarNuevoLogo(File file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getName();
        Path destino = logosDirectorio.resolve(fileName);
        
        // Eliminar logo anterior si existe
        if (logoPath != null) {
            try {
                Files.deleteIfExists(Paths.get(logoPath));
            } catch (IOException e) {
                logger.warning("No se pudo eliminar el logo anterior: " + e.getMessage());
            }
        }
        
        Files.copy(file.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        logoPath = destino.toString();
        cargarLogo(logoPath);
    }

    private void cargarLogo(String path) {
        try {
            Image image = new Image(new File(path).toURI().toString());
            if (image.isError()) {
                throw new IOException("No se pudo cargar la imagen");
            }
            
            logoImageView.setImage(image);
            logoImageView.setFitWidth(200);
            logoImageView.setFitHeight(200);
            logoImageView.setPreserveRatio(true);
        } catch (Exception e) {
            logger.warning("Error al cargar logo: " + e.getMessage());
            mostrarError("Error al cargar logo", 
                "No se pudo cargar la imagen del logo");
            logoImageView.setImage(null);
        }
    }

    private void guardarConfiguracion() {
        if (!validarDatos()) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ejecutarTransaccion(() -> {
                    actualizarConfiguracion();
                    if (configuracion.getId() == null) {
                        entityManager.persist(configuracion);
                    } else {
                        entityManager.merge(configuracion);
                    }
                });
                
                Platform.runLater(() -> 
                    mostrarMensaje("Configuración guardada", 
                        "La configuración se ha guardado exitosamente"));
                return null;
            }
        };

        task.setOnFailed(e -> {
            logger.severe("Error al guardar la configuración: " + 
                task.getException().getMessage());
            Platform.runLater(() -> 
                mostrarError("Error al guardar", 
                    "No se pudo guardar la configuración"));
        });

        new Thread(task).start();
    }

    private void ejecutarTransaccion(Runnable operacion) {
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            operacion.run();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    private void actualizarConfiguracion() {
        configuracion.setNombreEmpresa(nombreEmpresaField.getText().trim());
        configuracion.setIdiomaPredeterminado(idiomaComboBox.getValue());
        configuracion.setZonaHoraria(zonaHorariaComboBox.getValue());
        configuracion.setTiempoVencimientoTickets(tiempoVencimientoSpinner.getValue());
        configuracion.setNivelesPrioridad(new ArrayList<>(nivelesPrioridadListView.getItems()));
        
        if (logoPath != null) {
            configuracion.setLogoPath(logoPath);
        }
    }

    private boolean validarDatos() {
        List<String> errores = new ArrayList<>();
        
        if (nombreEmpresaField.getText().trim().isEmpty()) {
            errores.add("El nombre de la empresa es obligatorio");
        }
        
        if (idiomaComboBox.getValue() == null) {
            errores.add("Debe seleccionar un idioma");
        }
        
        if (zonaHorariaComboBox.getValue() == null) {
            errores.add("Debe seleccionar una zona horaria");
        }
        
        if (nivelesPrioridadListView.getItems().size() < 3) {
            errores.add("Debe definir al menos tres niveles de prioridad");
        }
        
        if (!errores.isEmpty()) {
            mostrarErroresValidacion("Errores de validación", errores);
            return false;
        }
        
        return true;
    }

    private void mostrarErroresValidacion(String titulo, List<String> errores) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(String.join("\n", errores));
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    public void dispose() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }
} 