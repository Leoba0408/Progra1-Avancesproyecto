package com.serviciotickets.controlador;

import com.serviciotickets.modelo.Rol;
import com.serviciotickets.modelo.Permiso;
import com.serviciotickets.persistencia.ConexionDB;
import javax.persistence.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Logger;

public class RolesPermisosController {
    private static final Logger logger = Logger.getLogger(RolesPermisosController.class.getName());

    @FXML
    private ListView<Rol> rolesListView;
    
    @FXML
    private TextField nombreRolField;
    
    @FXML
    private TextArea descripcionRolArea;
    
    @FXML
    private ListView<Permiso> permisosDisponiblesListView;
    
    @FXML
    private ListView<Permiso> permisosAsignadosListView;
    
    @FXML
    private Button agregarPermisoButton;
    
    @FXML
    private Button quitarPermisoButton;
    
    @FXML
    private Button nuevoRolButton;
    
    @FXML
    private Button guardarRolButton;
    
    @FXML
    private Button eliminarRolButton;
    
    private EntityManager entityManager;
    private Rol rolSeleccionado;
    private ObservableList<Permiso> permisosDisponibles;
    private ObservableList<Permiso> permisosAsignados;

    @FXML
    public void initialize() {
        try {
            entityManager = ConexionDB.getEntityManagerFactory().createEntityManager();
            inicializarListas();
            configurarEventos();
            configurarCeldasPersonalizadas();
            cargarDatosIniciales();
        } catch (Exception e) {
            logger.severe("Error al inicializar el controlador: " + e.getMessage());
            mostrarError("Error de Inicialización", 
                "No se pudo inicializar el controlador de roles y permisos");
        }
    }

    private void inicializarListas() {
        permisosDisponibles = FXCollections.observableArrayList();
        permisosAsignados = FXCollections.observableArrayList();
        
        permisosDisponiblesListView.setItems(permisosDisponibles);
        permisosAsignadosListView.setItems(permisosAsignados);
    }

    private void configurarEventos() {
        rolesListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> seleccionarRol(newVal));
        
        nuevoRolButton.setOnAction(e -> nuevoRol());
        guardarRolButton.setOnAction(e -> guardarRol());
        eliminarRolButton.setOnAction(e -> eliminarRol());
        agregarPermisoButton.setOnAction(e -> agregarPermiso());
        quitarPermisoButton.setOnAction(e -> quitarPermiso());
        
