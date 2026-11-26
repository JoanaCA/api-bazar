package com.bazar.api_bazar.repository;

import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;
import com.bazar.api_bazar.support.AbstractMySqlTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static com.bazar.api_bazar.support.TestData.cliente;
import static com.bazar.api_bazar.support.TestData.detalle;
import static com.bazar.api_bazar.support.TestData.producto;
import static com.bazar.api_bazar.support.TestData.venta;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@DisplayName("VentaRepository (MySQL)")
class VentaRepositoryIT extends AbstractMySqlTest {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("totalesPorDia suma solo las ventas de la fecha indicada y devuelve ceros si no hay ventas")
    void totalesPorDia_agregaEnBdYDevuelveCerosSinVentas() {
        Cliente ana = clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        Producto taza = productoRepository.save(producto(null, "Taza", "100.50", 20));
        LocalDate dia1 = LocalDate.of(2026, 3, 7);
        LocalDate dia2 = LocalDate.of(2026, 3, 8);

        ventaRepository.save(venta(null, ana, dia1, detalle(taza, 2))); // 201.00
        ventaRepository.save(venta(null, ana, dia1, detalle(taza, 1))); // 100.50
        ventaRepository.save(venta(null, ana, dia2, detalle(taza, 5))); // 502.50 (otro día)
        entityManager.flush();

        VentasDiaResponse reporteDia1 = ventaRepository.totalesPorDia(dia1);
        assertThat(reporteDia1.cantidadVentas()).isEqualTo(2L);
        assertThat(reporteDia1.montoTotal()).isEqualByComparingTo("301.50");

        VentasDiaResponse reporteSinVentas = ventaRepository.totalesPorDia(LocalDate.of(2020, 1, 1));
        assertThat(reporteSinVentas.cantidadVentas()).isZero();
        assertThat(reporteSinVentas.montoTotal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("findTopByOrderByTotalDesc devuelve empty sin datos y la venta mayor cuando existen ventas")
    void findTopByOrderByTotalDesc() {
        assertThat(ventaRepository.findTopByOrderByTotalDesc()).isEmpty();

        Cliente ana = clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        Producto taza = productoRepository.save(producto(null, "Taza", "100.00", 20));
        ventaRepository.save(venta(null, ana, LocalDate.of(2026, 3, 7), detalle(taza, 1)));
        Venta mayor = ventaRepository.save(venta(null, ana, LocalDate.of(2026, 3, 8), detalle(taza, 4)));
        entityManager.flush();

        assertThat(ventaRepository.findTopByOrderByTotalDesc())
                .isPresent()
                .get()
                .satisfies(v -> {
                    assertThat(v.getCodigoVenta()).isEqualTo(mayor.getCodigoVenta());
                    assertThat(v.getTotal()).isEqualByComparingTo("400.00");
                });
    }

    @Test
    @DisplayName("existsByClienteIdCliente y existsByDetallesProductoCodigoProducto detectan relaciones en uso")
    void existsByRelaciones() {
        Cliente conVentas = clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        Cliente sinVentas = clienteRepository.save(cliente(null, "Luis", "Gomez", "28999888"));
        Producto vendido = productoRepository.save(producto(null, "Taza", "100.00", 10));
        Producto sinVender = productoRepository.save(producto(null, "Plato", "50.00", 10));
        ventaRepository.save(venta(null, conVentas, LocalDate.of(2026, 3, 7), detalle(vendido, 1)));
        entityManager.flush();

        assertThat(ventaRepository.existsByClienteIdCliente(conVentas.getIdCliente())).isTrue();
        assertThat(ventaRepository.existsByClienteIdCliente(sinVentas.getIdCliente())).isFalse();
        assertThat(ventaRepository.existsByDetallesProductoCodigoProducto(vendido.getCodigoProducto())).isTrue();
        assertThat(ventaRepository.existsByDetallesProductoCodigoProducto(sinVender.getCodigoProducto())).isFalse();
    }

    @Test
    @DisplayName("orphanRemoval y cascade eliminan detalles huérfanos y borran detalles al borrar la venta")
    void cascadaYOrphanRemoval() {
        Cliente ana = clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        Producto taza = productoRepository.save(producto(null, "Taza", "100.00", 10));
        Producto plato = productoRepository.save(producto(null, "Plato", "50.00", 10));
        Venta v = ventaRepository.save(venta(null, ana, LocalDate.of(2026, 3, 7),
                detalle(taza, 1), detalle(plato, 2)));
        entityManager.flush();
        Long ventaId = v.getCodigoVenta();

        assertThat(contarDetalles(ventaId)).isEqualTo(2);

        // Quitar un detalle de la colección debe borrar esa fila (orphanRemoval = true)
        v.getDetalles().remove(0);
        entityManager.flush();
        assertThat(contarDetalles(ventaId)).isEqualTo(1);

        // Eliminar la venta debe borrar los detalles restantes en cascada
        ventaRepository.delete(v);
        entityManager.flush();
        assertThat(contarDetalles(ventaId)).isZero();
    }

    private int contarDetalles(Long ventaId) {
        Integer conteo = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detalle_venta WHERE codigo_venta = ?", Integer.class, ventaId);
        return conteo != null ? conteo : 0;
    }
}
