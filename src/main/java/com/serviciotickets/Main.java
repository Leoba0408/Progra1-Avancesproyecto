package com.serviciotickets;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.serviciotickets.modelo.*;
import com.serviciotickets.persistencia.ConexionDB;
import com.serviciotickets.servicio.SesionServicio;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ButtonBar;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;
import javafx.scene.control.DatePicker;

public class Main extends Application {

    private VBox root;
    private TabPane tabPane;
    private EntityManager entityManager;
    private TableView<Ticket> ticketsTable;
    private TableView<Usuario> usuariosTable;
    private TableView<Departamento> departamentosTable;
    private TableView<Tecnico> tecnicosTable;
    private TableView<Administrador> administradoresTable;

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

        // Primero mostrar pantalla de login
        mostrarPantallaLogin(primaryStage);
    }
    
    private void mostrarPantallaLogin(Stage primaryStage) {
        // Crear un nuevo VBox para la pantalla de login
        VBox loginBox = new VBox(10);
        loginBox.setPadding(new Insets(20));
        loginBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label titleLabel = new Label("Sistema de Tickets de Servicio");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        Label usuarioLabel = new Label("Usuario:");
        TextField usuarioField = new TextField();
        
        Label passwordLabel = new Label("Contraseña:");
        PasswordField passwordField = new PasswordField();
        
        grid.add(usuarioLabel, 0, 0);
        grid.add(usuarioField, 1, 0);
        grid.add(passwordLabel, 0, 1);
        grid.add(passwordField, 1, 1);
        
        Button loginButton = new Button("Iniciar Sesión");
        
        loginButton.setOnAction(e -> {
            String nombreUsuario = usuarioField.getText().trim();
            String password = passwordField.getText().trim();
            
            if (nombreUsuario.isEmpty() || password.isEmpty()) {
                mostrarError("Error de login", "Usuario y contraseña son obligatorios");
                return;
            }
            
            try {
                Usuario usuario = validarCredenciales(nombreUsuario, password);
                if (usuario != null) {
                    // Guardar usuario en sesión
                    SesionServicio.getInstancia().setUsuarioActual(usuario);
                    // Mostrar interfaz principal
                    mostrarInterfazPrincipal(primaryStage);
                } else {
                    mostrarError("Error de login", "Credenciales incorrectas");
                }
            } catch (Exception ex) {
                mostrarError("Error de login", ex.getMessage());
            }
        });
        
        loginBox.getChildren().addAll(titleLabel, grid, loginButton);
        
        Scene scene = new Scene(loginBox, 400, 300);
        primaryStage.setTitle("Login - Sistema de Tickets");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private Usuario validarCredenciales(String nombreUsuario, String password) {
        // Credenciales por defecto para acceso de administrador
        if ("admin".equals(nombreUsuario) && "admin".equals(password)) {
            try {
                // Verificar si ya existe un administrador por defecto
                try {
                    Usuario adminExistente = entityManager.createQuery(
                        "SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombre", Usuario.class)
                        .setParameter("nombre", "admin")
                        .getSingleResult();
                    return adminExistente;
                } catch (javax.persistence.NoResultException e) {
                    // No existe, crearlo
                    EntityTransaction transaction = null;
                    try {
                        transaction = entityManager.getTransaction();
                        transaction.begin();
                        
                        // Crear el administrador
                        Administrador admin = new Administrador();
                        admin.setNombre("Administrador");
                        admin.setApellido("Sistema");
                        admin.setEmail("admin@sistema.com");
                        admin.setTelefono("0000000000");
                        admin.setNombreUsuario("admin");
                        admin.setPassword("admin");
                        admin.setCargo("Administrador del Sistema");
                        
                        // Buscar rol administrador o crearlo
                        Rol rolAdmin;
                        try {
                            rolAdmin = entityManager.createQuery(
                                "SELECT r FROM Rol r WHERE r.nombre = 'ADMINISTRADOR'", Rol.class)
                                .getSingleResult();
                        } catch (javax.persistence.NoResultException ex) {
                            rolAdmin = new Rol();
                            rolAdmin.setNombre("ADMINISTRADOR");
                            rolAdmin.setDescripcion("Administrador del sistema");
                            entityManager.persist(rolAdmin);
                        }
                        
                        admin.setRol(rolAdmin);
                        admin.setFechaCreacion(LocalDateTime.now());
                        admin.setUltimaModificacion(LocalDateTime.now());
                        admin.setActivo(true);
                        
                        entityManager.persist(admin);
                        transaction.commit();
                        
                        return admin;
                    } catch (Exception ex) {
                        if (transaction != null && transaction.isActive()) {
                            transaction.rollback();
                        }
                        ex.printStackTrace();
                        
                        // Crear un objeto temporal para la sesión sin persistirlo en la BD
                        Administrador adminTemp = new Administrador();
                        adminTemp.setNombre("Administrador");
                        adminTemp.setApellido("Sistema");
                        adminTemp.setEmail("admin@sistema.com");
                        adminTemp.setNombreUsuario("admin");
                        
                        Rol rolTemp = new Rol();
                        rolTemp.setNombre("ADMINISTRADOR");
                        adminTemp.setRol(rolTemp);
                        
                        return adminTemp;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Crear un objeto temporal como último recurso
                Administrador adminTemp = new Administrador();
                adminTemp.setNombre("Administrador");
                adminTemp.setApellido("Sistema");
                adminTemp.setEmail("admin@sistema.com");
                adminTemp.setNombreUsuario("admin");
                
                Rol rolTemp = new Rol();
                rolTemp.setNombre("ADMINISTRADOR");
                adminTemp.setRol(rolTemp);
                
                return adminTemp;
            }
        }
        
        // Validación normal contra la base de datos
        try {
            return entityManager.createQuery(
                "SELECT u FROM Usuario u WHERE u.nombreUsuario = :nombre AND u.password = :password", 
                Usuario.class)
                .setParameter("nombre", nombreUsuario)
                .setParameter("password", password)
                .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    private void mostrarInterfazPrincipal(Stage primaryStage) {
        // Crear menú
        MenuBar menuBar = crearMenuBar();
        root.getChildren().add(menuBar);

        // Crear pestañas según el rol del usuario
        tabPane = new TabPane();
        
        // Todos los usuarios pueden ver la pestaña de tickets
        tabPane.getTabs().add(crearTabTickets());
        
        // Pestaña "Mis Tickets" para todos los usuarios
        tabPane.getTabs().add(crearTabMisTickets());
        
        // Si es técnico, agregar pestaña para ver tickets de su departamento
        if (SesionServicio.getInstancia().tieneRolTecnico()) {
            tabPane.getTabs().add(crearTabTicketsDepartamento());
        }
        
        // Solo técnicos y administradores pueden ver la pestaña de usuarios
        if (SesionServicio.getInstancia().tieneRolTecnico()) {
            tabPane.getTabs().add(crearTabUsuarios());
        }
        
        // Solo administradores pueden ver la pestaña de departamentos y roles
        if (SesionServicio.getInstancia().tieneRolAdministrador()) {
            tabPane.getTabs().add(crearTabDepartamentos());
            tabPane.getTabs().add(crearTabRolesPermisos());
        }
        
        root.getChildren().add(tabPane);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Sistema de Tickets de Servicio");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar crearMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu archivoMenu = new Menu("Archivo");
        
        MenuItem cerrarSesionItem = new MenuItem("Cerrar Sesión");
        cerrarSesionItem.setOnAction(e -> {
            SesionServicio.getInstancia().cerrarSesion();
            // Obtener el Stage actual
            Stage stage = (Stage) root.getScene().getWindow();
            
            // Reiniciar la aplicación (similar a start pero sin crear un nuevo EntityManager)
            try {
                // Crear un nuevo root para la nueva escena
                root = new VBox(10);
                root.setPadding(new Insets(10));
                
                // Mostrar pantalla de login
                mostrarPantallaLogin(stage);
            } catch (Exception ex) {
                mostrarError("Error al cerrar sesión", ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        MenuItem salirItem = new MenuItem("Salir");
        salirItem.setOnAction(e -> System.exit(0));
        
        archivoMenu.getItems().addAll(cerrarSesionItem, new SeparatorMenuItem(), salirItem);

        Menu ayudaMenu = new Menu("Ayuda");
        MenuItem acercaDeItem = new MenuItem("Acerca de");
        acercaDeItem.setOnAction(e -> mostrarAcercaDe());
        ayudaMenu.getItems().add(acercaDeItem);

        menuBar.getMenus().addAll(archivoMenu, ayudaMenu);
        
        // Mostrar el nombre del usuario actual
        Label usuarioLabel = new Label();
        usuarioLabel.textProperty().bind(new javafx.beans.binding.StringBinding() {
            {
                // Recompute on every scene pulse to keep current
                javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        invalidate();
                    }
                };
                timer.start();
            }
            
            @Override
            protected String computeValue() {
                Usuario usuario = SesionServicio.getInstancia().getUsuarioActual();
                if (usuario == null) return "No conectado";
                
                String rol = "Usuario";
                if (usuario instanceof Administrador) rol = "Administrador";
                else if (usuario instanceof Tecnico) rol = "Técnico";
                else if (SesionServicio.getInstancia().tieneRolAdministrador()) rol = "Administrador";
                else if (SesionServicio.getInstancia().tieneRolTecnico()) rol = "Técnico";
                
                return "Usuario: " + usuario.getNombreUsuario() + " (" + rol + ")";
            }
        });
        
        usuarioLabel.setPadding(new Insets(5, 10, 0, 0));
        
        Menu usuarioMenu = new Menu("");
        usuarioMenu.setGraphic(usuarioLabel);
        menuBar.getMenus().add(usuarioMenu);
        
        return menuBar;
    }

    private Tab crearTabTickets() {
        Tab tab = new Tab("Tickets");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Formulario de nuevo ticket - solo visible para usuarios autenticados
        if (SesionServicio.getInstancia().tieneRolUsuario()) {
            GridPane formGrid = new GridPane();
            formGrid.setHgap(10);
            formGrid.setVgap(10);

            // Título
            formGrid.add(new Label("Título:"), 0, 0);
            TextField tituloField = new TextField();
            formGrid.add(tituloField, 1, 0);

            // Descripción
            formGrid.add(new Label("Descripción:"), 0, 1);
            TextArea descripcionArea = new TextArea();
            descripcionArea.setPrefRowCount(3);
            formGrid.add(descripcionArea, 1, 1);

            // Solicitante (ComboBox para seleccionar usuario)
            formGrid.add(new Label("Solicitante:"), 0, 2);
            ComboBox<Usuario> solicitanteCombo = new ComboBox<>();
            cargarUsuarios(solicitanteCombo);
            
            // Preseleccionar al usuario actual
            Usuario usuarioActual = SesionServicio.getInstancia().getUsuarioActual();
            if (usuarioActual != null) {
                solicitanteCombo.setValue(usuarioActual);
                solicitanteCombo.setDisable(true); // Solo puede crear tickets para sí mismo
            }
            
            formGrid.add(solicitanteCombo, 1, 2);

            // Departamento
            formGrid.add(new Label("Departamento:"), 0, 3);
            ComboBox<Departamento> departamentoCombo = new ComboBox<>();
            cargarDepartamentos(departamentoCombo);
            formGrid.add(departamentoCombo, 1, 3);

            // Prioridad
            formGrid.add(new Label("Prioridad:"), 0, 4);
            ComboBox<Ticket.Prioridad> prioridadCombo = new ComboBox<>();
            prioridadCombo.getItems().addAll(Ticket.Prioridad.values());
            prioridadCombo.setValue(Ticket.Prioridad.MEDIA); // Valor por defecto
            formGrid.add(prioridadCombo, 1, 4);

            // Botón para crear el ticket
            Button crearButton = new Button("Crear Ticket");
            crearButton.setOnAction(e -> {
                if (solicitanteCombo.getValue() == null || departamentoCombo.getValue() == null) {
                    mostrarError("Datos incompletos", "Debe seleccionar un solicitante y un departamento");
                    return;
                }
                
                crearTicket(
                    tituloField.getText(),
                    descripcionArea.getText(),
                    prioridadCombo.getValue(),
                    solicitanteCombo.getValue(),
                    departamentoCombo.getValue()
                );
                
                // Limpiar los campos después de crear el ticket
                tituloField.clear();
                descripcionArea.clear();
                prioridadCombo.setValue(Ticket.Prioridad.MEDIA);
                
                // Recargar la tabla de tickets
                cargarTickets(ticketsTable);
            });

            content.getChildren().addAll(formGrid, crearButton);
        }

        // Tabla de tickets con columnas mejoradas - visible para todos
        ticketsTable = new TableView<>();
        
        TableColumn<Ticket, String> tituloCol = new TableColumn<>("Título");
        TableColumn<Ticket, String> estadoCol = new TableColumn<>("Estado");
        TableColumn<Ticket, String> prioridadCol = new TableColumn<>("Prioridad");
        TableColumn<Ticket, String> solicitanteCol = new TableColumn<>("Solicitante");
        TableColumn<Ticket, String> departamentoCol = new TableColumn<>("Departamento");
        TableColumn<Ticket, String> fechaCreacionCol = new TableColumn<>("Fecha Creación");
        
        ticketsTable.getColumns().addAll(Arrays.asList(
            tituloCol, estadoCol, prioridadCol, solicitanteCol, 
            departamentoCol, fechaCreacionCol
        ));

        // Configurar celdas para mostrar los valores correctos
        tituloCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitulo()));
        estadoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado().toString()));
        prioridadCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPrioridad().toString()));
        solicitanteCol.setCellValueFactory(cellData -> {
            Persona solicitante = cellData.getValue().getSolicitante();
            if (solicitante == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            try {
                return new javafx.beans.property.SimpleStringProperty(
                    solicitante.getNombre() + " " + solicitante.getApellido());
            } catch (Exception e) {
                // Si hay un error al cargar el solicitante, mostrar un valor alternativo
                return new javafx.beans.property.SimpleStringProperty("[Usuario no disponible]");
            }
        });
        departamentoCol.setCellValueFactory(cellData -> {
            Departamento departamento = cellData.getValue().getDepartamento();
            return new javafx.beans.property.SimpleStringProperty(
                departamento != null ? departamento.getNombre() : "");
        });
        fechaCreacionCol.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getFechaCreacion();
            return new javafx.beans.property.SimpleStringProperty(
                fecha != null ? fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });

        // Menú contextual para tickets - solo para técnicos y administradores
        if (SesionServicio.getInstancia().tieneRolTecnico()) {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem verDetallesItem = new MenuItem("Ver Detalles");
            MenuItem cambiarEstadoItem = new MenuItem("Cambiar Estado");
            MenuItem asignarTecnicoItem = new MenuItem("Asignar Técnico");
            
            verDetallesItem.setOnAction(e -> mostrarDetallesTicket(ticketsTable.getSelectionModel().getSelectedItem()));
            cambiarEstadoItem.setOnAction(e -> cambiarEstadoTicket(ticketsTable.getSelectionModel().getSelectedItem()));
            asignarTecnicoItem.setOnAction(e -> asignarTecnicoTicket(ticketsTable.getSelectionModel().getSelectedItem()));
            
            contextMenu.getItems().addAll(verDetallesItem, cambiarEstadoItem, asignarTecnicoItem);
            
            // Añadir opción de eliminar ticket solo para administradores
            if (SesionServicio.getInstancia().tieneRolAdministrador()) {
                MenuItem eliminarTicketItem = new MenuItem("Eliminar Ticket");
                eliminarTicketItem.setOnAction(e -> eliminarTicket(ticketsTable.getSelectionModel().getSelectedItem()));
                contextMenu.getItems().add(eliminarTicketItem);
            }
            
            ticketsTable.setContextMenu(contextMenu);
        }

        // Permitir selección de filas
        ticketsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

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

        // Pestañas para los diferentes tipos de usuarios
        TabPane tiposUsuarioTabs = new TabPane();
        
        // Pestaña para Usuarios regulares
        Tab usuariosTab = new Tab("Usuarios");
        usuariosTab.setClosable(false);
        usuariosTab.setContent(crearContenidoUsuariosRegulares());
        
        // Pestaña para Técnicos
        Tab tecnicosTab = new Tab("Técnicos");
        tecnicosTab.setClosable(false);
        tecnicosTab.setContent(crearContenidoTecnicos());
        
        // Pestaña para Administradores - solo visible para administradores
        Tab administradoresTab = new Tab("Administradores");
        administradoresTab.setClosable(false);
        administradoresTab.setContent(crearContenidoAdministradores());
        
        tiposUsuarioTabs.getTabs().addAll(usuariosTab, tecnicosTab);
        
        // Solo añadir la pestaña de administradores si el usuario es administrador
        if (SesionServicio.getInstancia().tieneRolAdministrador()) {
            tiposUsuarioTabs.getTabs().add(administradoresTab);
        }
        
        content.getChildren().add(tiposUsuarioTabs);

        tab.setContent(content);
        return tab;
    }
    
    private VBox crearContenidoUsuariosRegulares() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Formulario para crear usuario
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        
        // Campos básicos
        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);
        
        formGrid.add(new Label("Apellido:"), 0, 1);
        TextField apellidoField = new TextField();
        formGrid.add(apellidoField, 1, 1);
        
        formGrid.add(new Label("Email:"), 0, 2);
        TextField emailField = new TextField();
        formGrid.add(emailField, 1, 2);
        
        formGrid.add(new Label("Teléfono:"), 0, 3);
        TextField telefonoField = new TextField();
        formGrid.add(telefonoField, 1, 3);
        
        formGrid.add(new Label("Nombre de Usuario:"), 0, 4);
        TextField nombreUsuarioField = new TextField();
        formGrid.add(nombreUsuarioField, 1, 4);
        
        formGrid.add(new Label("Contraseña:"), 0, 5);
        PasswordField passwordField = new PasswordField();
        formGrid.add(passwordField, 1, 5);
        
        // Rol
        formGrid.add(new Label("Rol:"), 0, 6);
        ComboBox<Rol> rolComboBox = new ComboBox<>();
        cargarRoles(rolComboBox);
        formGrid.add(rolComboBox, 1, 6);
        
        // Botón para crear usuario
        Button crearButton = new Button("Crear Usuario");
        crearButton.setOnAction(e -> {
            crearUsuarioDesdeForm(
                nombreField.getText(),
                apellidoField.getText(),
                emailField.getText(),
                telefonoField.getText(),
                nombreUsuarioField.getText(),
                passwordField.getText(),
                rolComboBox.getValue(),
                null  // No hay departamento para usuarios regulares
            );
            
            // Limpiar campos
            nombreField.clear();
            apellidoField.clear();
            emailField.clear();
            telefonoField.clear();
            nombreUsuarioField.clear();
            passwordField.clear();
            
            // Recargar tabla
            cargarUsuariosRegulares(usuariosTable);
        });
        
        content.getChildren().addAll(formGrid, crearButton);
        
        // Tabla de usuarios
        usuariosTable = new TableView<>();
        
        TableColumn<Usuario, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Usuario, String> apellidoCol = new TableColumn<>("Apellido");
        TableColumn<Usuario, String> emailCol = new TableColumn<>("Email");
        TableColumn<Usuario, String> nombreUsuarioCol = new TableColumn<>("Usuario");
        TableColumn<Usuario, String> rolCol = new TableColumn<>("Rol");
        TableColumn<Usuario, Boolean> activoCol = new TableColumn<>("Activo");
        
        nombreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        apellidoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApellido()));
        emailCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        nombreUsuarioCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombreUsuario()));
        rolCol.setCellValueFactory(cellData -> {
            Rol rol = cellData.getValue().getRol();
            return new javafx.beans.property.SimpleStringProperty(rol != null ? rol.getNombre() : "");
        });
        activoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isActivo()).asObject());
        
        // Personalizar celda para mostrar activo como checkbox
        activoCol.setCellFactory(col -> new TableCell<Usuario, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(item);
                    checkBox.setDisable(true); // Solo lectura
                    setGraphic(checkBox);
                }
            }
        });
        
        usuariosTable.getColumns().addAll(Arrays.asList(nombreCol, apellidoCol, emailCol, nombreUsuarioCol, rolCol, activoCol));
        
        // Menú contextual
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editarItem = new MenuItem("Editar");
        MenuItem activarDesactivarItem = new MenuItem("Activar/Desactivar");
        MenuItem eliminarItem = new MenuItem("Eliminar");
        MenuItem cambiarClaveItem = new MenuItem("Cambiar Contraseña");
        
        editarItem.setOnAction(e -> editarUsuario(usuariosTable.getSelectionModel().getSelectedItem()));
        activarDesactivarItem.setOnAction(e -> activarDesactivarUsuario(usuariosTable.getSelectionModel().getSelectedItem()));
        eliminarItem.setOnAction(e -> eliminarUsuario(usuariosTable.getSelectionModel().getSelectedItem()));
        cambiarClaveItem.setOnAction(e -> cambiarContrasenaUsuario(usuariosTable.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().addAll(editarItem, activarDesactivarItem, cambiarClaveItem, eliminarItem);
        usuariosTable.setContextMenu(contextMenu);
        
        usuariosTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        content.getChildren().add(usuariosTable);
        
        cargarUsuariosRegulares(usuariosTable);
        
        return content;
    }
    
    private void cargarUsuariosRegulares(TableView<Usuario> tabla) {
        tabla.getItems().clear();
        try {
            List<Usuario> todos = entityManager.createQuery(
                "SELECT u FROM Usuario u", Usuario.class)
                .getResultList();
            
            List<Usuario> usuariosRegulares = new ArrayList<>();
            for (Usuario u : todos) {
                if (!(u instanceof Tecnico) && !(u instanceof Administrador)) {
                    usuariosRegulares.add(u);
                }
            }
            
            tabla.getItems().addAll(usuariosRegulares);
        } catch (Exception e) {
            mostrarError("Error al cargar usuarios", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cargarRoles(ComboBox<Rol> comboBox) {
        comboBox.getItems().clear();
        List<Rol> roles = entityManager.createQuery("SELECT r FROM Rol r", Rol.class).getResultList();
        
        // Filtrar el rol ADMINISTRADOR si el usuario actual es un técnico
        if (!SesionServicio.getInstancia().tieneRolAdministrador()) {
            roles.removeIf(rol -> "ADMINISTRADOR".equals(rol.getNombre()));
        }
        
        comboBox.getItems().addAll(roles);
        
        // Configurar cómo se muestra el rol
        comboBox.setCellFactory(param -> new ListCell<Rol>() {
            @Override
            protected void updateItem(Rol item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre());
                }
            }
        });
        
        // También para el valor seleccionado
        comboBox.setButtonCell(new ListCell<Rol>() {
            @Override
            protected void updateItem(Rol item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre());
                }
            }
        });
        
        // Seleccionar rol por defecto (USUARIO)
        for (Rol rol : roles) {
            if ("USUARIO".equals(rol.getNombre())) {
                comboBox.setValue(rol);
                break;
            }
        }
    }
    
    private VBox crearContenidoTecnicos() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Formulario para crear técnico
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        
        // Campos básicos
        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);
        
        formGrid.add(new Label("Apellido:"), 0, 1);
        TextField apellidoField = new TextField();
        formGrid.add(apellidoField, 1, 1);
        
        formGrid.add(new Label("Email:"), 0, 2);
        TextField emailField = new TextField();
        formGrid.add(emailField, 1, 2);
        
        formGrid.add(new Label("Teléfono:"), 0, 3);
        TextField telefonoField = new TextField();
        formGrid.add(telefonoField, 1, 3);
        
        formGrid.add(new Label("Nombre de Usuario:"), 0, 4);
        TextField nombreUsuarioField = new TextField();
        formGrid.add(nombreUsuarioField, 1, 4);
        
        formGrid.add(new Label("Contraseña:"), 0, 5);
        PasswordField passwordField = new PasswordField();
        formGrid.add(passwordField, 1, 5);
        
        // Departamento
        formGrid.add(new Label("Departamento:"), 0, 6);
        ComboBox<Departamento> departamentoComboBox = new ComboBox<>();
        cargarDepartamentos(departamentoComboBox);
        formGrid.add(departamentoComboBox, 1, 6);
        
        // Especialidad
        formGrid.add(new Label("Especialidad:"), 0, 7);
        TextField especialidadField = new TextField();
        formGrid.add(especialidadField, 1, 7);
        
        // Rol (siempre TECNICO)
        Rol rolTecnico = obtenerRolTecnico();
        
        // Botón para crear técnico
        Button crearButton = new Button("Crear Técnico");
        crearButton.setOnAction(e -> {
            Tecnico tecnico = crearTecnicoDesdeForm(
                nombreField.getText(),
                apellidoField.getText(),
                emailField.getText(),
                telefonoField.getText(),
                nombreUsuarioField.getText(),
                passwordField.getText(),
                departamentoComboBox.getValue(),
                especialidadField.getText(),
                rolTecnico
            );
            
            if (tecnico != null) {
                // Limpiar campos
                nombreField.clear();
                apellidoField.clear();
                emailField.clear();
                telefonoField.clear();
                nombreUsuarioField.clear();
                passwordField.clear();
                especialidadField.clear();
                
                // Recargar tabla
                cargarTecnicos(tecnicosTable);
            }
        });
        
        content.getChildren().addAll(formGrid, crearButton);
        
        // Tabla de técnicos
        tecnicosTable = new TableView<>();
        
        TableColumn<Tecnico, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Tecnico, String> apellidoCol = new TableColumn<>("Apellido");
        TableColumn<Tecnico, String> emailCol = new TableColumn<>("Email");
        TableColumn<Tecnico, String> departamentoCol = new TableColumn<>("Departamento");
        TableColumn<Tecnico, String> especialidadCol = new TableColumn<>("Especialidad");
        TableColumn<Tecnico, Boolean> activoCol = new TableColumn<>("Activo");
        
        nombreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        apellidoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApellido()));
        emailCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        departamentoCol.setCellValueFactory(cellData -> {
            Departamento departamento = cellData.getValue().getDepartamento();
            return new javafx.beans.property.SimpleStringProperty(departamento != null ? departamento.getNombre() : "");
        });
        especialidadCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEspecialidad()));
        activoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isActivo()).asObject());
        
        // Personalizar celda para mostrar activo como checkbox
        activoCol.setCellFactory(col -> new TableCell<Tecnico, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(item);
                    checkBox.setDisable(true); // Solo lectura
                    setGraphic(checkBox);
                }
            }
        });
        
        tecnicosTable.getColumns().addAll(Arrays.asList(nombreCol, apellidoCol, emailCol, departamentoCol, especialidadCol, activoCol));
        
        // Menú contextual
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editarItem = new MenuItem("Editar");
        MenuItem activarDesactivarItem = new MenuItem("Activar/Desactivar");
        MenuItem eliminarItem = new MenuItem("Eliminar");
        MenuItem cambiarClaveItem = new MenuItem("Cambiar Contraseña");
        
        editarItem.setOnAction(e -> editarTecnico(tecnicosTable.getSelectionModel().getSelectedItem()));
        activarDesactivarItem.setOnAction(e -> activarDesactivarTecnico(tecnicosTable.getSelectionModel().getSelectedItem()));
        eliminarItem.setOnAction(e -> eliminarTecnico(tecnicosTable.getSelectionModel().getSelectedItem()));
        cambiarClaveItem.setOnAction(e -> cambiarContrasenaTecnico(tecnicosTable.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().addAll(editarItem, activarDesactivarItem, cambiarClaveItem, eliminarItem);
        tecnicosTable.setContextMenu(contextMenu);
        
        tecnicosTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        content.getChildren().add(tecnicosTable);
        
        cargarTecnicos(tecnicosTable);
        
        return content;
    }
    
    private void cargarTecnicos(TableView<Tecnico> tabla) {
        tabla.getItems().clear();
        List<Tecnico> tecnicos = entityManager.createQuery(
            "SELECT t FROM Tecnico t", Tecnico.class)
            .getResultList();
        tabla.getItems().addAll(tecnicos);
    }
    
    private Rol obtenerRolTecnico() {
        try {
            return entityManager.createQuery(
                "SELECT r FROM Rol r WHERE r.nombre = 'TECNICO'", Rol.class)
                .getSingleResult();
        } catch (NoResultException e) {
            // Crear rol técnico si no existe
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                Rol rolTecnico = new Rol();
                rolTecnico.setNombre("TECNICO");
                
                // Agregar permisos básicos
                List<Permiso> permisos = new ArrayList<>();
                String[] permisosNecesarios = {
                    "VER_TICKET", "EDITAR_TICKET", "ASIGNAR_TICKET", "CERRAR_TICKET"
                };
                
                for (String nombrePermiso : permisosNecesarios) {
                    try {
                        Permiso permiso = entityManager.createQuery(
                            "SELECT p FROM Permiso p WHERE p.nombre = :nombre", Permiso.class)
                            .setParameter("nombre", nombrePermiso)
                            .getSingleResult();
                        permisos.add(permiso);
                    } catch (NoResultException ex) {
                        Permiso permiso = new Permiso();
                        permiso.setNombre(nombrePermiso);
                        permiso.setDescripcion("Permiso para " + nombrePermiso.toLowerCase().replace('_', ' '));
                        entityManager.persist(permiso);
                        permisos.add(permiso);
                    }
                }
                
                rolTecnico.setPermisos(permisos);
                entityManager.persist(rolTecnico);
                transaction.commit();
                
                return rolTecnico;
            } catch (Exception ex) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al crear rol TECNICO", ex.getMessage());
                return null;
            }
        }
    }
    
    private VBox crearContenidoAdministradores() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Formulario para crear administrador
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        
        // Campos básicos
        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);
        
        formGrid.add(new Label("Apellido:"), 0, 1);
        TextField apellidoField = new TextField();
        formGrid.add(apellidoField, 1, 1);
        
        formGrid.add(new Label("Email:"), 0, 2);
        TextField emailField = new TextField();
        formGrid.add(emailField, 1, 2);
        
        formGrid.add(new Label("Teléfono:"), 0, 3);
        TextField telefonoField = new TextField();
        formGrid.add(telefonoField, 1, 3);
        
        formGrid.add(new Label("Nombre de Usuario:"), 0, 4);
        TextField nombreUsuarioField = new TextField();
        formGrid.add(nombreUsuarioField, 1, 4);
        
        formGrid.add(new Label("Contraseña:"), 0, 5);
        PasswordField passwordField = new PasswordField();
        formGrid.add(passwordField, 1, 5);
        
        // Cargo
        formGrid.add(new Label("Cargo:"), 0, 6);
        TextField cargoField = new TextField();
        formGrid.add(cargoField, 1, 6);
        
        // Rol (siempre ADMINISTRADOR)
        Rol rolAdmin = obtenerRolAdministrador();
        
        // Botón para crear administrador
        Button crearButton = new Button("Crear Administrador");
        crearButton.setOnAction(e -> {
            Administrador admin = crearAdministradorDesdeForm(
                nombreField.getText(),
                apellidoField.getText(),
                emailField.getText(),
                telefonoField.getText(),
                nombreUsuarioField.getText(),
                passwordField.getText(),
                cargoField.getText(),
                rolAdmin
            );
            
            if (admin != null) {
                // Limpiar campos
                nombreField.clear();
                apellidoField.clear();
                emailField.clear();
                telefonoField.clear();
                nombreUsuarioField.clear();
                passwordField.clear();
                cargoField.clear();
                
                // Recargar tabla
                cargarAdministradores(administradoresTable);
            }
        });
        
        content.getChildren().addAll(formGrid, crearButton);
        
        // Tabla de administradores
        administradoresTable = new TableView<>();
        
        TableColumn<Administrador, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Administrador, String> apellidoCol = new TableColumn<>("Apellido");
        TableColumn<Administrador, String> emailCol = new TableColumn<>("Email");
        TableColumn<Administrador, String> cargoCol = new TableColumn<>("Cargo");
        TableColumn<Administrador, Boolean> activoCol = new TableColumn<>("Activo");
        
        nombreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        apellidoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApellido()));
        emailCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        cargoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCargo()));
        activoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isActivo()).asObject());
        
        // Personalizar celda para mostrar activo como checkbox
        activoCol.setCellFactory(col -> new TableCell<Administrador, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(item);
                    checkBox.setDisable(true); // Solo lectura
                    setGraphic(checkBox);
                }
            }
        });
        
        administradoresTable.getColumns().addAll(Arrays.asList(nombreCol, apellidoCol, emailCol, cargoCol, activoCol));
        
        // Menú contextual
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editarItem = new MenuItem("Editar");
        MenuItem activarDesactivarItem = new MenuItem("Activar/Desactivar");
        MenuItem eliminarItem = new MenuItem("Eliminar");
        MenuItem cambiarClaveItem = new MenuItem("Cambiar Contraseña");
        
        editarItem.setOnAction(e -> editarAdministrador(administradoresTable.getSelectionModel().getSelectedItem()));
        activarDesactivarItem.setOnAction(e -> activarDesactivarAdministrador(administradoresTable.getSelectionModel().getSelectedItem()));
        eliminarItem.setOnAction(e -> eliminarAdministrador(administradoresTable.getSelectionModel().getSelectedItem()));
        cambiarClaveItem.setOnAction(e -> cambiarContrasenaAdministrador(administradoresTable.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().addAll(editarItem, activarDesactivarItem, cambiarClaveItem, eliminarItem);
        administradoresTable.setContextMenu(contextMenu);
        
        administradoresTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        content.getChildren().add(administradoresTable);
        
        cargarAdministradores(administradoresTable);
        
        return content;
    }

    private void crearTicket(String titulo, String descripcion, Ticket.Prioridad prioridad, Persona solicitante, Departamento departamento) {
        if (titulo == null || titulo.trim().isEmpty() || descripcion == null || descripcion.trim().isEmpty() || prioridad == null || solicitante == null || departamento == null) {
            mostrarError("Error de validación", "Todos los campos son obligatorios");
            return;
        }

        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            Ticket ticket = new Ticket(titulo, descripcion, prioridad, departamento);
            ticket.setSolicitante(solicitante);
            entityManager.persist(ticket);
            
            transaction.commit();
            mostrarMensaje("Ticket creado exitosamente");
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear ticket", e.getMessage());
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

    private void cargarUsuarios(ComboBox<Usuario> comboBox) {
        comboBox.getItems().clear();
        List<Usuario> usuarios = entityManager.createQuery("SELECT u FROM Usuario u", Usuario.class).getResultList();
        comboBox.getItems().addAll(usuarios);
        
        // Configurar cómo se muestra el usuario en el ComboBox
        comboBox.setCellFactory(param -> new ListCell<Usuario>() {
            @Override
            protected void updateItem(Usuario item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre() + " " + item.getApellido() + " (" + item.getEmail() + ")");
                }
            }
        });
        
        // También configurar cómo se muestra el usuario seleccionado
        comboBox.setButtonCell(new ListCell<Usuario>() {
            @Override
            protected void updateItem(Usuario item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre() + " " + item.getApellido() + " (" + item.getEmail() + ")");
                }
            }
        });
    }
    
    private void cargarDepartamentos(ComboBox<Departamento> comboBox) {
        comboBox.getItems().clear();
        List<Departamento> departamentos = entityManager.createQuery("SELECT d FROM Departamento d", Departamento.class).getResultList();
        comboBox.getItems().addAll(departamentos);
        
        // Configurar cómo se muestra el departamento en el ComboBox
        comboBox.setCellFactory(param -> new ListCell<Departamento>() {
            @Override
            protected void updateItem(Departamento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre());
                }
            }
        });
        
        // También configurar cómo se muestra el departamento seleccionado
        comboBox.setButtonCell(new ListCell<Departamento>() {
            @Override
            protected void updateItem(Departamento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre());
                }
            }
        });
    }
    
    private void mostrarDetallesTicket(Ticket ticket) {
        if (ticket == null) {
            mostrarError("Error", "No hay ticket seleccionado");
            return;
        }
        
        // Crear una ventana de diálogo para mostrar los detalles
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalles del Ticket #" + ticket.getId());
        dialog.setHeaderText("Información completa del ticket");
        
        // Contenido
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Detalles básicos
        grid.add(new Label("ID:"), 0, 0);
        grid.add(new Label(ticket.getId().toString()), 1, 0);
        
        grid.add(new Label("Título:"), 0, 1);
        grid.add(new Label(ticket.getTitulo()), 1, 1);
        
        grid.add(new Label("Descripción:"), 0, 2);
        TextArea descripcionArea = new TextArea(ticket.getDescripcion());
        descripcionArea.setEditable(false);
        descripcionArea.setPrefRowCount(3);
        descripcionArea.setPrefWidth(300);
        grid.add(descripcionArea, 1, 2);
        
        grid.add(new Label("Estado:"), 0, 3);
        grid.add(new Label(ticket.getEstado().toString()), 1, 3);
        
        grid.add(new Label("Prioridad:"), 0, 4);
        grid.add(new Label(ticket.getPrioridad().toString()), 1, 4);
        
        grid.add(new Label("Solicitante:"), 0, 5);
        Persona solicitante = ticket.getSolicitante();
        grid.add(new Label(solicitante != null ? solicitante.getNombre() + " " + solicitante.getApellido() : ""), 1, 5);
        
        grid.add(new Label("Departamento:"), 0, 6);
        Departamento departamento = ticket.getDepartamento();
        grid.add(new Label(departamento != null ? departamento.getNombre() : ""), 1, 6);
        
        grid.add(new Label("Técnico Asignado:"), 0, 7);
        Persona tecnico = ticket.getTecnicoAsignado();
        grid.add(new Label(tecnico != null ? tecnico.getNombre() + " " + tecnico.getApellido() : "No asignado"), 1, 7);
        
        grid.add(new Label("Fecha Creación:"), 0, 8);
        LocalDateTime fechaCreacion = ticket.getFechaCreacion();
        grid.add(new Label(fechaCreacion != null ? 
                fechaCreacion.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""), 1, 8);
        
        grid.add(new Label("Última Modificación:"), 0, 9);
        LocalDateTime ultimaModificacion = ticket.getUltimaModificacion();
        grid.add(new Label(ultimaModificacion != null ? 
                ultimaModificacion.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""), 1, 9);
        
        // Notas del ticket
        grid.add(new Label("Notas:"), 0, 10);
        ListView<NotaTicket> notasListView = new ListView<>();
        
        // Convertir lista a ObservableList
        ObservableList<NotaTicket> notasObservables = FXCollections.observableArrayList();
        if (ticket.getNotas() != null) {
            notasObservables.addAll(ticket.getNotas());
        }
        
        notasListView.setItems(notasObservables);
        notasListView.setCellFactory(param -> new ListCell<NotaTicket>() {
            @Override
            protected void updateItem(NotaTicket item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getContenido() + " - " + 
                           (item.getCreadoPor() != null ? item.getCreadoPor().getNombre() : "Sistema") + 
                           " (" + item.getFechaCreacion().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")");
                }
            }
        });
        
        notasListView.setPrefHeight(150);
        grid.add(notasListView, 1, 10);
        
        // Botones para añadir notas o cambiar estado
        HBox botonesBox = new HBox(10);
        Button agregarNotaBtn = new Button("Agregar Nota");
        Button cambiarEstadoBtn = new Button("Cambiar Estado");
        Button asignarTecnicoBtn = new Button("Asignar Técnico");
        
        agregarNotaBtn.setOnAction(e -> {
            dialog.close();
            agregarNotaTicket(ticket);
        });
        
        cambiarEstadoBtn.setOnAction(e -> {
            dialog.close();
            cambiarEstadoTicket(ticket);
        });
        
        asignarTecnicoBtn.setOnAction(e -> {
            dialog.close();
            asignarTecnicoTicket(ticket);
        });
        
        botonesBox.getChildren().addAll(agregarNotaBtn, cambiarEstadoBtn, asignarTecnicoBtn);
        grid.add(botonesBox, 1, 11);
        
        // Configurar el diálogo
        dialog.getDialogPane().setContent(grid);
        ButtonType cerrarButton = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cerrarButton);
        
        dialog.showAndWait();
    }
    
    private void cambiarEstadoTicket(Ticket ticket) {
        if (ticket == null) {
            mostrarError("Error", "No hay ticket seleccionado");
            return;
        }
        
        // Crear diálogo para cambiar estado
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar Estado del Ticket #" + ticket.getId());
        dialog.setHeaderText("Seleccione el nuevo estado para el ticket");
        
        // Opciones de estado (estas pueden variar según tu modelo de datos)
        ComboBox<String> estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll(
            "ABIERTO", "EN_PROCESO", "EN_ESPERA", "RESUELTO", "CERRADO"
        );
        estadoCombo.setValue(ticket.getEstado().toString());
        
        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        grid.add(new Label("Estado actual:"), 0, 0);
        grid.add(new Label(ticket.getEstado().toString()), 1, 0);
        
        grid.add(new Label("Nuevo estado:"), 0, 1);
        grid.add(estadoCombo, 1, 1);
        
        // Comentario opcional
        grid.add(new Label("Comentario (opcional):"), 0, 2);
        TextArea comentarioArea = new TextArea();
        comentarioArea.setPrefRowCount(3);
        grid.add(comentarioArea, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                return estadoCombo.getValue();
            }
            return null;
        });
        
        // Procesar resultado
        Optional<String> resultado = dialog.showAndWait();
        
        resultado.ifPresent(nuevoEstado -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Cambiar el estado
                ticket.cambiarEstado(nuevoEstado);
                
                // Si hay comentario, agregar una nota
                if (comentarioArea.getText() != null && !comentarioArea.getText().trim().isEmpty()) {
                    NotaTicket nota = new NotaTicket();
                    nota.setContenido("Cambio de estado a " + nuevoEstado + ": " + comentarioArea.getText().trim());
                    nota.setFechaCreacion(LocalDateTime.now());
                    // Aquí puedes establecer el usuario que crea la nota si tienes un sistema de sesión
                    nota.setTicket(ticket);
                    ticket.agregarNota(nota);
                    entityManager.persist(nota);
                }
                
                entityManager.merge(ticket);
                transaction.commit();
                
                mostrarMensaje("Estado cambiado exitosamente");
                cargarTickets(ticketsTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al cambiar estado", e.getMessage());
            }
        });
    }
    
    private void asignarTecnicoTicket(Ticket ticket) {
        if (ticket == null) {
            mostrarError("Error", "No hay ticket seleccionado");
            return;
        }
        
        // Obtener lista de técnicos (puedes filtrar por departamento si quieres)
        List<Tecnico> tecnicos = entityManager.createQuery("SELECT t FROM Tecnico t", Tecnico.class).getResultList();
        
        if (tecnicos.isEmpty()) {
            mostrarError("Error", "No hay técnicos disponibles en el sistema");
            return;
        }
        
        // Crear diálogo para asignar técnico
        Dialog<Tecnico> dialog = new Dialog<>();
        dialog.setTitle("Asignar Técnico al Ticket #" + ticket.getId());
        dialog.setHeaderText("Seleccione un técnico para asignar al ticket");
        
        // ComboBox para seleccionar técnico
        ComboBox<Tecnico> tecnicoCombo = new ComboBox<>();
        tecnicoCombo.getItems().addAll(tecnicos);
        
        // Configurar cómo se muestra el técnico en el ComboBox
        tecnicoCombo.setCellFactory(param -> new ListCell<Tecnico>() {
            @Override
            protected void updateItem(Tecnico item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre() + " " + item.getApellido() + 
                           (item.getDepartamento() != null ? " (" + item.getDepartamento().getNombre() + ")" : ""));
                }
            }
        });
        
        // También configurar cómo se muestra el técnico seleccionado
        tecnicoCombo.setButtonCell(new ListCell<Tecnico>() {
            @Override
            protected void updateItem(Tecnico item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre() + " " + item.getApellido() + 
                           (item.getDepartamento() != null ? " (" + item.getDepartamento().getNombre() + ")" : ""));
                }
            }
        });
        
        // Seleccionar el técnico actual si existe
        if (ticket.getTecnicoAsignado() != null && ticket.getTecnicoAsignado() instanceof Tecnico) {
            for (Tecnico tecnico : tecnicos) {
                if (tecnico.getId().equals(ticket.getTecnicoAsignado().getId())) {
                    tecnicoCombo.setValue(tecnico);
                    break;
                }
            }
        }
        
        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        grid.add(new Label("Técnico:"), 0, 0);
        grid.add(tecnicoCombo, 1, 0);
        
        // Comentario opcional
        grid.add(new Label("Comentario (opcional):"), 0, 1);
        TextArea comentarioArea = new TextArea();
        comentarioArea.setPrefRowCount(3);
        grid.add(comentarioArea, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Validar selección
        Node botonGuardar = dialog.getDialogPane().lookupButton(guardarButton);
        botonGuardar.setDisable(true);
        
        tecnicoCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(newValue == null);
        });
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                return tecnicoCombo.getValue();
            }
            return null;
        });
        
        // Procesar resultado
        Optional<Tecnico> resultado = dialog.showAndWait();
        
        resultado.ifPresent(tecnico -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Asignar el técnico
                ticket.setTecnicoAsignado(tecnico);
                
                // También cambiar el estado a "EN_PROCESO" si estaba en "ABIERTO"
                if ("ABIERTO".equals(ticket.getEstado().toString())) {
                    ticket.cambiarEstado("EN_PROCESO");
                }
                
                // Si hay comentario, agregar una nota
                if (comentarioArea.getText() != null && !comentarioArea.getText().trim().isEmpty()) {
                    NotaTicket nota = new NotaTicket();
                    nota.setContenido("Asignación de técnico a " + tecnico.getNombre() + " " + tecnico.getApellido() + 
                                     ": " + comentarioArea.getText().trim());
                    nota.setFechaCreacion(LocalDateTime.now());
                    // Aquí puedes establecer el usuario que crea la nota si tienes un sistema de sesión
                    nota.setTicket(ticket);
                    ticket.agregarNota(nota);
                    entityManager.persist(nota);
                }
                
                entityManager.merge(ticket);
                transaction.commit();
                
                mostrarMensaje("Técnico asignado exitosamente");
                cargarTickets(ticketsTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al asignar técnico", e.getMessage());
            }
        });
    }
    
    private void agregarNotaTicket(Ticket ticket) {
        if (ticket == null) {
            mostrarError("Error", "No hay ticket seleccionado");
            return;
        }
        
        // Crear diálogo para agregar nota
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Agregar Nota al Ticket #" + ticket.getId());
        dialog.setHeaderText("Ingrese el contenido de la nota");
        
        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        grid.add(new Label("Contenido:"), 0, 0);
        TextArea contenidoArea = new TextArea();
        contenidoArea.setPrefRowCount(5);
        contenidoArea.setPrefWidth(300);
        grid.add(contenidoArea, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Validar contenido
        Node botonGuardar = dialog.getDialogPane().lookupButton(guardarButton);
        botonGuardar.setDisable(true);
        
        contenidoArea.textProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(newValue == null || newValue.trim().isEmpty());
        });
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                return contenidoArea.getText();
            }
            return null;
        });
        
        // Procesar resultado
        Optional<String> resultado = dialog.showAndWait();
        
        resultado.ifPresent(contenido -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Crear y agregar la nota
                NotaTicket nota = new NotaTicket();
                nota.setContenido(contenido.trim());
                nota.setFechaCreacion(LocalDateTime.now());
                // Aquí puedes establecer el usuario que crea la nota si tienes un sistema de sesión
                nota.setTicket(ticket);
                ticket.agregarNota(nota);
                
                entityManager.persist(nota);
                entityManager.merge(ticket);
                transaction.commit();
                
                mostrarMensaje("Nota agregada exitosamente");
                cargarTickets(ticketsTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al agregar nota", e.getMessage());
            }
        });
    }

    private Tab crearTabDepartamentos() {
        Tab tab = new Tab("Departamentos");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Formulario para crear departamento
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        // Nombre del departamento
        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);

        // Descripción
        formGrid.add(new Label("Descripción:"), 0, 1);
        TextArea descripcionArea = new TextArea();
        descripcionArea.setPrefRowCount(3);
        formGrid.add(descripcionArea, 1, 1);

        // Botón para crear departamento
        Button crearButton = new Button("Crear Departamento");
        crearButton.setOnAction(e -> {
            crearDepartamento(nombreField.getText(), descripcionArea.getText());
            nombreField.clear();
            descripcionArea.clear();
            cargarDepartamentos(departamentosTable);
        });

        content.getChildren().addAll(formGrid, crearButton);

        // Tabla de departamentos
        departamentosTable = new TableView<>();
        
        TableColumn<Departamento, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Departamento, String> descripcionCol = new TableColumn<>("Descripción");
        
        nombreCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        descripcionCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcion()));
        
        departamentosTable.getColumns().addAll(Arrays.asList(nombreCol, descripcionCol));
        
        // Menú contextual para departamentos
        ContextMenu contextMenu = new ContextMenu();
        MenuItem verColaItem = new MenuItem("Ver Cola de Tickets");
        MenuItem asignarTecnicosItem = new MenuItem("Asignar Técnicos");
        MenuItem editarItem = new MenuItem("Editar");
        MenuItem eliminarItem = new MenuItem("Eliminar");
        
        verColaItem.setOnAction(e -> 
            verColaTicketsDepartamento(departamentosTable.getSelectionModel().getSelectedItem()));
        asignarTecnicosItem.setOnAction(e -> 
            asignarTecnicosDepartamento(departamentosTable.getSelectionModel().getSelectedItem()));
        editarItem.setOnAction(e -> 
            editarDepartamento(departamentosTable.getSelectionModel().getSelectedItem()));
        eliminarItem.setOnAction(e -> 
            eliminarDepartamento(departamentosTable.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().addAll(verColaItem, asignarTecnicosItem, editarItem, eliminarItem);
        departamentosTable.setContextMenu(contextMenu);

        content.getChildren().add(departamentosTable);
        
        // Botones adicionales
        HBox botonesBox = new HBox(10);
        Button verColaBtn = new Button("Ver Cola de Tickets");
        Button asignarTecnicosBtn = new Button("Asignar Técnicos");
        
        verColaBtn.setOnAction(e -> {
            Departamento seleccionado = departamentosTable.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                mostrarError("Error", "Debe seleccionar un departamento");
                return;
            }
            verColaTicketsDepartamento(seleccionado);
        });
        
        asignarTecnicosBtn.setOnAction(e -> {
            Departamento seleccionado = departamentosTable.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                mostrarError("Error", "Debe seleccionar un departamento");
                return;
            }
            asignarTecnicosDepartamento(seleccionado);
        });
        
        botonesBox.getChildren().addAll(verColaBtn, asignarTecnicosBtn);
        content.getChildren().add(botonesBox);
        
        cargarDepartamentos(departamentosTable);
        
        tab.setContent(content);
        return tab;
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
            
            Departamento departamento = new Departamento();
            departamento.setNombre(nombre.trim());
            departamento.setDescripcion(descripcion != null ? descripcion.trim() : "");
            
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
    
    private void cargarDepartamentos(TableView<Departamento> tabla) {
        tabla.getItems().clear();
        List<Departamento> departamentos = entityManager.createQuery(
            "SELECT d FROM Departamento d", Departamento.class).getResultList();
        tabla.getItems().addAll(departamentos);
    }
    
    private void verColaTicketsDepartamento(Departamento departamento) {
        if (departamento == null) {
            mostrarError("Error", "No hay departamento seleccionado");
            return;
        }
        
        // Crear una ventana de diálogo para mostrar la cola de tickets
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Cola de Tickets - " + departamento.getNombre());
        dialog.setHeaderText("Tickets asignados al departamento " + departamento.getNombre());
        
        // Contenido
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Tabla de tickets
        TableView<Ticket> ticketsTable = new TableView<>();
        
        TableColumn<Ticket, Long> idCol = new TableColumn<>("ID");
        TableColumn<Ticket, String> tituloCol = new TableColumn<>("Título");
        TableColumn<Ticket, String> prioridadCol = new TableColumn<>("Prioridad");
        TableColumn<Ticket, String> estadoCol = new TableColumn<>("Estado");
        TableColumn<Ticket, String> solicitanteCol = new TableColumn<>("Solicitante");
        TableColumn<Ticket, String> fechaCreacionCol = new TableColumn<>("Fecha Creación");
        TableColumn<Ticket, String> tecnicoCol = new TableColumn<>("Técnico Asignado");
        
        idCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleLongProperty(cellData.getValue().getId()).asObject());
        tituloCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitulo()));
        prioridadCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPrioridad().toString()));
        estadoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado()));
        solicitanteCol.setCellValueFactory(cellData -> {
            Persona solicitante = cellData.getValue().getSolicitante();
            return new javafx.beans.property.SimpleStringProperty(
                solicitante != null ? solicitante.getNombre() + " " + solicitante.getApellido() : "");
        });
        fechaCreacionCol.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getFechaCreacion();
            return new javafx.beans.property.SimpleStringProperty(
                fecha != null ? fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });
        tecnicoCol.setCellValueFactory(cellData -> {
            Persona tecnico = cellData.getValue().getTecnicoAsignado();
            return new javafx.beans.property.SimpleStringProperty(
                tecnico != null ? tecnico.getNombre() + " " + tecnico.getApellido() : "Sin asignar");
        });
        
        ticketsTable.getColumns().addAll(
            Arrays.asList(idCol, tituloCol, prioridadCol, estadoCol, solicitanteCol, fechaCreacionCol, tecnicoCol)
        );
        
        // Cargar tickets del departamento
        List<Ticket> tickets = entityManager.createQuery(
            "SELECT t FROM Ticket t WHERE t.departamento = :departamento ORDER BY t.prioridad, t.fechaCreacion", 
            Ticket.class)
            .setParameter("departamento", departamento)
            .getResultList();
        
        ticketsTable.getItems().addAll(tickets);
        
        // Filtros
        HBox filtrosBox = new HBox(10);
        filtrosBox.setPadding(new Insets(5));
        
        // Filtro por estado
        ComboBox<String> estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll("TODOS", "NUEVO", "ABIERTO", "EN_PROCESO", "EN_ESPERA", "RESUELTO", "CERRADO");
        estadoCombo.setValue("TODOS");
        estadoCombo.setPromptText("Filtrar por Estado");
        
        // Filtro por prioridad
        ComboBox<String> prioridadCombo = new ComboBox<>();
        prioridadCombo.getItems().addAll("TODAS", "ALTA", "MEDIA", "BAJA");
        prioridadCombo.setValue("TODAS");
        prioridadCombo.setPromptText("Filtrar por Prioridad");
        
        // Botón para aplicar filtros
        Button aplicarFiltrosBtn = new Button("Aplicar Filtros");
        aplicarFiltrosBtn.setOnAction(e -> {
            String estadoFiltro = estadoCombo.getValue();
            String prioridadFiltro = prioridadCombo.getValue();
            
            // Construir consulta con filtros
            StringBuilder queryString = new StringBuilder("SELECT t FROM Ticket t WHERE t.departamento = :departamento");
            
            if (!"TODOS".equals(estadoFiltro)) {
                queryString.append(" AND t.estado = :estado");
            }
            
            if (!"TODAS".equals(prioridadFiltro)) {
                queryString.append(" AND t.prioridad = :prioridad");
            }
            
            queryString.append(" ORDER BY t.prioridad, t.fechaCreacion");
            
            try {
                javax.persistence.TypedQuery<Ticket> query = entityManager.createQuery(queryString.toString(), Ticket.class)
                    .setParameter("departamento", departamento);
                
                if (!"TODOS".equals(estadoFiltro)) {
                    query.setParameter("estado", estadoFiltro);
                }
                
                if (!"TODAS".equals(prioridadFiltro)) {
                    query.setParameter("prioridad", Ticket.Prioridad.valueOf(prioridadFiltro));
                }
                
                List<Ticket> ticketsFiltrados = query.getResultList();
                ticketsTable.getItems().clear();
                ticketsTable.getItems().addAll(ticketsFiltrados);
            } catch (Exception ex) {
                mostrarError("Error al aplicar filtros", ex.getMessage());
            }
        });
        
        // Botón para limpiar filtros
        Button limpiarFiltrosBtn = new Button("Limpiar Filtros");
        limpiarFiltrosBtn.setOnAction(e -> {
            estadoCombo.setValue("TODOS");
            prioridadCombo.setValue("TODAS");
            ticketsTable.getItems().clear();
            ticketsTable.getItems().addAll(tickets);
        });
        
        filtrosBox.getChildren().addAll(
            new Label("Estado:"), estadoCombo,
            new Label("Prioridad:"), prioridadCombo,
            aplicarFiltrosBtn, limpiarFiltrosBtn
        );
        
        // Botones para tomar tickets y ver detalles
        HBox botonesBox = new HBox(10);
        
        Button tomarTicketBtn = new Button("Tomar Ticket");
        Button verDetallesBtn = new Button("Ver Detalles");
        
        // Solo técnicos y administradores pueden tomar tickets
        if (SesionServicio.getInstancia().tieneRolTecnico()) {
            tomarTicketBtn.setOnAction(e -> {
                Ticket ticketSeleccionado = ticketsTable.getSelectionModel().getSelectedItem();
                if (ticketSeleccionado == null) {
                    mostrarError("Error", "Debe seleccionar un ticket para tomar");
                    return;
                }
                
                tomarTicket(ticketSeleccionado, departamento);
                // Recargar tickets
                ticketsTable.getItems().clear();
                List<Ticket> ticketsActualizados = entityManager.createQuery(
                    "SELECT t FROM Ticket t WHERE t.departamento = :departamento ORDER BY t.prioridad, t.fechaCreacion", 
                    Ticket.class)
                    .setParameter("departamento", departamento)
                    .getResultList();
                ticketsTable.getItems().addAll(ticketsActualizados);
            });
            
            botonesBox.getChildren().add(tomarTicketBtn);
        }
        
        verDetallesBtn.setOnAction(e -> {
            Ticket ticketSeleccionado = ticketsTable.getSelectionModel().getSelectedItem();
            if (ticketSeleccionado == null) {
                mostrarError("Error", "Debe seleccionar un ticket para ver detalles");
                return;
            }
            
            dialog.close();
            mostrarDetallesTicket(ticketSeleccionado);
        });
        
        botonesBox.getChildren().add(verDetallesBtn);
        
        content.getChildren().addAll(
            new Label("Tickets en el departamento: " + tickets.size()),
            filtrosBox,
            ticketsTable,
            botonesBox
        );
        
        // Configurar el diálogo
        dialog.getDialogPane().setContent(content);
        ButtonType cerrarButton = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cerrarButton);
        
        // Ajustar tamaño de la ventana
        dialog.getDialogPane().setPrefWidth(800);
        dialog.getDialogPane().setPrefHeight(600);
        
        dialog.showAndWait();
    }
    
    private void tomarTicket(Ticket ticket, Departamento departamento) {
        if (ticket == null) {
            mostrarError("Error", "No hay ticket seleccionado");
            return;
        }
        
        // Verificar si el ticket ya está asignado
        if (ticket.getTecnicoAsignado() != null) {
            mostrarError("No disponible", "El ticket ya ha sido asignado a " + 
                ticket.getTecnicoAsignado().getNombre() + " " + ticket.getTecnicoAsignado().getApellido());
            return;
        }
        
        // Verificar si el usuario actual es un técnico
        Usuario usuarioActual = SesionServicio.getInstancia().getUsuarioActual();
        if (!(usuarioActual instanceof Tecnico)) {
            mostrarError("Error", "Solo los técnicos pueden tomar tickets");
            return;
        }
        
        Tecnico tecnico = (Tecnico) usuarioActual;
        
        // Verificar si el técnico pertenece al departamento
        if (tecnico.getDepartamento() == null || !tecnico.getDepartamento().getId().equals(departamento.getId())) {
            mostrarError("Error", "Solo los técnicos del departamento pueden tomar tickets");
            return;
        }
        
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            // Asignar ticket al técnico
            ticket.setTecnicoAsignado(tecnico);
            ticket.cambiarEstado("EN_PROCESO");
            
            // Agregar nota
            NotaTicket nota = new NotaTicket();
            nota.setContenido("Ticket tomado por el técnico " + tecnico.getNombre() + " " + tecnico.getApellido());
            nota.setFechaCreacion(LocalDateTime.now());
            nota.setCreadoPor(tecnico);
            nota.setTicket(ticket);
            
            ticket.agregarNota(nota);
            
            entityManager.persist(nota);
            entityManager.merge(ticket);
            
            transaction.commit();
            
            mostrarMensaje("Ticket asignado exitosamente");
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al tomar ticket", e.getMessage());
        }
    }

    private void crearUsuarioDesdeForm(String nombre, String apellido, String email, String telefono, String nombreUsuario, String password, Rol rol, Departamento departamento) {
        // Validar datos
        if (nombre == null || nombre.trim().isEmpty() || 
            apellido == null || apellido.trim().isEmpty() ||
            email == null || email.trim().isEmpty() || 
            nombreUsuario == null || nombreUsuario.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            rol == null) {
            mostrarError("Error de validación", "Todos los campos obligatorios deben completarse");
            return;
        }
        
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            // Crear usuario
            Usuario usuario = new Usuario();
            usuario.setNombre(nombre.trim());
            usuario.setApellido(apellido.trim());
            usuario.setEmail(email.trim());
            usuario.setTelefono(telefono != null ? telefono.trim() : "");
            usuario.setNombreUsuario(nombreUsuario.trim());
            usuario.setPassword(password); // En producción, esto debería estar hasheado
            usuario.setRol(rol);
            usuario.setFechaCreacion(LocalDateTime.now());
            usuario.setUltimaModificacion(LocalDateTime.now());
            usuario.setActivo(true);
            
            // Si es para un técnico o admin, establecer departamento
            if (departamento != null) {
                usuario.setDepartamento(departamento);
            }
            
            entityManager.persist(usuario);
            transaction.commit();
            
            mostrarMensaje("Usuario creado exitosamente");
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear usuario", e.getMessage());
        }
    }
    
    private Tecnico crearTecnicoDesdeForm(String nombre, String apellido, String email, String telefono, String nombreUsuario, String password, Departamento departamento, String especialidad, Rol rolTecnico) {
        // Validar datos
        if (nombre == null || nombre.trim().isEmpty() || 
            apellido == null || apellido.trim().isEmpty() ||
            email == null || email.trim().isEmpty() || 
            nombreUsuario == null || nombreUsuario.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            mostrarError("Error de validación", "Todos los campos obligatorios deben completarse");
            return null;
        }
        
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            // Crear técnico
            Tecnico tecnico = new Tecnico();
            tecnico.setNombre(nombre.trim());
            tecnico.setApellido(apellido.trim());
            tecnico.setEmail(email.trim());
            tecnico.setTelefono(telefono != null ? telefono.trim() : "");
            tecnico.setNombreUsuario(nombreUsuario.trim());
            tecnico.setPassword(password); // En producción, esto debería estar hasheado
            tecnico.setRol(rolTecnico);
            tecnico.setFechaCreacion(LocalDateTime.now());
            tecnico.setUltimaModificacion(LocalDateTime.now());
            tecnico.setActivo(true);
            tecnico.setEspecialidad(especialidad != null ? especialidad.trim() : "");
            
            if (departamento != null) {
                tecnico.setDepartamento(departamento);
            }
            
            entityManager.persist(tecnico);
            transaction.commit();
            
            mostrarMensaje("Técnico creado exitosamente");
            return tecnico;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear técnico", e.getMessage());
            return null;
        }
    }
    
    private Administrador crearAdministradorDesdeForm(String nombre, String apellido, String email, String telefono, String nombreUsuario, String password, String cargo, Rol rolAdmin) {
        // Validar datos
        if (nombre == null || nombre.trim().isEmpty() || 
            apellido == null || apellido.trim().isEmpty() ||
            email == null || email.trim().isEmpty() || 
            nombreUsuario == null || nombreUsuario.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            mostrarError("Error de validación", "Todos los campos obligatorios deben completarse");
            return null;
        }
        
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            // Crear administrador
            Administrador admin = new Administrador();
            admin.setNombre(nombre.trim());
            admin.setApellido(apellido.trim());
            admin.setEmail(email.trim());
            admin.setTelefono(telefono != null ? telefono.trim() : "");
            admin.setNombreUsuario(nombreUsuario.trim());
            admin.setPassword(password); // En producción, esto debería estar hasheado
            admin.setRol(rolAdmin);
            admin.setFechaCreacion(LocalDateTime.now());
            admin.setUltimaModificacion(LocalDateTime.now());
            admin.setActivo(true);
            admin.setCargo(cargo != null ? cargo.trim() : "");
            
            entityManager.persist(admin);
            transaction.commit();
            
            mostrarMensaje("Administrador creado exitosamente");
            return admin;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear administrador", e.getMessage());
            return null;
        }
    }
    
    private Rol obtenerRolAdministrador() {
        try {
            return entityManager.createQuery(
                "SELECT r FROM Rol r WHERE r.nombre = 'ADMINISTRADOR'", Rol.class)
                .getSingleResult();
        } catch (NoResultException e) {
            // Crear rol administrador si no existe
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                Rol rolAdmin = new Rol();
                rolAdmin.setNombre("ADMINISTRADOR");
                
                // Agregar todos los permisos
                List<Permiso> permisos = entityManager.createQuery(
                    "SELECT p FROM Permiso p", Permiso.class).getResultList();
                
                rolAdmin.setPermisos(new HashSet<>(permisos));
                entityManager.persist(rolAdmin);
                transaction.commit();
                
                return rolAdmin;
            } catch (Exception ex) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al crear rol ADMINISTRADOR", ex.getMessage());
                return null;
            }
        }
    }
    
    private void editarUsuario(Usuario usuario) {
        if (usuario == null) {
            mostrarError("Error", "No hay usuario seleccionado");
            return;
        }        
        // Crear diálogo para editar usuario
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Editar Usuario");
        dialog.setHeaderText("Editar información del usuario");
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(usuario.getNombre());
        TextField apellidoField = new TextField(usuario.getApellido());
        TextField emailField = new TextField(usuario.getEmail());
        TextField telefonoField = new TextField(usuario.getTelefono());
        TextField nombreUsuarioField = new TextField(usuario.getNombreUsuario());
        
        ComboBox<Rol> rolComboBox = new ComboBox<>();
        cargarRoles(rolComboBox);
        rolComboBox.setValue(usuario.getRol());
        
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Apellido:"), 0, 1);
        grid.add(apellidoField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Teléfono:"), 0, 3);
        grid.add(telefonoField, 1, 3);
        grid.add(new Label("Nombre de Usuario:"), 0, 4);
        grid.add(nombreUsuarioField, 1, 4);
        grid.add(new Label("Rol:"), 0, 5);
        grid.add(rolComboBox, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                // Actualizar datos
                usuario.setNombre(nombreField.getText().trim());
                usuario.setApellido(apellidoField.getText().trim());
                usuario.setEmail(emailField.getText().trim());
                usuario.setTelefono(telefonoField.getText().trim());
                usuario.setNombreUsuario(nombreUsuarioField.getText().trim());
                usuario.setRol(rolComboBox.getValue());
                usuario.setUltimaModificacion(LocalDateTime.now());
                return usuario;
            }
            return null;
        });
        
        // Procesar resultado
        Optional<Usuario> resultado = dialog.showAndWait();
        resultado.ifPresent(usuarioActualizado -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                entityManager.merge(usuarioActualizado);
                transaction.commit();
                mostrarMensaje("Usuario actualizado exitosamente");
                cargarUsuariosRegulares(usuariosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al actualizar usuario", e.getMessage());
            }
        });
    }
    
    private void activarDesactivarUsuario(Usuario usuario) {
        if (usuario == null) {
            mostrarError("Error", "No hay usuario seleccionado");
            return;
        }
        
        boolean nuevoEstado = !usuario.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar operación");
        confirmacion.setHeaderText("¿Está seguro que desea " + accion + " al usuario?");
        confirmacion.setContentText("Usuario: " + usuario.getNombre() + " " + usuario.getApellido());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                usuario.setActivo(nuevoEstado);
                usuario.setUltimaModificacion(LocalDateTime.now());
                
                entityManager.merge(usuario);
                transaction.commit();
                
                mostrarMensaje("Usuario " + accion + "do exitosamente");
                cargarUsuariosRegulares(usuariosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al " + accion + " usuario", e.getMessage());
            }
        }
    }
    
    private void eliminarUsuario(Usuario usuario) {
        if (usuario == null) {
            mostrarError("Error", "No hay usuario seleccionado");
            return;
        }
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar al usuario?");
        confirmacion.setContentText("Esta acción no se puede deshacer.\nUsuario: " + usuario.getNombre() + " " + usuario.getApellido());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Comprobar si tiene tickets asociados
                if (!usuario.getTickets().isEmpty()) {
                    throw new IllegalStateException("No se puede eliminar el usuario porque tiene tickets asociados");
                }
                
                entityManager.remove(
                    entityManager.contains(usuario) ? usuario : entityManager.merge(usuario)
                );
                
                transaction.commit();
                mostrarMensaje("Usuario eliminado exitosamente");
                cargarUsuariosRegulares(usuariosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar usuario", e.getMessage());
            }
        }
    }
    
    private void cambiarContrasenaUsuario(Usuario usuario) {
        if (usuario == null) {
            mostrarError("Error", "No hay usuario seleccionado");
            return;
        }
        
        // Crear diálogo para cambiar contraseña
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar Contraseña");
        dialog.setHeaderText("Cambiar contraseña para " + usuario.getNombre() + " " + usuario.getApellido());
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        PasswordField nuevaPasswordField = new PasswordField();
        PasswordField confirmarPasswordField = new PasswordField();
        
        grid.add(new Label("Nueva contraseña:"), 0, 0);
        grid.add(nuevaPasswordField, 1, 0);
        grid.add(new Label("Confirmar contraseña:"), 0, 1);
        grid.add(confirmarPasswordField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Validación de contraseñas
        Node botonGuardar = dialog.getDialogPane().lookupButton(guardarButton);
        botonGuardar.setDisable(true);
        
        // Habilitar botón sólo si las contraseñas coinciden y no están vacías
        confirmarPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(
                nuevaPasswordField.getText().trim().isEmpty() || 
                !nuevaPasswordField.getText().equals(confirmarPasswordField.getText())
            );
        });
        
        nuevaPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(
                newValue.trim().isEmpty() || 
                !newValue.equals(confirmarPasswordField.getText())
            );
        });
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                return nuevaPasswordField.getText();
            }
            return null;
        });
        
        // Procesar resultado
        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(nuevaPassword -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                usuario.setPassword(nuevaPassword); // En producción, esto debería estar hasheado
                usuario.setUltimaModificacion(LocalDateTime.now());
                
                entityManager.merge(usuario);
                transaction.commit();
                
                mostrarMensaje("Contraseña cambiada exitosamente");
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al cambiar contraseña", e.getMessage());
            }
        });
    }
    
    private void editarTecnico(Tecnico tecnico) {
        if (tecnico == null) {
            mostrarError("Error", "No hay técnico seleccionado");
            return;
        }
        
        // Crear diálogo para editar técnico
        Dialog<Tecnico> dialog = new Dialog<>();
        dialog.setTitle("Editar Técnico");
        dialog.setHeaderText("Editar información del técnico");
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(tecnico.getNombre());
        TextField apellidoField = new TextField(tecnico.getApellido());
        TextField emailField = new TextField(tecnico.getEmail());
        TextField telefonoField = new TextField(tecnico.getTelefono());
        TextField nombreUsuarioField = new TextField(tecnico.getNombreUsuario());
        TextField especialidadField = new TextField(tecnico.getEspecialidad());
        
        ComboBox<Departamento> departamentoComboBox = new ComboBox<>();
        cargarDepartamentos(departamentoComboBox);
        departamentoComboBox.setValue(tecnico.getDepartamento());
        
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Apellido:"), 0, 1);
        grid.add(apellidoField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Teléfono:"), 0, 3);
        grid.add(telefonoField, 1, 3);
        grid.add(new Label("Nombre de Usuario:"), 0, 4);
        grid.add(nombreUsuarioField, 1, 4);
        grid.add(new Label("Especialidad:"), 0, 5);
        grid.add(especialidadField, 1, 5);
        grid.add(new Label("Departamento:"), 0, 6);
        grid.add(departamentoComboBox, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                // Actualizar datos
                tecnico.setNombre(nombreField.getText().trim());
                tecnico.setApellido(apellidoField.getText().trim());
                tecnico.setEmail(emailField.getText().trim());
                tecnico.setTelefono(telefonoField.getText().trim());
                tecnico.setNombreUsuario(nombreUsuarioField.getText().trim());
                tecnico.setEspecialidad(especialidadField.getText().trim());
                tecnico.setDepartamento(departamentoComboBox.getValue());
                tecnico.setUltimaModificacion(LocalDateTime.now());
                return tecnico;
            }
            return null;
        });
        
        // Procesar resultado
        Optional<Tecnico> resultado = dialog.showAndWait();
        resultado.ifPresent(tecnicoActualizado -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                entityManager.merge(tecnicoActualizado);
                transaction.commit();
                mostrarMensaje("Técnico actualizado exitosamente");
                cargarTecnicos(tecnicosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al actualizar técnico", e.getMessage());
            }
        });
    }
    
    private void activarDesactivarTecnico(Tecnico tecnico) {
        if (tecnico == null) {
            mostrarError("Error", "No hay técnico seleccionado");
            return;
        }
        
        boolean nuevoEstado = !tecnico.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar operación");
        confirmacion.setHeaderText("¿Está seguro que desea " + accion + " al técnico?");
        confirmacion.setContentText("Técnico: " + tecnico.getNombre() + " " + tecnico.getApellido());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                tecnico.setActivo(nuevoEstado);
                tecnico.setUltimaModificacion(LocalDateTime.now());
                
                entityManager.merge(tecnico);
                transaction.commit();
                
                mostrarMensaje("Técnico " + accion + "do exitosamente");
                cargarTecnicos(tecnicosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al " + accion + " técnico", e.getMessage());
            }
        }
    }
    
    private void eliminarTecnico(Tecnico tecnico) {
        if (tecnico == null) {
            mostrarError("Error", "No hay técnico seleccionado");
            return;
        }
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar al técnico?");
        confirmacion.setContentText("Esta acción no se puede deshacer.\nTécnico: " + tecnico.getNombre() + " " + tecnico.getApellido());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Comprobar si tiene tickets asignados
                List<Ticket> ticketsAsignados = entityManager.createQuery(
                    "SELECT t FROM Ticket t WHERE t.tecnicoAsignado = :tecnico", Ticket.class)
                    .setParameter("tecnico", tecnico)
                    .getResultList();
                
                if (!ticketsAsignados.isEmpty()) {
                    throw new IllegalStateException("No se puede eliminar el técnico porque tiene tickets asignados");
                }
                
                entityManager.remove(
                    entityManager.contains(tecnico) ? tecnico : entityManager.merge(tecnico)
                );
                
                transaction.commit();
                mostrarMensaje("Técnico eliminado exitosamente");
                cargarTecnicos(tecnicosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar técnico", e.getMessage());
            }
        }
    }
    
    private void cambiarContrasenaTecnico(Tecnico tecnico) {
        if (tecnico == null) {
            mostrarError("Error", "No hay técnico seleccionado");
            return;
        }
        
        // Crear diálogo para cambiar contraseña
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar Contraseña");
        dialog.setHeaderText("Cambiar contraseña para " + tecnico.getNombre() + " " + tecnico.getApellido());
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        PasswordField nuevaPasswordField = new PasswordField();
        PasswordField confirmarPasswordField = new PasswordField();
        
        grid.add(new Label("Nueva contraseña:"), 0, 0);
        grid.add(nuevaPasswordField, 1, 0);
        grid.add(new Label("Confirmar contraseña:"), 0, 1);
        grid.add(confirmarPasswordField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Validación de contraseñas
        Node botonGuardar = dialog.getDialogPane().lookupButton(guardarButton);
        botonGuardar.setDisable(true);
        
        // Habilitar botón sólo si las contraseñas coinciden y no están vacías
        confirmarPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(
                nuevaPasswordField.getText().trim().isEmpty() || 
                !nuevaPasswordField.getText().equals(confirmarPasswordField.getText())
            );
        });
        
        nuevaPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(
                newValue.trim().isEmpty() || 
                !newValue.equals(confirmarPasswordField.getText())
            );
        });
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                return nuevaPasswordField.getText();
            }
            return null;
        });
        
        // Procesar resultado
        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(nuevaPassword -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                tecnico.setPassword(nuevaPassword); // En producción, esto debería estar hasheado
                tecnico.setUltimaModificacion(LocalDateTime.now());
                
                entityManager.merge(tecnico);
                transaction.commit();
                
                mostrarMensaje("Contraseña cambiada exitosamente");
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al cambiar contraseña", e.getMessage());
            }
        });
    }
    
    private void editarAdministrador(Administrador admin) {
        if (admin == null) {
            mostrarError("Error", "No hay administrador seleccionado");
            return;
        }
        
        // Crear diálogo para editar administrador
        Dialog<Administrador> dialog = new Dialog<>();
        dialog.setTitle("Editar Administrador");
        dialog.setHeaderText("Editar información del administrador");
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(admin.getNombre());
        TextField apellidoField = new TextField(admin.getApellido());
        TextField emailField = new TextField(admin.getEmail());
        TextField telefonoField = new TextField(admin.getTelefono());
        TextField nombreUsuarioField = new TextField(admin.getNombreUsuario());
        TextField cargoField = new TextField(admin.getCargo());
        
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Apellido:"), 0, 1);
        grid.add(apellidoField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Teléfono:"), 0, 3);
        grid.add(telefonoField, 1, 3);
        grid.add(new Label("Nombre de Usuario:"), 0, 4);
        grid.add(nombreUsuarioField, 1, 4);
        grid.add(new Label("Cargo:"), 0, 5);
        grid.add(cargoField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                // Actualizar datos
                admin.setNombre(nombreField.getText().trim());
                admin.setApellido(apellidoField.getText().trim());
                admin.setEmail(emailField.getText().trim());
                admin.setTelefono(telefonoField.getText().trim());
                admin.setNombreUsuario(nombreUsuarioField.getText().trim());
                admin.setCargo(cargoField.getText().trim());
                admin.setUltimaModificacion(LocalDateTime.now());
                return admin;
            }
            return null;
        });
        
        // Procesar resultado
        Optional<Administrador> resultado = dialog.showAndWait();
        resultado.ifPresent(adminActualizado -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                entityManager.merge(adminActualizado);
                transaction.commit();
                mostrarMensaje("Administrador actualizado exitosamente");
                cargarAdministradores(administradoresTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al actualizar administrador", e.getMessage());
            }
        });
    }
    
    private void activarDesactivarAdministrador(Administrador admin) {
        if (admin == null) {
            mostrarError("Error", "No hay administrador seleccionado");
            return;
        }
        
        boolean nuevoEstado = !admin.isActivo();
        String accion = nuevoEstado ? "activar" : "desactivar";
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar operación");
        confirmacion.setHeaderText("¿Está seguro que desea " + accion + " al administrador?");
        confirmacion.setContentText("Administrador: " + admin.getNombre() + " " + admin.getApellido());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                admin.setActivo(nuevoEstado);
                admin.setUltimaModificacion(LocalDateTime.now());
                
                entityManager.merge(admin);
                transaction.commit();
                
                mostrarMensaje("Administrador " + accion + "do exitosamente");
                cargarAdministradores(administradoresTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al " + accion + " administrador", e.getMessage());
            }
        }
    }
    
    private void eliminarAdministrador(Administrador admin) {
        if (admin == null) {
            mostrarError("Error", "No hay administrador seleccionado");
            return;
        }
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar al administrador?");
        confirmacion.setContentText("Esta acción no se puede deshacer.\nAdministrador: " + admin.getNombre() + " " + admin.getApellido());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Comprobar si tiene departamentos gestionados
                if (admin.getDepartamentosGestionados() != null && !admin.getDepartamentosGestionados().isEmpty()) {
                    throw new IllegalStateException("No se puede eliminar el administrador porque tiene departamentos asignados");
                }
                
                entityManager.remove(
                    entityManager.contains(admin) ? admin : entityManager.merge(admin)
                );
                
                transaction.commit();
                mostrarMensaje("Administrador eliminado exitosamente");
                cargarAdministradores(administradoresTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar administrador", e.getMessage());
            }
        }
    }
    
    private void cambiarContrasenaAdministrador(Administrador admin) {
        if (admin == null) {
            mostrarError("Error", "No hay administrador seleccionado");
            return;
        }
        
        // Crear diálogo para cambiar contraseña
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar Contraseña");
        dialog.setHeaderText("Cambiar contraseña para " + admin.getNombre() + " " + admin.getApellido());
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        PasswordField nuevaPasswordField = new PasswordField();
        PasswordField confirmarPasswordField = new PasswordField();
        
        grid.add(new Label("Nueva contraseña:"), 0, 0);
        grid.add(nuevaPasswordField, 1, 0);
        grid.add(new Label("Confirmar contraseña:"), 0, 1);
        grid.add(confirmarPasswordField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Validación de contraseñas
        Node botonGuardar = dialog.getDialogPane().lookupButton(guardarButton);
        botonGuardar.setDisable(true);
        
        // Habilitar botón sólo si las contraseñas coinciden y no están vacías
        confirmarPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(
                nuevaPasswordField.getText().trim().isEmpty() || 
                !nuevaPasswordField.getText().equals(confirmarPasswordField.getText())
            );
        });
        
        nuevaPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            botonGuardar.setDisable(
                newValue.trim().isEmpty() || 
                !newValue.equals(confirmarPasswordField.getText())
            );
        });
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                return nuevaPasswordField.getText();
            }
            return null;
        });
        
        // Procesar resultado
        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(nuevaPassword -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                admin.setPassword(nuevaPassword); // En producción, esto debería estar hasheado
                admin.setUltimaModificacion(LocalDateTime.now());
                
                entityManager.merge(admin);
                transaction.commit();
                
                mostrarMensaje("Contraseña cambiada exitosamente");
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al cambiar contraseña", e.getMessage());
            }
        });
    }
    
    private void eliminarTicket(Ticket ticket) {
        if (ticket == null) {
            mostrarError("Error", "No hay ticket seleccionado");
            return;
        }
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar el ticket #" + ticket.getId() + "?");
        confirmacion.setContentText("Esta acción no se puede deshacer.\nTicket: " + ticket.getTitulo());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Eliminar todas las notas asociadas al ticket
                for (NotaTicket nota : new ArrayList<>(ticket.getNotas())) {
                    entityManager.remove(nota);
                }
                
                // Eliminar el ticket
                entityManager.remove(
                    entityManager.contains(ticket) ? ticket : entityManager.merge(ticket)
                );
                
                transaction.commit();
                mostrarMensaje("Ticket eliminado exitosamente");
                cargarTickets(ticketsTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar ticket", e.getMessage());
            }
        }
    }
    
    private void cargarAdministradores(TableView<Administrador> tabla) {
        tabla.getItems().clear();
        List<Administrador> administradores = entityManager.createQuery(
            "SELECT a FROM Administrador a", Administrador.class).getResultList();
        tabla.getItems().addAll(administradores);
    }
    
    private Tab crearTabRolesPermisos() {
        Tab tab = new Tab("Roles y Permisos");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Crear TabPane para roles y permisos
        TabPane rolesPermisosTabPane = new TabPane();
        
        // Pestaña para Roles
        Tab rolesTab = new Tab("Roles");
        rolesTab.setClosable(false);
        rolesTab.setContent(crearContenidoRoles());
        
        // Pestaña para Permisos
        Tab permisosTab = new Tab("Permisos");
        permisosTab.setClosable(false);
        permisosTab.setContent(crearContenidoPermisos());
        
        rolesPermisosTabPane.getTabs().addAll(rolesTab, permisosTab);
        content.getChildren().add(rolesPermisosTabPane);
        
        tab.setContent(content);
        return tab;
    }
    
    private VBox crearContenidoRoles() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Formulario para crear rol
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        
        // Nombre del rol
        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);
        
        // Descripción del rol
        formGrid.add(new Label("Descripción:"), 0, 1);
        TextArea descripcionArea = new TextArea();
        descripcionArea.setPrefRowCount(3);
        formGrid.add(descripcionArea, 1, 1);
        
        // Selección de permisos
        formGrid.add(new Label("Permisos:"), 0, 2);
        ListView<Permiso> permisosListView = new ListView<>();
        permisosListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Cargar permisos disponibles
        List<Permiso> permisos = entityManager.createQuery("SELECT p FROM Permiso p", Permiso.class).getResultList();
        permisosListView.getItems().addAll(permisos);
        
        // Configurar cómo se muestran los permisos
        permisosListView.setCellFactory(param -> new ListCell<Permiso>() {
            @Override
            protected void updateItem(Permiso item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre() + " - " + item.getDescripcion());
                }
            }
        });
        
        permisosListView.setPrefHeight(150);
        formGrid.add(permisosListView, 1, 2);
        
        // Botón para crear rol
        Button crearButton = new Button("Crear Rol");
        crearButton.setOnAction(e -> {
            crearRol(nombreField.getText(), descripcionArea.getText(), 
                     new ArrayList<>(permisosListView.getSelectionModel().getSelectedItems()));
            nombreField.clear();
            descripcionArea.clear();
            permisosListView.getSelectionModel().clearSelection();
            cargarRolesEnTabla();
        });
        
        content.getChildren().addAll(formGrid, crearButton);
        
        // Tabla de roles
        TableView<Rol> rolesTable = new TableView<>();
        
        TableColumn<Rol, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Rol, String> descripcionCol = new TableColumn<>("Descripción");
        TableColumn<Rol, String> permisosCol = new TableColumn<>("Permisos");
        
        nombreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        descripcionCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcion()));
        permisosCol.setCellValueFactory(cellData -> {
            Set<Permiso> permisosRol = cellData.getValue().getPermisos();
            if (permisosRol == null || permisosRol.isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty("Sin permisos");
            }
            StringBuilder sb = new StringBuilder();
            for (Permiso p : permisosRol) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(p.getNombre());
            }
            return new javafx.beans.property.SimpleStringProperty(sb.toString());
        });
        
        rolesTable.getColumns().addAll(Arrays.asList(nombreCol, descripcionCol, permisosCol));
        
        // Menú contextual para roles
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editarItem = new MenuItem("Editar");
        MenuItem eliminarItem = new MenuItem("Eliminar");
        
        editarItem.setOnAction(e -> editarRol(rolesTable.getSelectionModel().getSelectedItem()));
        eliminarItem.setOnAction(e -> eliminarRol(rolesTable.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().addAll(editarItem, eliminarItem);
        rolesTable.setContextMenu(contextMenu);
        
        content.getChildren().add(rolesTable);
        
        // Cargar roles existentes
        cargarRoles(rolesTable);
        
        return content;
    }
    
    private void cargarRoles(TableView<Rol> tabla) {
        tabla.getItems().clear();
        List<Rol> roles = entityManager.createQuery("SELECT r FROM Rol r", Rol.class).getResultList();
        tabla.getItems().addAll(roles);
    }
    
    private void crearRol(String nombre, String descripcion, List<Permiso> permisos) {
        if (nombre == null || nombre.trim().isEmpty()) {
            mostrarError("Error de validación", "El nombre del rol es obligatorio");
            return;
        }
        
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            Rol rol = new Rol();
            rol.setNombre(nombre.toUpperCase().trim());
            rol.setDescripcion(descripcion != null ? descripcion.trim() : "");
            
            if (permisos != null && !permisos.isEmpty()) {
                rol.setPermisos(new HashSet<>(permisos));
            }
            
            entityManager.persist(rol);
            transaction.commit();
            
            mostrarMensaje("Rol creado exitosamente");
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear rol", e.getMessage());
        }
    }
    
    private void editarRol(Rol rol) {
        if (rol == null) {
            mostrarError("Error", "No hay rol seleccionado");
            return;
        }
        
        // Crear diálogo para editar rol
        Dialog<Rol> dialog = new Dialog<>();
        dialog.setTitle("Editar Rol");
        dialog.setHeaderText("Editar información del rol");
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(rol.getNombre());
        TextArea descripcionArea = new TextArea(rol.getDescripcion());
        descripcionArea.setPrefRowCount(3);
        
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(descripcionArea, 1, 1);
        
        // Selección de permisos
        grid.add(new Label("Permisos:"), 0, 2);
        ListView<Permiso> permisosListView = new ListView<>();
        permisosListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Cargar permisos disponibles
        List<Permiso> todosPermisos = entityManager.createQuery("SELECT p FROM Permiso p", Permiso.class).getResultList();
        permisosListView.getItems().addAll(todosPermisos);
        
        // Configurar cómo se muestran los permisos
        permisosListView.setCellFactory(param -> new ListCell<Permiso>() {
            @Override
            protected void updateItem(Permiso item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombre() + " - " + item.getDescripcion());
                }
            }
        });
        
        // Seleccionar permisos actuales
        if (rol.getPermisos() != null) {
            for (Permiso p : rol.getPermisos()) {
                for (int i = 0; i < permisosListView.getItems().size(); i++) {
                    if (permisosListView.getItems().get(i).getId().equals(p.getId())) {
                        permisosListView.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        }
        
        permisosListView.setPrefHeight(150);
        grid.add(permisosListView, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                // Actualizar datos
                rol.setNombre(nombreField.getText().toUpperCase().trim());
                rol.setDescripcion(descripcionArea.getText().trim());
                
                // Actualizar permisos
                Set<Permiso> permisosSeleccionados = new HashSet<>(permisosListView.getSelectionModel().getSelectedItems());
                rol.setPermisos(permisosSeleccionados);
                
                return rol;
            }
            return null;
        });
        
        // Procesar resultado
        Optional<Rol> resultado = dialog.showAndWait();
        resultado.ifPresent(rolActualizado -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                entityManager.merge(rolActualizado);
                transaction.commit();
                mostrarMensaje("Rol actualizado exitosamente");
                cargarRolesEnTabla();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al actualizar rol", e.getMessage());
            }
        });
    }
    
    private void eliminarRol(Rol rol) {
        if (rol == null) {
            mostrarError("Error", "No hay rol seleccionado");
            return;
        }
        
        // Verificar si hay usuarios que usan este rol
        List<Usuario> usuariosConRol = entityManager.createQuery(
            "SELECT u FROM Usuario u WHERE u.rol = :rol", Usuario.class)
            .setParameter("rol", rol)
            .getResultList();
        
        if (!usuariosConRol.isEmpty()) {
            mostrarError("No se puede eliminar", 
                "El rol está siendo utilizado por " + usuariosConRol.size() + " usuarios");
            return;
        }
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar el rol?");
        confirmacion.setContentText("Esta acción no se puede deshacer.\nRol: " + rol.getNombre());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                entityManager.remove(
                    entityManager.contains(rol) ? rol : entityManager.merge(rol)
                );
                
                transaction.commit();
                mostrarMensaje("Rol eliminado exitosamente");
                cargarRolesEnTabla();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar rol", e.getMessage());
            }
        }
    }
    
    private void cargarRolesEnTabla() {
        // Este método se utiliza para recargar todas las tablas que muestran roles
        // Recarga tanto en el panel de gestión de roles como en los ComboBox
    }
    
    private VBox crearContenidoPermisos() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Formulario para crear permiso
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        
        // Nombre del permiso
        formGrid.add(new Label("Nombre:"), 0, 0);
        TextField nombreField = new TextField();
        formGrid.add(nombreField, 1, 0);
        
        // Descripción del permiso
        formGrid.add(new Label("Descripción:"), 0, 1);
        TextArea descripcionArea = new TextArea();
        descripcionArea.setPrefRowCount(3);
        formGrid.add(descripcionArea, 1, 1);
        
        // Botón para crear permiso
        Button crearButton = new Button("Crear Permiso");
        crearButton.setOnAction(e -> {
            crearPermiso(nombreField.getText(), descripcionArea.getText());
            nombreField.clear();
            descripcionArea.clear();
            cargarPermisosEnTabla();
        });
        
        content.getChildren().addAll(formGrid, crearButton);
        
        // Tabla de permisos
        TableView<Permiso> permisosTable = new TableView<>();
        
        TableColumn<Permiso, String> nombreCol = new TableColumn<>("Nombre");
        TableColumn<Permiso, String> descripcionCol = new TableColumn<>("Descripción");
        
        nombreCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNombre()));
        descripcionCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcion()));
        
        permisosTable.getColumns().addAll(Arrays.asList(nombreCol, descripcionCol));
        
        // Menú contextual para permisos
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editarItem = new MenuItem("Editar");
        MenuItem eliminarItem = new MenuItem("Eliminar");
        
        editarItem.setOnAction(e -> editarPermiso(permisosTable.getSelectionModel().getSelectedItem()));
        eliminarItem.setOnAction(e -> eliminarPermiso(permisosTable.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().addAll(editarItem, eliminarItem);
        permisosTable.setContextMenu(contextMenu);
        
        content.getChildren().add(permisosTable);
        
        // Cargar permisos existentes
        cargarPermisos(permisosTable);
        
        return content;
    }
    
    private void cargarPermisos(TableView<Permiso> tabla) {
        tabla.getItems().clear();
        List<Permiso> permisos = entityManager.createQuery("SELECT p FROM Permiso p", Permiso.class).getResultList();
        tabla.getItems().addAll(permisos);
    }
    
    private void crearPermiso(String nombre, String descripcion) {
        if (nombre == null || nombre.trim().isEmpty()) {
            mostrarError("Error de validación", "El nombre del permiso es obligatorio");
            return;
        }
        
        EntityTransaction transaction = null;
        try {
            transaction = entityManager.getTransaction();
            transaction.begin();
            
            Permiso permiso = new Permiso();
            permiso.setNombre(nombre.toUpperCase().trim());
            permiso.setDescripcion(descripcion != null ? descripcion.trim() : "");
            
            entityManager.persist(permiso);
            transaction.commit();
            
            mostrarMensaje("Permiso creado exitosamente");
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            mostrarError("Error al crear permiso", e.getMessage());
        }
    }
    
    private void editarPermiso(Permiso permiso) {
        if (permiso == null) {
            mostrarError("Error", "No hay permiso seleccionado");
            return;
        }
        
        // Crear diálogo para editar permiso
        Dialog<Permiso> dialog = new Dialog<>();
        dialog.setTitle("Editar Permiso");
        dialog.setHeaderText("Editar información del permiso");
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(permiso.getNombre());
        TextArea descripcionArea = new TextArea(permiso.getDescripcion());
        descripcionArea.setPrefRowCount(3);
        
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(descripcionArea, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                // Actualizar datos
                permiso.setNombre(nombreField.getText().toUpperCase().trim());
                permiso.setDescripcion(descripcionArea.getText().trim());
                return permiso;
            }
            return null;
        });
        
        // Procesar resultado
        Optional<Permiso> resultado = dialog.showAndWait();
        resultado.ifPresent(permisoActualizado -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                entityManager.merge(permisoActualizado);
                transaction.commit();
                mostrarMensaje("Permiso actualizado exitosamente");
                cargarPermisosEnTabla();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al actualizar permiso", e.getMessage());
            }
        });
    }
    
    private void eliminarPermiso(Permiso permiso) {
        if (permiso == null) {
            mostrarError("Error", "No hay permiso seleccionado");
            return;
        }
        
        // Verificar si hay roles que usan este permiso
        List<Rol> rolesConPermiso = entityManager.createQuery(
            "SELECT r FROM Rol r JOIN r.permisos p WHERE p = :permiso", Rol.class)
            .setParameter("permiso", permiso)
            .getResultList();
        
        if (!rolesConPermiso.isEmpty()) {
            mostrarError("No se puede eliminar", 
                "El permiso está siendo utilizado por " + rolesConPermiso.size() + " roles");
            return;
        }
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar el permiso?");
        confirmacion.setContentText("Esta acción no se puede deshacer.\nPermiso: " + permiso.getNombre());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                entityManager.remove(
                    entityManager.contains(permiso) ? permiso : entityManager.merge(permiso)
                );
                
                transaction.commit();
                mostrarMensaje("Permiso eliminado exitosamente");
                cargarPermisosEnTabla();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar permiso", e.getMessage());
            }
        }
    }
    
    private void cargarPermisosEnTabla() {
        // Este método se utiliza para recargar todas las tablas que muestran permisos
    }
    
    private void asignarTecnicosDepartamento(Departamento departamento) {
        if (departamento == null) {
            mostrarError("Error", "No hay departamento seleccionado");
            return;
        }
        
        // Crear diálogo para asignar técnicos
        Dialog<List<Tecnico>> dialog = new Dialog<>();
        dialog.setTitle("Asignar Técnicos a " + departamento.getNombre());
        dialog.setHeaderText("Seleccione los técnicos que pertenecerán al departamento");
        
        // Contenido
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Lista de técnicos disponibles
        ListView<Tecnico> tecnicosListView = new ListView<>();
        tecnicosListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Cargar todos los técnicos
        List<Tecnico> todosTecnicos = entityManager.createQuery(
            "SELECT t FROM Tecnico t", Tecnico.class).getResultList();
        tecnicosListView.getItems().addAll(todosTecnicos);
        
        // Configurar cómo se muestran los técnicos
        tecnicosListView.setCellFactory(param -> new ListCell<Tecnico>() {
            @Override
            protected void updateItem(Tecnico item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String departamentoText = item.getDepartamento() != null ? 
                        " (" + item.getDepartamento().getNombre() + ")" : " (Sin departamento)";
                    setText(item.getNombre() + " " + item.getApellido() + " - " + item.getEspecialidad() + departamentoText);
                    
                    // Marcar los que ya están en este departamento
                    if (item.getDepartamento() != null && 
                        item.getDepartamento().getId().equals(departamento.getId())) {
                        getStyleClass().add("selected-item");
                    } else {
                        getStyleClass().remove("selected-item");
                    }
                }
            }
        });
        
        // Seleccionar los técnicos actuales del departamento
        for (int i = 0; i < tecnicosListView.getItems().size(); i++) {
            Tecnico tecnico = tecnicosListView.getItems().get(i);
            if (tecnico.getDepartamento() != null && 
                tecnico.getDepartamento().getId().equals(departamento.getId())) {
                tecnicosListView.getSelectionModel().select(i);
            }
        }
        
        content.getChildren().addAll(
            new Label("Técnicos disponibles:"),
            tecnicosListView
        );
        
        // Configurar el diálogo
        dialog.getDialogPane().setContent(content);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                return new ArrayList<>(tecnicosListView.getSelectionModel().getSelectedItems());
            }
            return null;
        });
        
        // Procesar resultado
        Optional<List<Tecnico>> resultado = dialog.showAndWait();
        resultado.ifPresent(tecnicosSeleccionados -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                // Quitar todos los técnicos actuales del departamento
                List<Tecnico> tecnicosActuales = entityManager.createQuery(
                    "SELECT t FROM Tecnico t WHERE t.departamento = :departamento", Tecnico.class)
                    .setParameter("departamento", departamento)
                    .getResultList();
                
                for (Tecnico tecnico : tecnicosActuales) {
                    if (!tecnicosSeleccionados.contains(tecnico)) {
                        tecnico.setDepartamento(null);
                        entityManager.merge(tecnico);
                    }
                }
                
                // Asignar los técnicos seleccionados al departamento
                for (Tecnico tecnico : tecnicosSeleccionados) {
                    tecnico.setDepartamento(departamento);
                    entityManager.merge(tecnico);
                }
                
                transaction.commit();
                mostrarMensaje("Técnicos asignados exitosamente");
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al asignar técnicos", e.getMessage());
            }
        });
    }
    
    private void editarDepartamento(Departamento departamento) {
        if (departamento == null) {
            mostrarError("Error", "No hay departamento seleccionado");
            return;
        }
        
        // Crear diálogo para editar departamento
        Dialog<Departamento> dialog = new Dialog<>();
        dialog.setTitle("Editar Departamento");
        dialog.setHeaderText("Editar información del departamento");
        
        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nombreField = new TextField(departamento.getNombre());
        TextArea descripcionArea = new TextArea(departamento.getDescripcion());
        descripcionArea.setPrefRowCount(3);
        
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(nombreField, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(descripcionArea, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Botones
        ButtonType guardarButton = new ButtonType("Guardar", ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButton, cancelarButton);
        
        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButton) {
                // Actualizar datos
                departamento.setNombre(nombreField.getText().trim());
                departamento.setDescripcion(descripcionArea.getText().trim());
                return departamento;
            }
            return null;
        });
        
        // Procesar resultado
        Optional<Departamento> resultado = dialog.showAndWait();
        resultado.ifPresent(departamentoActualizado -> {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                entityManager.merge(departamentoActualizado);
                transaction.commit();
                mostrarMensaje("Departamento actualizado exitosamente");
                cargarDepartamentos(departamentosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al actualizar departamento", e.getMessage());
            }
        });
    }
    
    private void eliminarDepartamento(Departamento departamento) {
        if (departamento == null) {
            mostrarError("Error", "No hay departamento seleccionado");
            return;
        }
        
        // Verificar si hay tickets o técnicos asignados
        List<Ticket> ticketsAsignados = entityManager.createQuery(
            "SELECT t FROM Ticket t WHERE t.departamento = :departamento", Ticket.class)
            .setParameter("departamento", departamento)
            .getResultList();
        
        List<Tecnico> tecnicosAsignados = entityManager.createQuery(
            "SELECT t FROM Tecnico t WHERE t.departamento = :departamento", Tecnico.class)
            .setParameter("departamento", departamento)
            .getResultList();
        
        if (!ticketsAsignados.isEmpty() || !tecnicosAsignados.isEmpty()) {
            StringBuilder mensaje = new StringBuilder("No se puede eliminar el departamento porque tiene ");
            if (!ticketsAsignados.isEmpty()) {
                mensaje.append(ticketsAsignados.size()).append(" tickets asignados");
            }
            
            if (!tecnicosAsignados.isEmpty()) {
                if (!ticketsAsignados.isEmpty()) {
                    mensaje.append(" y ");
                }
                mensaje.append(tecnicosAsignados.size()).append(" técnicos asignados");
            }
            
            mostrarError("No se puede eliminar", mensaje.toString());
            return;
        }
        
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar el departamento?");
        confirmacion.setContentText("Esta acción no se puede deshacer.\nDepartamento: " + departamento.getNombre());
        
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            EntityTransaction transaction = null;
            try {
                transaction = entityManager.getTransaction();
                transaction.begin();
                
                entityManager.remove(
                    entityManager.contains(departamento) ? departamento : entityManager.merge(departamento)
                );
                
                transaction.commit();
                mostrarMensaje("Departamento eliminado exitosamente");
                cargarDepartamentos(departamentosTable);
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                mostrarError("Error al eliminar departamento", e.getMessage());
            }
        }
    }
    
    private Tab crearTabMisTickets() {
        Tab tab = new Tab("Mis Tickets");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Obtener el usuario actual
        Usuario usuarioActual = SesionServicio.getInstancia().getUsuarioActual();
        
        // Título
        Label tituloLabel = new Label("Mis Tickets");
        tituloLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        content.getChildren().add(tituloLabel);
        
        // Panel de filtros
        HBox filtrosBox = new HBox(10);
        filtrosBox.setPadding(new Insets(5));
        
        // Filtro por estado
        ComboBox<String> estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll("TODOS", "NUEVO", "ABIERTO", "EN_PROCESO", "EN_ESPERA", "RESUELTO", "CERRADO");
        estadoCombo.setValue("TODOS");
        estadoCombo.setPromptText("Filtrar por Estado");
        
        // Filtro por fecha
        DatePicker fechaDesde = new DatePicker();
        fechaDesde.setPromptText("Fecha desde");
        DatePicker fechaHasta = new DatePicker();
        fechaHasta.setPromptText("Fecha hasta");
        
        // Botón para aplicar filtros
        Button aplicarFiltrosBtn = new Button("Aplicar Filtros");
        Button limpiarFiltrosBtn = new Button("Limpiar Filtros");
        
        filtrosBox.getChildren().addAll(
            new Label("Estado:"), estadoCombo,
            new Label("Desde:"), fechaDesde,
            new Label("Hasta:"), fechaHasta,
            aplicarFiltrosBtn, limpiarFiltrosBtn
        );
        
        content.getChildren().add(filtrosBox);
        
        // Tabla de tickets
        TableView<Ticket> misTicketsTable = new TableView<>();
        
        TableColumn<Ticket, Long> idCol = new TableColumn<>("ID");
        TableColumn<Ticket, String> tituloCol = new TableColumn<>("Título");
        TableColumn<Ticket, String> estadoCol = new TableColumn<>("Estado");
        TableColumn<Ticket, String> departamentoCol = new TableColumn<>("Departamento");
        TableColumn<Ticket, String> fechaCreacionCol = new TableColumn<>("Fecha Creación");
        TableColumn<Ticket, String> tecnicoCol = new TableColumn<>("Técnico Asignado");
        
        idCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleLongProperty(cellData.getValue().getId()).asObject());
        tituloCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitulo()));
        estadoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado()));
        departamentoCol.setCellValueFactory(cellData -> {
            Departamento departamento = cellData.getValue().getDepartamento();
            return new javafx.beans.property.SimpleStringProperty(
                departamento != null ? departamento.getNombre() : "");
        });
        fechaCreacionCol.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getFechaCreacion();
            return new javafx.beans.property.SimpleStringProperty(
                fecha != null ? fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });
        tecnicoCol.setCellValueFactory(cellData -> {
            Persona tecnico = cellData.getValue().getTecnicoAsignado();
            return new javafx.beans.property.SimpleStringProperty(
                tecnico != null ? tecnico.getNombre() + " " + tecnico.getApellido() : "Sin asignar");
        });
        
        misTicketsTable.getColumns().addAll(
            Arrays.asList(idCol, tituloCol, estadoCol, departamentoCol, fechaCreacionCol, tecnicoCol)
        );
        
        // Cargar tickets del usuario actual
        List<Ticket> misTickets = entityManager.createQuery(
            "SELECT t FROM Ticket t WHERE t.solicitante = :usuario ORDER BY t.fechaCreacion DESC", 
            Ticket.class)
            .setParameter("usuario", usuarioActual)
            .getResultList();
        
        misTicketsTable.getItems().addAll(misTickets);
        
        // Configurar acción para aplicar filtros
        aplicarFiltrosBtn.setOnAction(e -> {
            String estadoFiltro = estadoCombo.getValue();
            LocalDate desde = fechaDesde.getValue();
            LocalDate hasta = fechaHasta.getValue();
            
            // Construir consulta con filtros
            StringBuilder queryString = new StringBuilder("SELECT t FROM Ticket t WHERE t.solicitante = :usuario");
            
            if (!"TODOS".equals(estadoFiltro)) {
                queryString.append(" AND t.estado = :estado");
            }
            
            if (desde != null) {
                queryString.append(" AND t.fechaCreacion >= :desde");
            }
            
            if (hasta != null) {
                queryString.append(" AND t.fechaCreacion <= :hasta");
            }
            
            queryString.append(" ORDER BY t.fechaCreacion DESC");
            
            try {
                javax.persistence.TypedQuery<Ticket> query = entityManager.createQuery(queryString.toString(), Ticket.class)
                    .setParameter("usuario", usuarioActual);
                
                if (!"TODOS".equals(estadoFiltro)) {
                    query.setParameter("estado", estadoFiltro);
                }
                
                if (desde != null) {
                    query.setParameter("desde", desde.atStartOfDay());
                }
                
                if (hasta != null) {
                    query.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
                }
                
                List<Ticket> ticketsFiltrados = query.getResultList();
                misTicketsTable.getItems().clear();
                misTicketsTable.getItems().addAll(ticketsFiltrados);
            } catch (Exception ex) {
                mostrarError("Error al aplicar filtros", ex.getMessage());
            }
        });
        
        limpiarFiltrosBtn.setOnAction(e -> {
            estadoCombo.setValue("TODOS");
            fechaDesde.setValue(null);
            fechaHasta.setValue(null);
            misTicketsTable.getItems().clear();
            misTicketsTable.getItems().addAll(misTickets);
        });
        
        // Menú contextual para ver detalles
        ContextMenu contextMenu = new ContextMenu();
        MenuItem verDetallesItem = new MenuItem("Ver Detalles");
        
        verDetallesItem.setOnAction(e -> 
            mostrarDetallesTicket(misTicketsTable.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().add(verDetallesItem);
        misTicketsTable.setContextMenu(contextMenu);
        
        // Botón para ver detalles
        Button verDetallesBtn = new Button("Ver Detalles");
        verDetallesBtn.setOnAction(e -> {
            Ticket seleccionado = misTicketsTable.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                mostrarDetallesTicket(seleccionado);
            } else {
                mostrarError("Error", "Debe seleccionar un ticket para ver detalles");
            }
        });
        
        content.getChildren().addAll(misTicketsTable, verDetallesBtn);
        
        tab.setContent(content);
        return tab;
    }
    
    private Tab crearTabTicketsDepartamento() {
        Tab tab = new Tab("Tickets del Departamento");
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Obtener el usuario actual
        Usuario usuarioActual = SesionServicio.getInstancia().getUsuarioActual();
        Departamento departamentoTemp = null;
        
        // Obtener el departamento del usuario (si es técnico)
        if (usuarioActual instanceof Tecnico) {
            departamentoTemp = ((Tecnico) usuarioActual).getDepartamento();
        }
        
        // Variable final para usar en lambdas
        final Departamento departamentoUsuario = departamentoTemp;
        
        // Si no tiene departamento asignado, mostrar mensaje
        if (departamentoUsuario == null) {
            content.getChildren().add(new Label("No tiene un departamento asignado"));
            tab.setContent(content);
            return tab;
        }
        
        // Título
        Label tituloLabel = new Label("Tickets del Departamento: " + departamentoUsuario.getNombre());
        tituloLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        content.getChildren().add(tituloLabel);
        
        // Pestañas para ver tickets asignados y sin asignar
        TabPane ticketsTabPane = new TabPane();
        
        // Pestaña para tickets sin asignar
        Tab sinAsignarTab = new Tab("Sin Asignar");
        sinAsignarTab.setClosable(false);
        VBox sinAsignarContent = new VBox(10);
        sinAsignarContent.setPadding(new Insets(10));
        
        // Tabla de tickets sin asignar
        TableView<Ticket> ticketsSinAsignarTable = new TableView<>();
        configurarTablaTickets(ticketsSinAsignarTable);
        
        // Cargar tickets sin asignar del departamento
        List<Ticket> ticketsSinAsignar = entityManager.createQuery(
            "SELECT t FROM Ticket t WHERE t.departamento = :departamento AND t.tecnicoAsignado IS NULL ORDER BY t.prioridad, t.fechaCreacion", 
            Ticket.class)
            .setParameter("departamento", departamentoUsuario)
            .getResultList();
        
        ticketsSinAsignarTable.getItems().addAll(ticketsSinAsignar);
        
        // Botón para tomar ticket
        Button tomarTicketBtn = new Button("Tomar Ticket");
        tomarTicketBtn.setOnAction(e -> {
            Ticket seleccionado = ticketsSinAsignarTable.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                tomarTicket(seleccionado, departamentoUsuario);
                // Recargar tablas
                ticketsSinAsignarTable.getItems().clear();
                List<Ticket> ticketsActualizados = entityManager.createQuery(
                    "SELECT t FROM Ticket t WHERE t.departamento = :departamento AND t.tecnicoAsignado IS NULL ORDER BY t.prioridad, t.fechaCreacion", 
                    Ticket.class)
                    .setParameter("departamento", departamentoUsuario)
                    .getResultList();
                ticketsSinAsignarTable.getItems().addAll(ticketsActualizados);
            } else {
                mostrarError("Error", "Debe seleccionar un ticket para tomar");
            }
        });
        
        sinAsignarContent.getChildren().addAll(
            new Label("Tickets sin asignar: " + ticketsSinAsignar.size()),
            ticketsSinAsignarTable, 
            tomarTicketBtn
        );
        
        sinAsignarTab.setContent(sinAsignarContent);
        
        // Pestaña para tickets asignados al técnico actual
        Tab misAsignadosTab = new Tab("Mis Asignados");
        misAsignadosTab.setClosable(false);
        VBox misAsignadosContent = new VBox(10);
        misAsignadosContent.setPadding(new Insets(10));
        
        // Tabla de tickets asignados
        TableView<Ticket> ticketsAsignadosTable = new TableView<>();
        configurarTablaTickets(ticketsAsignadosTable);
        
        // Cargar tickets asignados al técnico
        List<Ticket> ticketsAsignados = entityManager.createQuery(
            "SELECT t FROM Ticket t WHERE t.tecnicoAsignado = :tecnico ORDER BY t.prioridad, t.fechaCreacion", 
            Ticket.class)
            .setParameter("tecnico", usuarioActual)
            .getResultList();
        
        ticketsAsignadosTable.getItems().addAll(ticketsAsignados);
        
        // Botón para ver detalles
        Button verDetallesBtn = new Button("Ver Detalles");
        verDetallesBtn.setOnAction(e -> {
            Ticket seleccionado = ticketsAsignadosTable.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                mostrarDetallesTicket(seleccionado);
            } else {
                mostrarError("Error", "Debe seleccionar un ticket para ver detalles");
            }
        });
        
        misAsignadosContent.getChildren().addAll(
            new Label("Tickets asignados a mí: " + ticketsAsignados.size()),
            ticketsAsignadosTable, 
            verDetallesBtn
        );
        
        misAsignadosTab.setContent(misAsignadosContent);
        
        // Pestaña para todos los tickets del departamento (solo para administradores)
        if (SesionServicio.getInstancia().tieneRolAdministrador()) {
            Tab todosTab = new Tab("Todos los Tickets");
            todosTab.setClosable(false);
            
            Button verColaDepartamentoBtn = new Button("Ver Todos los Tickets del Departamento");
            verColaDepartamentoBtn.setOnAction(e -> verColaTicketsDepartamento(departamentoUsuario));
            
            VBox todosContent = new VBox(10);
            todosContent.setPadding(new Insets(10));
            todosContent.getChildren().add(verColaDepartamentoBtn);
            
            todosTab.setContent(todosContent);
            ticketsTabPane.getTabs().add(todosTab);
        }
        
        ticketsTabPane.getTabs().addAll(sinAsignarTab, misAsignadosTab);
        content.getChildren().add(ticketsTabPane);
        
        tab.setContent(content);
        return tab;
    }
    
    private void configurarTablaTickets(TableView<Ticket> tabla) {
        TableColumn<Ticket, Long> idCol = new TableColumn<>("ID");
        TableColumn<Ticket, String> tituloCol = new TableColumn<>("Título");
        TableColumn<Ticket, String> prioridadCol = new TableColumn<>("Prioridad");
        TableColumn<Ticket, String> estadoCol = new TableColumn<>("Estado");
        TableColumn<Ticket, String> solicitanteCol = new TableColumn<>("Solicitante");
        TableColumn<Ticket, String> fechaCreacionCol = new TableColumn<>("Fecha Creación");
        
        idCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleLongProperty(cellData.getValue().getId()).asObject());
        tituloCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitulo()));
        prioridadCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPrioridad().toString()));
        estadoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEstado()));
        solicitanteCol.setCellValueFactory(cellData -> {
            Persona solicitante = cellData.getValue().getSolicitante();
            return new javafx.beans.property.SimpleStringProperty(
                solicitante != null ? solicitante.getNombre() + " " + solicitante.getApellido() : "");
        });
        fechaCreacionCol.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getFechaCreacion();
            return new javafx.beans.property.SimpleStringProperty(
                fecha != null ? fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });
        
        tabla.getColumns().addAll(
            Arrays.asList(idCol, tituloCol, prioridadCol, estadoCol, solicitanteCol, fechaCreacionCol)
        );
        
        // Menú contextual
        ContextMenu contextMenu = new ContextMenu();
        MenuItem verDetallesItem = new MenuItem("Ver Detalles");
        
        verDetallesItem.setOnAction(e -> 
            mostrarDetallesTicket(tabla.getSelectionModel().getSelectedItem()));
        
        contextMenu.getItems().add(verDetallesItem);
        tabla.setContextMenu(contextMenu);
    }
} 