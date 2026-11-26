package com.bazar.api_bazar.repository;

import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.support.AbstractMySqlTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static com.bazar.api_bazar.support.TestData.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DisplayName("ProductoRepository (MySQL)")
class ProductoRepositoryIT extends AbstractMySqlTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findByCantidadDisponibleLessThan excluye el umbral, pagina y ordena")
    void findByCantidadDisponibleLessThan_limiteExclusivoYPaginado() {
        productoRepository.save(producto(null, "Vaso", "20.00", 0));
        productoRepository.save(producto(null, "Plato", "50.00", 4));
        productoRepository.save(producto(null, "Taza", "100.00", 5));
        productoRepository.save(producto(null, "Jarra", "200.00", 10));
        entityManager.flush();

        Page<Producto> pagina = productoRepository.findByCantidadDisponibleLessThan(
                5, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nombre"))));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(Producto::getNombre)
                .containsExactly("Plato", "Vaso");
    }

    @Test
    @DisplayName("@Version incrementa al actualizar y rechaza modificaciones con versión desactualizada")
    void version_rechazaModificacionConVersionObsoleta() {
        Producto guardado = productoRepository.saveAndFlush(producto(null, "Taza", "100.00", 10));
        Long id = guardado.getCodigoProducto();
        assertThat(guardado.getVersion()).isZero();
        entityManager.clear();

        // Dos lecturas que parten de la misma versión (0): la primera se separa del contexto
        Producto lecturaObsoleta = productoRepository.findById(id).orElseThrow();
        entityManager.detach(lecturaObsoleta);

        // Otra operación actualiza el producto y sube la versión en base de datos a 1
        Producto lecturaActual = productoRepository.findById(id).orElseThrow();
        lecturaActual.setCantidadDisponible(9);
        productoRepository.saveAndFlush(lecturaActual);
        assertThat(lecturaActual.getVersion()).isEqualTo(1L);
        entityManager.clear();

        // Intentar guardar la copia con versión 0 debe fallar por bloqueo optimista
        lecturaObsoleta.setCantidadDisponible(8);
        assertThatThrownBy(() -> productoRepository.saveAndFlush(lecturaObsoleta))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
