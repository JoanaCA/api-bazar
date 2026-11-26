package com.bazar.api_bazar.integration;

import com.bazar.api_bazar.support.AbstractMySqlTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica las migraciones de Flyway contra MySQL real. Que el contexto arranque ya prueba que las entidades
 * coinciden con el esquema (ddl-auto=validate); acá además se comprueba el estado de las migraciones y las
 * restricciones de la base.
 */
@SpringBootTest
class FlywayMigrationIT extends AbstractMySqlTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbc.update("DELETE FROM cliente WHERE dni = '99000001'");
    }

    @Test
    @DisplayName("Todas las migraciones están aplicadas y sin errores")
    void migracionesAplicadas() {
        MigrationInfo[] migraciones = flyway.info().all();

        assertThat(migraciones).isNotEmpty();
        assertThat(migraciones).allSatisfy(m -> assertThat(m.getState()).isEqualTo(MigrationState.SUCCESS));
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
    }

    @Test
    @DisplayName("Existen las tablas del modelo")
    void existenLasTablas() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);

        assertThat(tablas).contains("cliente", "producto", "venta", "detalle_venta", "flyway_schema_history");
    }

    @Test
    @DisplayName("La base rechaza stock negativo aunque falle la lógica de la aplicación")
    void laBaseRechazaStockNegativo() {
        // MySQL informa la violación de un CHECK con el código 3819, que Spring no clasifica como integridad de datos.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO producto (nombre, marca, costo, cantidad_disponible, version) VALUES ('X', 'Y', 1.00, -1, 0)"))
                .hasMessageContaining("ck_producto_stock");
    }

    @Test
    @DisplayName("La base rechaza un DNI duplicado")
    void laBaseRechazaDniDuplicado() {
        jdbc.update("INSERT INTO cliente (nombre, apellido, dni) VALUES ('A', 'B', '99000001')");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO cliente (nombre, apellido, dni) VALUES ('C', 'D', '99000001')"))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
