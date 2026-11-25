package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.response.DetalleVentaResponse;
import com.bazar.api_bazar.dto.response.MayorVentaResponse;
import com.bazar.api_bazar.dto.response.VentaResponse;
import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.StockInsuficienteException;
import com.bazar.api_bazar.mapper.VentaMapperImpl;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;
import com.bazar.api_bazar.repository.ClienteRepository;
import com.bazar.api_bazar.repository.ProductoRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.bazar.api_bazar.support.TestData.cliente;
import static com.bazar.api_bazar.support.TestData.detalle;
import static com.bazar.api_bazar.support.TestData.item;
import static com.bazar.api_bazar.support.TestData.producto;
import static com.bazar.api_bazar.support.TestData.venta;
import static com.bazar.api_bazar.support.TestData.ventaRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService")
class VentaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 1, 15);

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private VentaService service;
    private Cliente ana;
    private Producto taza;

    @BeforeEach
    void setUp() {
        service = new VentaService(ventaRepository, productoRepository, clienteRepository, new VentaMapperImpl());
        ana = cliente(1L, "Ana", "Perez", "30123456");
        taza = producto(10L, "Taza", "100.50", 10);
    }

    private void clienteExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(ana));
    }

    private void productoExiste(Producto producto) {
        when(productoRepository.findById(producto.getCodigoProducto())).thenReturn(Optional.of(producto));
    }

    private void saveDevuelveLoRecibido() {
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("con stock suficiente descuenta el stock, copia el precio y calcula el total")
        void conStockSuficiente_descuentaStockCopiaPrecioYCalculaTotal() {
            clienteExiste();
            productoExiste(taza);
            saveDevuelveLoRecibido();

            VentaResponse respuesta = service.crear(ventaRequest(1L, FECHA, item(10L, 3)));

            assertThat(respuesta.total()).isEqualByComparingTo("301.50");
            assertThat(respuesta.fechaVenta()).isEqualTo(FECHA);
            assertThat(respuesta.clienteId()).isEqualTo(1L);
            assertThat(respuesta.detalles()).singleElement().satisfies(detalle -> {
                assertThat(detalle.productoId()).isEqualTo(10L);
                assertThat(detalle.cantidad()).isEqualTo(3);
                assertThat(detalle.precioUnitario()).isEqualByComparingTo("100.50");
                assertThat(detalle.subtotal()).isEqualByComparingTo("301.50");
            });
            assertThat(taza.getCantidadDisponible()).isEqualTo(7);
            verify(ventaRepository).save(any(Venta.class));
        }

        @Test
        @DisplayName("con varios productos suma los subtotales y descuenta cada stock")
        void conVariosProductos_sumaElTotal() {
            Producto plato = producto(11L, "Plato", "50.00", 5);
            clienteExiste();
            productoExiste(taza);
            productoExiste(plato);
            saveDevuelveLoRecibido();

            VentaResponse respuesta = service.crear(ventaRequest(1L, FECHA, item(10L, 2), item(11L, 4)));

            assertThat(respuesta.total()).isEqualByComparingTo("401.00");
            assertThat(respuesta.detalles()).hasSize(2);
            assertThat(taza.getCantidadDisponible()).isEqualTo(8);
            assertThat(plato.getCantidadDisponible()).isEqualTo(1);
        }

        @Test
        @DisplayName("si el mismo producto viene repetido suma las cantidades en un solo detalle")
        void conProductoRepetido_sumaLasCantidades() {
            clienteExiste();
            productoExiste(taza);
            saveDevuelveLoRecibido();

            VentaResponse respuesta = service.crear(ventaRequest(1L, FECHA, item(10L, 2), item(10L, 3)));

            assertThat(respuesta.detalles()).singleElement()
                    .satisfies(detalle -> assertThat(detalle.cantidad()).isEqualTo(5));
            assertThat(respuesta.total()).isEqualByComparingTo("502.50");
            assertThat(taza.getCantidadDisponible()).isEqualTo(5);
        }

        @Test
        @DisplayName("permite comprar exactamente todo el stock disponible")
        void pidiendoTodoElStock_dejaElStockEnCero() {
            clienteExiste();
            productoExiste(taza);
            saveDevuelveLoRecibido();

            service.crear(ventaRequest(1L, FECHA, item(10L, 10)));

            assertThat(taza.getCantidadDisponible()).isZero();
        }

        @Test
        @DisplayName("con stock insuficiente lanza StockInsuficienteException y no guarda la venta")
        void conStockInsuficiente_lanzaExcepcionYNoGuarda() {
            clienteExiste();
            productoExiste(taza);

            assertThatThrownBy(() -> service.crear(ventaRequest(1L, FECHA, item(10L, 11))))
                    .isInstanceOf(StockInsuficienteException.class)
                    .hasMessageContaining("Taza")
                    .hasMessageContaining("disponible 10")
                    .hasMessageContaining("solicitado 11");

            assertThat(taza.getCantidadDisponible()).isEqualTo(10);
            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("con un producto inexistente lanza RecursoNoEncontradoException y no guarda")
        void conProductoInexistente_lanzaNoEncontrado() {
            clienteExiste();
            when(productoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crear(ventaRequest(1L, FECHA, item(99L, 1))))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("producto")
                    .hasMessageContaining("99");

            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("con un cliente inexistente lanza RecursoNoEncontradoException sin tocar productos")
        void conClienteInexistente_lanzaNoEncontrado() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crear(ventaRequest(1L, FECHA, item(10L, 1))))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("cliente");

            verifyNoInteractions(productoRepository);
            verify(ventaRepository, never()).save(any());
        }

        @Test
        @DisplayName("sin fecha usa la fecha de hoy")
        void sinFecha_usaLaFechaDeHoy() {
            clienteExiste();
            productoExiste(taza);
            saveDevuelveLoRecibido();
            LocalDate antes = LocalDate.now();

            VentaResponse respuesta = service.crear(ventaRequest(1L, null, item(10L, 1)));

            assertThat(respuesta.fechaVenta()).isBetween(antes, LocalDate.now());
        }

        @Test
        @DisplayName("guarda la venta asociada al cliente y con el total calculado")
        void guardaLaVentaConClienteYTotal() {
            clienteExiste();
            productoExiste(taza);
            saveDevuelveLoRecibido();
            ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);

            service.crear(ventaRequest(1L, FECHA, item(10L, 2)));

            verify(ventaRepository).save(captor.capture());
            Venta guardada = captor.getValue();
            assertThat(guardada.getCliente()).isSameAs(ana);
            assertThat(guardada.getTotal()).isEqualByComparingTo("201.00");
            assertThat(guardada.getDetalles()).singleElement()
                    .satisfies(detalle -> assertThat(detalle.getVenta()).isSameAs(guardada));
        }
    }

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        private Venta ventaOriginal() {
            // Venta original de 3 tazas: el stock ya quedó en 7.
            taza.setCantidadDisponible(7);
            return venta(1L, ana, FECHA, detalle(taza, 3));
        }

        @Test
        @DisplayName("restituye el stock original y aplica la nueva cantidad")
        void restituyeElStockOriginalYAplicaElNuevo() {
            Venta original = ventaOriginal();
            when(ventaRepository.findById(1L)).thenReturn(Optional.of(original));
            clienteExiste();
            productoExiste(taza);

            VentaResponse respuesta = service.actualizar(1L, ventaRequest(1L, FECHA, item(10L, 5)));

            // 7 (actual) + 3 (devueltas) - 5 (nuevas) = 5
            assertThat(taza.getCantidadDisponible()).isEqualTo(5);
            assertThat(respuesta.detalles()).singleElement()
                    .satisfies(detalle -> assertThat(detalle.cantidad()).isEqualTo(5));
            assertThat(respuesta.total()).isEqualByComparingTo("502.50");
        }

        @Test
        @DisplayName("cuenta como disponible el stock que devuelve la propia venta")
        void conStockInsuficiente_incluyeLoQueDevuelveLaVenta() {
            Venta original = ventaOriginal();
            when(ventaRepository.findById(1L)).thenReturn(Optional.of(original));
            clienteExiste();
            productoExiste(taza);

            // Disponible real tras devolver = 10; pedir 11 falla.
            assertThatThrownBy(() -> service.actualizar(1L, ventaRequest(1L, FECHA, item(10L, 11))))
                    .isInstanceOf(StockInsuficienteException.class)
                    .hasMessageContaining("disponible 10");
        }

        @Test
        @DisplayName("sin fecha conserva la fecha original")
        void sinFecha_conservaLaFechaOriginal() {
            Venta original = ventaOriginal();
            when(ventaRepository.findById(1L)).thenReturn(Optional.of(original));
            clienteExiste();
            productoExiste(taza);

            VentaResponse respuesta = service.actualizar(1L, ventaRequest(1L, null, item(10L, 1)));

            assertThat(respuesta.fechaVenta()).isEqualTo(FECHA);
        }

        @Test
        @DisplayName("con una fecha nueva la reemplaza")
        void conFecha_laReemplaza() {
            Venta original = ventaOriginal();
            when(ventaRepository.findById(1L)).thenReturn(Optional.of(original));
            clienteExiste();
            productoExiste(taza);

            VentaResponse respuesta = service.actualizar(1L, ventaRequest(1L, LocalDate.of(2026, 2, 1), item(10L, 1)));

            assertThat(respuesta.fechaVenta()).isEqualTo(LocalDate.of(2026, 2, 1));
        }

        @Test
        @DisplayName("una venta inexistente lanza RecursoNoEncontradoException")
        void ventaInexistente_lanzaNoEncontrado() {
            when(ventaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizar(1L, ventaRequest(1L, FECHA, item(10L, 1))))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("venta");

            verifyNoInteractions(productoRepository, clienteRepository);
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("anula la venta y devuelve las unidades al stock")
        void restituyeElStockYBorra() {
            taza.setCantidadDisponible(7);
            Venta existente = venta(1L, ana, FECHA, detalle(taza, 3));
            when(ventaRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            assertThat(taza.getCantidadDisponible()).isEqualTo(10);
            verify(ventaRepository).delete(existente);
        }

        @Test
        @DisplayName("una venta inexistente lanza RecursoNoEncontradoException y no borra")
        void inexistente_lanzaNoEncontrado() {
            when(ventaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(RecursoNoEncontradoException.class);

            verify(ventaRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("obtener devuelve la venta mapeada")
        void obtener_devuelveLaVenta() {
            when(ventaRepository.findById(1L))
                    .thenReturn(Optional.of(venta(1L, ana, FECHA, detalle(taza, 2))));

            VentaResponse respuesta = service.obtener(1L);

            assertThat(respuesta.codigoVenta()).isEqualTo(1L);
            assertThat(respuesta.total()).isEqualByComparingTo("201.00");
        }

        @Test
        @DisplayName("obtener una venta inexistente lanza RecursoNoEncontradoException")
        void obtener_inexistente() {
            when(ventaRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtener(5L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("5");
        }

        @Test
        @DisplayName("listar devuelve una página de ventas mapeadas")
        void listar_devuelveLaPagina() {
            Pageable pageable = PageRequest.of(0, 10);
            Venta venta = venta(1L, ana, FECHA, detalle(taza, 1));
            when(ventaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(venta), pageable, 1));

            Page<VentaResponse> pagina = service.listar(pageable);

            assertThat(pagina.getTotalElements()).isEqualTo(1);
            assertThat(pagina.getContent()).singleElement()
                    .satisfies(v -> assertThat(v.codigoVenta()).isEqualTo(1L));
        }

        @Test
        @DisplayName("listarProductos devuelve los detalles de la venta (requerimiento 5)")
        void listarProductos_devuelveLosDetalles() {
            Producto plato = producto(11L, "Plato", "50.00", 5);
            when(ventaRepository.findById(1L))
                    .thenReturn(Optional.of(venta(1L, ana, FECHA, detalle(taza, 2), detalle(plato, 1))));

            List<DetalleVentaResponse> detalles = service.listarProductos(1L);

            assertThat(detalles).extracting(DetalleVentaResponse::productoNombre).containsExactly("Taza", "Plato");
        }

        @Test
        @DisplayName("listarProductos de una venta inexistente lanza RecursoNoEncontradoException")
        void listarProductos_ventaInexistente() {
            when(ventaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listarProductos(1L)).isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("totalesPorDia devuelve lo que calcula el repositorio (requerimiento 6)")
        void totalesPorDia_delegaEnElRepositorio() {
            VentasDiaResponse esperado = new VentasDiaResponse(new BigDecimal("500.00"), 3L);
            when(ventaRepository.totalesPorDia(FECHA)).thenReturn(esperado);

            assertThat(service.totalesPorDia(FECHA)).isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("mayorVenta (requerimiento 7)")
    class MayorVenta {

        @Test
        @DisplayName("sin ventas registradas lanza RecursoNoEncontradoException")
        void sinVentas_lanzaNoEncontrado() {
            when(ventaRepository.findTopByOrderByTotalDesc()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.mayorVenta())
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("No hay ventas");
        }

        @Test
        @DisplayName("devuelve el total, las unidades vendidas y el nombre del cliente")
        void conVentas_devuelveElResumen() {
            Producto plato = producto(11L, "Plato", "50.00", 5);
            Venta mayor = venta(7L, ana, FECHA, detalle(taza, 3), detalle(plato, 2));
            when(ventaRepository.findTopByOrderByTotalDesc()).thenReturn(Optional.of(mayor));

            MayorVentaResponse respuesta = service.mayorVenta();

            assertThat(respuesta.codigoVenta()).isEqualTo(7L);
            assertThat(respuesta.total()).isEqualByComparingTo("401.50");
            assertThat(respuesta.cantidadProductos()).isEqualTo(5);
            assertThat(respuesta.nombreCliente()).isEqualTo("Ana");
            assertThat(respuesta.apellidoCliente()).isEqualTo("Perez");
        }
    }
}
