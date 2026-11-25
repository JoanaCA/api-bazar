package com.bazar.api_bazar.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base para tests que necesitan una base de datos real: levanta MySQL 8.4 en Docker.
 *
 * <p>El contenedor es un singleton: arranca la primera vez que se necesita y se reutiliza en toda la suite
 * (Testcontainers lo destruye al terminar la JVM). Si la máquina no tiene Docker, los tests que heredan de esta
 * clase se omiten en lugar de fallar.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMySqlTest {

    private static MySQLContainer mysql;

    private static synchronized MySQLContainer mysql() {
        if (mysql == null) {
            mysql = new MySQLContainer("mysql:8.4");
            mysql.start();
        }
        return mysql;
    }

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry registry) {
        MySQLContainer contenedor = mysql();
        registry.add("spring.datasource.url", contenedor::getJdbcUrl);
        registry.add("spring.datasource.username", contenedor::getUsername);
        registry.add("spring.datasource.password", contenedor::getPassword);
    }
}
