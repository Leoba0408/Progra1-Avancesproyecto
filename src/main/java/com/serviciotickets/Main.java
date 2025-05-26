package com.serviciotickets;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.serviciotickets.modelo.*;
import com.serviciotickets.persistencia.ConexionDB;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private VBox root;
    private TabPane tabPane;
    private EntityManager entityManager;
    private TableView<Ticket> ticketsTable;
    private TableView<Usuario> usuariosTable;
    private TableView<Departamento> departamentosTable;

    @Override
    public void start(Stage primaryStage) {
        try {
            entityManager = ConexionDB.getEntityManagerFactory().createEntityManager();
            inicializarUI(primaryStage);
        } catch (Exception e) {
            mostrarError("Error al iniciar la aplicación", e.getMessage());
            System.exit(1);
        }
    }

    private void inicializarUI(Stage primaryStage) {
        root = new VBox(10);
        root.setPadding(new Insets(10));

        // Crear menú
        MenuBar menuBar = crearMenuBar();
        root.getChildren().add(menuBar);

        // Crear pestañas
        tabPane = new TabPane();
        tabPane.getTabs().addAll(
            crearTabTickets(),
            crearTabUsuarios(),
            crearTabDepartamentos()
        );
        root.getChildren().add(tabPane);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Sistema de Tickets de Servicio");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar crearMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu archivoMenu = new Menu("Archivo");
        MenuItem salirItem = new MenuItem("Salir");
        salirItem.setOnAction(e -> System.exit(0));
        archivoMenu.getItems().add(salirItem);

        Menu ayudaMenu = new Menu("Ayuda");
        MenuItem acercaDeItem = new MenuItem("Acerca de");
        acercaDeItem.setOnAction(e -> mostrarAcercaDe());
        ayudaMenu.getItems().add(acercaDeItem);

        menuBar.getMenus().addAll(archivoMenu, ayudaMenu);
        return menuBar;
    }

    private Tab crearTabTickets() {
        Tab tab = new Tab("Tickets");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Formulario de nuevo ticket
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        formGrid.add(new Label("Título:"), 0, 0);
        TextField tituloField = new TextField();
        formGrid.add(tituloField, 1, 0);

        formGrid.add(new Label("Descripción:"), 0, 1);
        TextArea descripcionArea = new TextArea();
        descripcionArea.setPrefRowCount(3);
        formGrid.add(descripcionArea, 1, 1);

        formGrid.add(new Label("Prioridad:"), 0, 2);
        ComboBox<Ticket.Prioridad> prioridadCombo = new ComboBox<>();
        prioridadCombo.getItems().addAll(Ticket.Prioridad.values());
        formGrid.add(prioridadCombo, 1, 2);

        Button crearButton = new Button("Crear Ticket");
        crearButton.setOnAction(e -> {
            crearTicket(tituloField.getText(),
                        descripcionArea.getText(),
                        prioridadCombo.getValue());
            cargarTickets(ticketsTable);
        });

        content.getChildren().addAll(formGrid, crearButton);

        ticketsTable = new TableView<>();
        TableColumn<Ticket, String> tituloCol = new TableColumn<>("Título");
        TableColumn<Ticket, String> estadoCol = new TableColumn<>("Estado");
        TableColumn<Ticket, String> prioridadCol = new TableColumn<>("Prioridad");
        ticketsTable.getColumns().add(tituloCol);
        ticketsTable.getColumns().add(estadoCol);
        ticketsTable.getColumns().add(prioridadCol);

        // Configurar celdas para mostrar los valores correctos
        tituloCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitulo()));
        estadoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado().toString()));
        prioridadCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPrioridad().toString()));

        content.getChildren().add(ticketsTable);
        tab.setContent(content);

        // Cargar tickets al abrir la pestaña
        cargarTickets(ticketsTable);

        return tab;
    }

    private void cargarTickets(TableView<Ticket> ticketsTable) {
        ticketsTable.getItems().clear();
        java.util.List<Ticket> tickets = entityManager.createQuery("SELECT t FROM Ticket t", Ticket.class).getResultList();
        ticketsTable.getItems().addAll(tickets);
    }

    private Tab crearTabUsuarios() {
        Tab tab = new Tab("Usuarios");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Formulario de nuevo usuario
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);

        formGrid.add(new Label("Apellido:"), 0, 1);
        TextField apellidoField = new TextField();
        formGrid.add(apellidoField, 1, 1);

        formGrid.add(new Label("Email:"), 0, 2);
        TextField emailField = new TextField();
        formGrid.add(emailField, 1, 2);

        formGrid.add(new Label("Nombre de Usuario:"), 0, 3);
        TextField nombreUsuarioField = new TextField();
        formGrid.add(nombreUsuarioField, 1, 3);

        Button crearButton = new Button("Crear Usuario");
        crearButton.setOnAction(e -> {
            crearUsuario(nombreField.getText(),
                         apellidoField.getText(),
                         emailField.getText(),
                         nombreUsuarioField.getText());
            cargarUsuarios(usuariosTable);
        });

        content.getChildren().addAll(formGrid, crearButton);

        usuariosTable = new TableView<>();
        TableColumn<Usuario, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Usuario, String> apellidoCol = new TableColumn<>("Apellido");
        TableColumn<Usuario, String> emailCol = new TableColumn<>("Email");
        TableColumn<Usuario, String> nombreUsuarioCol = new TableColumn<>("Nombre de Usuario");
        usuariosTable.getColumns().add(nombreCol);
        usuariosTable.getColumns().add(apellidoCol);
        usuariosTable.getColumns().add(emailCol);
        usuariosTable.getColumns().add(nombreUsuarioCol);

        nombreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        apellidoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApellido()));
        emailCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        nombreUsuarioCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombreUsuario()));

        content.getChildren().add(usuariosTable);
        tab.setContent(content);

        cargarUsuarios(usuariosTable);

        return tab;
    }

    private void cargarUsuarios(TableView<Usuario> usuariosTable) {
        usuariosTable.getItems().clear();
        java.util.List<Usuario> usuarios = entityManager.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
        usuariosTable.getItems().addAll(usuarios);
    }

    private Tab crearTabDepartamentos() {
        Tab tab = new Tab("Departamentos");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Formulario de nuevo departamento
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);

        formGrid.add(new Label("Descripción:"), 0, 1);
        TextArea descripcionArea = new TextArea();
        descripcionArea.setPrefRowCount(3);
        formGrid.add(descripcionArea, 1, 1);

        Button crearButton = new Button("Crear Departamento");
        crearButton.setOnAction(e -> {
            crearDepartamento(nombreField.getText(),
                              descripcionArea.getText());
            cargarDepartamentos(departamentosTable);
        });

        content.getChildren().addAll(formGrid, crearButton);

        departamentosTable = new TableView<>();
        TableColumn<Departamento, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Departamento, String> descripcionCol = new TableColumn<>("Descripción");
        departamentosTable.getColumns().add(nombreCol);
        departamentosTable.getColumns().add(descripcionCol);

        nombreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        descripcionCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcion()));

        content.getChildren().add(departamentosTable);
        tab.setContent(content);

        cargarDepartamentos(departamentosTable);

        return tab;
    }

    private void cargarDepartamentos(TableView<Departamento> departamentosTable) {
        departamentosTable.getItems().clear();
        java.util.List<Departamento> departamentos = entityManager.createQuery("SELECT d FROM Departamento d", Departamento.class).getResultList();
        departamentosTable.getItems().addAll(departamentos);
    }

    private void crearTicket(String titulo, String descripcion, Ticket.Prioridad prioridad) {
        if (titulo == null || titulo.trim().isEmpty() || descripcion == null || descripcion.trim().isEmpty() || prioridad == null) {
            mostrarError("Error de validación", "Todos los campos son obligatorios");
            return;
        }

        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            // Buscar un departamento por defecto
            Departamento departamento = entityManager.createQuery(
                "SELECT d FROM Departamento d WHERE d.nombre = 'Soporte'", Departamento.class)
                .getSingleResult();
            
            // Buscar un usuario por defecto
            Usuario usuario = entityManager.createQuery(
                "SELECT u FROM Usuario u WHERE u.email = 'admin@example.com'", Usuario.class)
                .getSingleResult();
            
            Ticket ticket = new Ticket(titulo, descripcion, prioridad, departamento);
            ticket.setSolicitante((Persona)usuario);
            entityManager.persist(ticket);
            
            transaction.commit();
            mostrarMensaje("Ticket creado exitosamente");
        } catch (NoResultException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear ticket", "No se encontró el departamento o usuario por defecto");
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear ticket", e.getMessage());
        }
    }

    private void crearUsuario(String nombre, String apellido, String email, String nombreUsuario) {
        try {
            // Limpiar espacios en blanco
            nombre = nombre != null ? nombre.trim() : "";
            apellido = apellido != null ? apellido.trim() : "";
            email = email != null ? email.trim() : "";
            nombreUsuario = nombreUsuario != null ? nombreUsuario.trim() : "";

            // Validaciones
            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || nombreUsuario.isEmpty()) {
                mostrarError("Error de validación", "Todos los campos son obligatorios");
                return;
            }

            // Validar formato de email
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                mostrarError("Error de validación", "El formato del email no es válido");
                return;
            }

            // Validar longitud del nombre de usuario
            if (nombreUsuario.length() < 5 || nombreUsuario.length() > 30) {
                mostrarError("Error de validación", "El nombre de usuario debe tener entre 5 y 30 caracteres");
                return;
            }

            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Verificar si el email ya existe
                if (emailExiste(email)) {
                    throw new IllegalArgumentException("El email ya está registrado");
                }

                // Verificar si el nombre de usuario ya existe
                if (nombreUsuarioExiste(nombreUsuario)) {
                    throw new IllegalArgumentException("El nombre de usuario ya está registrado");
                }
                
                // Buscar o crear el rol por defecto
                Rol rolUsuario = obtenerRolPorDefecto();
                
                // Crear el usuario usando el constructor apropiado
                Usuario usuario = new Usuario(nombre, apellido, email, nombreUsuario, "Usuario123!");
                usuario.setRol(rolUsuario);
                usuario.setActivo(true);
                
                // Persistir y forzar la escritura
                entityManager.persist(usuario);
                entityManager.flush();
                
                transaction.commit();
                mostrarMensaje("Usuario creado exitosamente");
                
                // Limpiar los campos del formulario
                limpiarCamposUsuario();
                
                // Actualizar la tabla
                cargarUsuarios(usuariosTable);
                
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        } catch (Exception e) {
            mostrarError("Error al crear usuario", e.getMessage());
        }
    }

    private boolean emailExiste(String email) {
        try {
            entityManager.createQuery(
                "SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class)
                .setParameter("email", email)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean nombreUsuarioExiste(String nombreUsuario) {
        try {
            entityManager.createQuery(
                "SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombreUsuario", Usuario.class)
                .setParameter("nombreUsuario", nombreUsuario)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private Rol obtenerRolPorDefecto() {
        try {
            return entityManager.createQuery(
                "SELECT r FROM Rol r WHERE r.nombre = 'USUARIO'", Rol.class)
                .getSingleResult();
        } catch (NoResultException e) {
            // Crear permisos básicos si no existen
            List<Permiso> permisosBasicos = new ArrayList<>();
            String[] permisosNecesarios = {
                "CREAR_TICKET",
                "VER_TICKET",
                "AGREGAR_NOTA_TICKET"
            };
            
            for (String nombrePermiso : permisosNecesarios) {
                try {
                    Permiso permiso = entityManager.createQuery(
                        "SELECT p FROM Permiso p WHERE p.nombre = :nombre", Permiso.class)
                        .setParameter("nombre", nombrePermiso)
                        .getSingleResult();
                    permisosBasicos.add(permiso);
                } catch (NoResultException nre) {
                    Permiso nuevoPermiso = new Permiso(nombrePermiso, "Permiso básico para " + nombrePermiso.toLowerCase());
                    entityManager.persist(nuevoPermiso);
                    permisosBasicos.add(nuevoPermiso);
                }
            }
            
            // Crear el rol con los permisos básicos
            Rol rolUsuario = new Rol("USUARIO", "Rol básico para usuarios del sistema");
            for (Permiso permiso : permisosBasicos) {
                rolUsuario.agregarPermiso(permiso);
            }
            
            entityManager.persist(rolUsuario);
            entityManager.flush();
            return rolUsuario;
        }
    }

    private void limpiarCamposUsuario() {
        // Buscar los campos por su posición en el GridPane
        GridPane formGrid = (GridPane) ((VBox) tabPane.getSelectionModel().getSelectedItem().getContent()).getChildren().get(0);
        
        // Limpiar cada campo
        ((TextField) formGrid.getChildren().get(1)).clear(); // nombreField
        ((TextField) formGrid.getChildren().get(3)).clear(); // apellidoField
        ((TextField) formGrid.getChildren().get(5)).clear(); // emailField
        ((TextField) formGrid.getChildren().get(7)).clear(); // nombreUsuarioField
    }

    private void crearDepartamento(String nombre, String descripcion) {
        if (nombre == null || nombre.trim().isEmpty()) {
            mostrarError("Error de validación", "El nombre del departamento es obligatorio");
            return;
        }

        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            // Verificar si el departamento ya existe
            try {
                entityManager.createQuery(
                    "SELECT d FROM Departamento d WHERE d.nombre = :nombre", Departamento.class)
                    .setParameter("nombre", nombre)
                    .getSingleResult();
                throw new IllegalArgumentException("El departamento ya existe");
            } catch (NoResultException e) {
                // El departamento no existe, continuar con la creación
            }
            
            Departamento departamento = new Departamento(nombre, descripcion);
            entityManager.persist(departamento);
            
            transaction.commit();
            mostrarMensaje("Departamento creado exitosamente");
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear departamento", e.getMessage());
        }
    }

    private void mostrarMensaje(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAcercaDe() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("Sistema de Tickets de Servicio");
        alert.setContentText("Versión 1.0\nDesarrollado con JavaFX y PostgreSQL");
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        } finally {
            ConexionDB.cerrarEntityManagerFactory();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 