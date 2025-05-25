# Sistema de Tickets de Servicio

Este es un sistema de gestión de tickets de servicio desarrollado en Java utilizando JavaFX para la interfaz gráfica y PostgreSQL como base de datos.

## Características

- Gestión de tickets de servicio
- Gestión de usuarios (Administradores, Técnicos y Usuarios)
- Gestión de departamentos
- Sistema de colas para tickets
- Historial de cambios de estado con capacidad de deshacer
- Interfaz gráfica moderna y amigable
- Persistencia de datos en PostgreSQL

## Requisitos

- Java 17 o superior
- PostgreSQL 12 o superior
- Maven 3.6 o superior

## Configuración

1. Clonar el repositorio
2. Configurar la base de datos PostgreSQL:
   - Crear una base de datos llamada `sistema_tickets`
   - Modificar las credenciales en `ConexionDB.java` según corresponda

3. Compilar el proyecto:
```bash
mvn clean install
```

4. Ejecutar la aplicación:
```bash
mvn javafx:run
```

## Estructura del Proyecto

```
src/main/java/com/serviciotickets/
├── Main.java                 # Clase principal con la interfaz gráfica
├── modelo/
│   ├── Persona.java         # Clase base abstracta
│   ├── Usuario.java         # Clase para usuarios normales
│   ├── Tecnico.java         # Clase para técnicos
│   ├── Administrador.java   # Clase para administradores
│   ├── Ticket.java          # Clase para tickets
│   └── Departamento.java    # Clase para departamentos
└── persistencia/
    └── ConexionDB.java      # Clase para manejo de la base de datos
```

## Características Técnicas

- Programación orientada a objetos
- Patrones de diseño
- Persistencia con Hibernate
- Interfaz gráfica con JavaFX
- Manejo de errores y excepciones
- Validación de datos
- Serialización de objetos

## Contribuir

1. Fork el proyecto
2. Crear una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abrir un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles. 