        // Configurar listeners para habilitar/deshabilitar botones
        permisosDisponiblesListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> actualizarBotonesPermisos());
        permisosAsignadosListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> actualizarBotonesPermisos());
    }

    private void configurarCeldasPersonalizadas() {
        rolesListView.setCellFactory(lv -> new ListCell<Rol>() {
            @Override
            protected void updateItem(Rol rol, boolean empty) {
                super.updateItem(rol, empty);
                setText(empty || rol == null ? null : rol.getNombre());
            }
        });
        
        ListCell<Permiso> permisoCell = new ListCell<Permiso>() {
            @Override
            protected void updateItem(Permiso permiso, boolean empty) {
                super.updateItem(permiso, empty);
                setText(empty || permiso == null ? null : permiso.getNombre());
            }
        };
        
        permisosDisponiblesListView.setCellFactory(lv -> permisoCell);
        permisosAsignadosListView.setCellFactory(lv -> permisoCell);
    }

    private void cargarDatosIniciales() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                cargarRoles();
                cargarPermisos();
                return null;
            }
        };
        
        task.setOnFailed(e -> {
            logger.severe("Error al cargar datos iniciales: " + task.getException().getMessage());
            Platform.runLater(() -> mostrarError("Error", 
                "No se pudieron cargar los datos iniciales"));
        });
        
        new Thread(task).start();
    }

    private void cargarRoles() {
        try {
            List<Rol> roles = entityManager.createQuery("SELECT r FROM Rol r", Rol.class)
                .getResultList();
            Platform.runLater(() -> rolesListView.setItems(FXCollections.observableArrayList(roles)));
        } catch (PersistenceException pe) {
            logger.severe("Error al cargar roles: " + pe.getMessage());
            Platform.runLater(() -> mostrarError("Error", "No se pudieron cargar los roles"));
        }
    }

    private void cargarPermisos() {
        try {
            final List<Permiso> permisosIniciales = entityManager.createQuery(
                "SELECT p FROM Permiso p", Permiso.class)
                .getResultList();
                
            if (permisosIniciales.isEmpty()) {
                crearPermisosIniciales();
                final List<Permiso> permisosActualizados = entityManager.createQuery(
                    "SELECT p FROM Permiso p", Permiso.class)
                    .getResultList();
                Platform.runLater(() -> permisosDisponibles.setAll(permisosActualizados));
            } else {
                Platform.runLater(() -> permisosDisponibles.setAll(permisosIniciales));
            }
        } catch (PersistenceException pe) {
            logger.severe("Error al cargar permisos: " + pe.getMessage());
            Platform.runLater(() -> mostrarError("Error", "No se pudieron cargar los permisos"));
        }
    }

    private void crearPermisosIniciales() {
        ejecutarTransaccion(() -> {
            String[][] permisosData = {
                {"CREAR_TICKET", "Permite crear tickets"},
                {"VER_TICKET", "Permite ver tickets"},
                {"EDITAR_TICKET", "Permite editar tickets"},
                {"ELIMINAR_TICKET", "Permite eliminar tickets"},
                {"ASIGNAR_TICKET", "Permite asignar tickets"},
                {"CAMBIAR_ESTADO_TICKET", "Permite cambiar el estado de tickets"},
                {"AGREGAR_NOTA_TICKET", "Permite agregar notas a tickets"},
                {"GESTIONAR_USUARIOS", "Permite gestionar usuarios"},
                {"GESTIONAR_DEPARTAMENTOS", "Permite gestionar departamentos"},
                {"GESTIONAR_FLUJOS", "Permite gestionar flujos de trabajo"},
                {"CONFIGURAR_SISTEMA", "Permite configurar parámetros del sistema"}
            };
            
            for (String[] data : permisosData) {
                Permiso permiso = new Permiso(data[0], data[1]);
                entityManager.persist(permiso);
            }
        }, "Error al crear permisos iniciales");
    }

    private void seleccionarRol(Rol rol) {
        rolSeleccionado = rol;
        if (rol != null) {
            nombreRolField.setText(rol.getNombre());
            descripcionRolArea.setText(rol.getDescripcion());
            actualizarPermisosAsignados();
        } else {
            nombreRolField.clear();
            descripcionRolArea.clear();
            permisosAsignados.clear();
        }
        actualizarBotonesPermisos();
    }

    private void actualizarPermisosAsignados() {
        if (rolSeleccionado != null) {
            Set<Permiso> asignados = rolSeleccionado.getPermisos();
            permisosAsignados.setAll(asignados);
            permisosDisponibles.removeAll(asignados);
        }
    }

    private void actualizarBotonesPermisos() {
        boolean rolSeleccionado = this.rolSeleccionado != null;
        boolean permisoDisponibleSeleccionado = !permisosDisponiblesListView.getSelectionModel().isEmpty();
        boolean permisoAsignadoSeleccionado = !permisosAsignadosListView.getSelectionModel().isEmpty();
        
        Platform.runLater(() -> {
            agregarPermisoButton.setDisable(!rolSeleccionado || !permisoDisponibleSeleccionado);
            quitarPermisoButton.setDisable(!rolSeleccionado || !permisoAsignadoSeleccionado);
            guardarRolButton.setDisable(!rolSeleccionado && nombreRolField.getText().trim().isEmpty());
            eliminarRolButton.setDisable(!rolSeleccionado);
        });
    }

    private void nuevoRol() {
        rolSeleccionado = null;
        nombreRolField.clear();
        descripcionRolArea.clear();
        permisosAsignados.clear();
        cargarPermisos();
        actualizarBotonesPermisos();
    }

    private void guardarRol() {
        if (!validarDatos()) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ejecutarTransaccion(() -> {
                    if (rolSeleccionado == null) {
                        rolSeleccionado = new Rol();
                    }
                    
                    rolSeleccionado.setNombre(nombreRolField.getText().trim());
                    rolSeleccionado.setDescripcion(descripcionRolArea.getText().trim());
                    rolSeleccionado.getPermisos().clear();
                    rolSeleccionado.getPermisos().addAll(permisosAsignados);
                    
                    if (rolSeleccionado.getId() == null) {
                        entityManager.persist(rolSeleccionado);
                    } else {
                        entityManager.merge(rolSeleccionado);
                    }
                }, "Error al guardar el rol");
                
                Platform.runLater(() -> {
                    mostrarMensaje("Rol guardado", "El rol se ha guardado exitosamente");
                    cargarRoles();
                });
                
                return null;
            }
        };
        
        task.setOnFailed(e -> {
            logger.severe("Error al guardar rol: " + task.getException().getMessage());
            Platform.runLater(() -> mostrarError("Error", 
                "No se pudo guardar el rol"));
        });
        
        new Thread(task).start();
    }

    private void eliminarRol() {
        if (rolSeleccionado == null) {
            return;
        }

        Task<Boolean> validacionTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Long usuariosConRol = entityManager.createQuery(
                    "SELECT COUNT(u) FROM Usuario u WHERE u.rol = :rol", Long.class)
                    .setParameter("rol", rolSeleccionado)
                    .getSingleResult();
                return usuariosConRol == 0;
            }
        };
        
        validacionTask.setOnSucceeded(e -> {
            if (validacionTask.getValue()) {
                confirmarYEliminarRol();
            } else {
                mostrarError("No se puede eliminar", 
                    "Existen usuarios con este rol asignado. Reasigne los usuarios antes de eliminar el rol.");
            }
        });
        
        validacionTask.setOnFailed(e -> {
            logger.severe("Error al validar rol: " + validacionTask.getException().getMessage());
            mostrarError("Error", "No se pudo validar el rol para eliminación");
        });
        
        new Thread(validacionTask).start();
    }

    private void confirmarYEliminarRol() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea eliminar el rol?");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ejecutarTransaccion(() -> {
                    entityManager.remove(
                        entityManager.contains(rolSeleccionado) ? 
                        rolSeleccionado : 
                        entityManager.merge(rolSeleccionado)
                    );
                }, "Error al eliminar el rol");
                
                mostrarMensaje("Rol eliminado", "El rol se ha eliminado exitosamente");
                nuevoRol();
                cargarRoles();
            }
        });
    }

    private void agregarPermiso() {
        Permiso permiso = permisosDisponiblesListView.getSelectionModel().getSelectedItem();
        if (permiso != null) {
            permisosDisponibles.remove(permiso);
            permisosAsignados.add(permiso);
            actualizarBotonesPermisos();
        }
    }

    private void quitarPermiso() {
        Permiso permiso = permisosAsignadosListView.getSelectionModel().getSelectedItem();
        if (permiso != null) {
            permisosAsignados.remove(permiso);
            permisosDisponibles.add(permiso);
            actualizarBotonesPermisos();
        }
    }

    private boolean validarDatos() {
        List<String> errores = new ArrayList<>();
        
        String nombre = nombreRolField.getText().trim();
        if (nombre.isEmpty()) {
            errores.add("El nombre del rol es obligatorio");
        } else if (nombre.length() > 50) {
            errores.add("El nombre del rol no puede exceder los 50 caracteres");
        }
        
        if (!errores.isEmpty()) {
            mostrarErroresValidacion("Error de validación", errores);
            return false;
        }
        
        try {
            Rol rolExistente = entityManager.createQuery(
                "SELECT r FROM Rol r WHERE r.nombre = :nombre AND r.id != :id", 
                Rol.class)
                .setParameter("nombre", nombre)
                .setParameter("id", rolSeleccionado != null ? rolSeleccionado.getId() : -1L)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
                
            if (rolExistente != null) {
                mostrarError("Error de validación", "Ya existe un rol con ese nombre");
                return false;
            }
        } catch (PersistenceException pe) {
            logger.severe("Error al validar rol existente: " + pe.getMessage());
            mostrarError("Error", "No se pudo validar el nombre del rol");
            return false;
        }
        
        return true;
    }

    private void ejecutarTransaccion(Runnable operacion, String mensajeError) {
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
            logger.severe(mensajeError + ": " + e.getMessage());
            throw new RuntimeException(mensajeError, e);
        }
    }

    private void mostrarErroresValidacion(String titulo, List<String> errores) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(String.join("\n", errores));
            alert.showAndWait();
        });
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
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    @FXML
    public void dispose() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }
} 