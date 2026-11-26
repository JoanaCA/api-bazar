package com.bazar.api_bazar.integration;

import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.repository.ClienteRepository;
import com.bazar.api_bazar.repository.ProductoRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import com.bazar.api_bazar.support.AbstractMySqlTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.bazar.api_bazar.support.TestData.cliente;
import static com.bazar.api_bazar.support.TestData.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Prueba compras simultáneas reales contra MySQL para demostrar que {@code @Version}
 * y la transacción impiden que el stock quede negativo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Concurrencia en ventas (MySQL real)")
class ConcurrenciaVentaIT extends AbstractMySqlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @BeforeEach
    @AfterEach
    void limpiarBaseDeDatos() {
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @RepeatedTest(2)
    @DisplayName("8 hilos compran la última unidad a la vez: solo 1 se concreta y los otros 7 reciben 409")
    void ultimaUnidad_soloUnaVentaTieneExito() throws Exception {
        Cliente ana = clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        Producto taza = productoRepository.save(producto(null, "Taza", "100.00", 1));

        List<Integer> estados = ejecutarVentasSimultaneas(8, ana.getIdCliente(), taza.getCodigoProducto(), 1);

        assertThat(estados.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(estados.stream().filter(s -> s == 409).count()).isEqualTo(7);
        assertThat(ventaRepository.count()).isEqualTo(1);
        assertThat(productoRepository.findById(taza.getCodigoProducto()).orElseThrow().getCantidadDisponible())
                .isZero();
    }

    @RepeatedTest(2)
    @DisplayName("6 hilos compran de a 2 unidades sobre un stock de 3: solo 1 se concreta y queda 1 unidad")
    void stockParcial_noPermiteVenderMasQueElDisponible() throws Exception {
        Cliente ana = clienteRepository.save(cliente(null, "Ana", "Perez", "30123456"));
        Producto plato = productoRepository.save(producto(null, "Plato", "50.00", 3));

        List<Integer> estados = ejecutarVentasSimultaneas(6, ana.getIdCliente(), plato.getCodigoProducto(), 2);

        assertThat(estados.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(estados.stream().filter(s -> s == 409).count()).isEqualTo(5);
        assertThat(ventaRepository.count()).isEqualTo(1);
        assertThat(productoRepository.findById(plato.getCodigoProducto()).orElseThrow().getCantidadDisponible())
                .isEqualTo(1);
    }

    private List<Integer> ejecutarVentasSimultaneas(int hilos, Long clienteId, Long productoId, int cantidad)
            throws Exception {
        String body = "{\"clienteId\":%d,\"items\":[{\"productoId\":%d,\"cantidad\":%d}]}"
                .formatted(clienteId, productoId, cantidad);

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch disparo = new CountDownLatch(1);

        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    listos.countDown();
                    if (!disparo.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timeout esperando sincronización de hilos");
                    }
                    return mockMvc.perform(post("/api/v1/ventas")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
            disparo.countDown();

            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                estados.add(futuro.get(30, TimeUnit.SECONDS));
            }
            return estados;
        } finally {
            pool.shutdownNow();
        }
    }
}
