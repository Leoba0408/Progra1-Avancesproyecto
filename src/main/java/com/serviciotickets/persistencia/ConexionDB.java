package com.serviciotickets.persistencia;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

public class ConexionDB {
    private static EntityManagerFactory entityManagerFactory;

    public static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null) {
            try {
                Map<String, String> properties = new HashMap<>();
                properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
                properties.put("jakarta.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/sistema_tickets");
                properties.put("jakarta.persistence.jdbc.user", "postgres");
                properties.put("jakarta.persistence.jdbc.password", "1234");
                properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
                properties.put("hibernate.show_sql", "true");
                properties.put("hibernate.hbm2ddl.auto", "update");

                entityManagerFactory = Persistence.createEntityManagerFactory("sistema_tickets", properties);
            } catch (Exception e) {
                System.err.println("Error al crear EntityManagerFactory: " + e);
                throw new ExceptionInInitializerError(e);
            }
        }
        return entityManagerFactory;
    }

    public static void cerrarEntityManagerFactory() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
} 