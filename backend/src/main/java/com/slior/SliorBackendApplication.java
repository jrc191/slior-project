package com.slior;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de arranque de SLIOR Backend.
 *
 * <p>SLIOR (Sistema de Optimización de Rutas Logísticas) es una API REST
 * que permite gestionar rutas de reparto, optimizar el orden de las paradas
 * y registrar la entrega de paquetes mediante códigos de barras.</p>
 *
 * <p>Tecnologías: Spring Boot 3.2.4 · Java 17 · PostgreSQL 15</p>
 *
 * @author José Ramos Contioso
 * @version 1.0.0
 */
@SpringBootApplication
public class SliorBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SliorBackendApplication.class, args);
    }
}
