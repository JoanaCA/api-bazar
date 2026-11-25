package com.bazar.api_bazar.service;

import com.bazar.api_bazar.config.BazarProperties;
import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.response.ProductoResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.ReglaDeNegocioException;
import com.bazar.api_bazar.mapper.ProductoMapperImpl;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.repository.ProductoRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;
import java.util.Optional;

import static com.bazar.api_bazar.support.TestData.producto;
import static com.bazar.api_bazar.support.TestData.productoRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService")
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private VentaRepository ventaRepository;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        service = crearService(5);
    }

    private ProductoService crearService(int stockMinimo) {
        return new ProductoService(productoRepository, ventaRepository, new ProductoMapperImpl(),
                new BazarProperties(new BazarProperties.Stock(stockMinimo)));
    }

    @Test
    @DisplayName("crear guarda el producto y devuelve el código asignado")
    void crear_guardaYDevuelveElCodigo() {
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto guardado = invocacion.getArgument(0);
            guardado.setCodigoProducto(1L);
            return guardado;
        });

        ProductoResponse respuesta = service.crear(productoRequest());

        assertThat(respuesta.codigoProducto()).isEqualTo(1L);
        assertThat(respuesta.nombre()).isEqualTo("Taza");
        assertThat(respuesta.costo()).isEqualByComparingTo("100.50");
        assertThat(respuesta.cantidadDisponible()).isEqualTo(10);
    }

    @Test
    @DisplayName("obtener devuelve el producto")
    void obtener_devuelveElProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Taza", "100.50", 10)));

        assertThat(service.obtener(1L).nombre()).isEqualTo("Taza");
    }

    @Test
    @DisplayName("obtener un producto inexistente lanza RecursoNoEncontradoException")
    void obtener_inexistente_lanzaNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("producto")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("listar devuelve una página de productos mapeados")
    void listar_devuelveLaPagina() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productoRepository.findAll(pageable)).thenReturn(
                new PageImpl<>(List.of(producto(1L, "Taza", "100.50", 10), producto(2L, "Plato", "50.00", 3)),
                        pageable, 2));

        Page<ProductoResponse> pagina = service.listar(pageable);

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(ProductoResponse::nombre).containsExactly("Taza", "Plato");
    }

    @Test
    @DisplayName("actualizar modifica los datos del producto existente")
    void actualizar_modificaLosDatos() {
        Producto existente = producto(1L, "Viejo", "1.00", 1);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(existente));

        ProductoResponse respuesta = service.actualizar(1L,
                new ProductoRequest("Nuevo", "Otra", new BigDecimal("2.50"), 20));

        assertThat(respuesta.codigoProducto()).isEqualTo(1L);
        assertThat(respuesta.nombre()).isEqualTo("Nuevo");
        assertThat(existente.getCantidadDisponible()).isEqualTo(20);
        assertThat(existente.getCosto()).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("actualizar un producto inexistente lanza RecursoNoEncontradoException")
    void actualizar_inexistente_lanzaNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, productoRequest()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("eliminar un producto sin ventas lo borra")
    void eliminar_sinVentas_loBorra() {
        Producto existente = producto(1L, "Taza", "100.50", 10);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(ventaRepository.existsByDetallesProductoCodigoProducto(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(productoRepository).delete(existente);
    }

    @Test
    @DisplayName("eliminar un producto que forma parte de ventas lanza ReglaDeNegocioException y no lo borra")
    void eliminar_conVentas_lanzaReglaDeNegocio() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Taza", "100.50", 10)));
        when(ventaRepository.existsByDetallesProductoCodigoProducto(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ventas registradas");

        verify(productoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("eliminar un producto inexistente lanza RecursoNoEncontradoException")
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);

        verify(productoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("listarStockBajo consulta con el umbral configurado (requerimiento 4)")
    void listarStockBajo_usaElUmbralConfigurado() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductoService conUmbral8 = crearService(8);
        when(productoRepository.findByCantidadDisponibleLessThan(eq(8), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(producto(2L, "Plato", "50.00", 3)), pageable, 1));

        Page<ProductoResponse> pagina = conUmbral8.listarStockBajo(pageable);

        ArgumentCaptor<Integer> umbral = ArgumentCaptor.forClass(Integer.class);
        verify(productoRepository).findByCantidadDisponibleLessThan(umbral.capture(), eq(pageable));
        assertThat(umbral.getValue()).isEqualTo(8);
        assertThat(pagina.getContent()).singleElement()
                .satisfies(p -> assertThat(p.nombre()).isEqualTo("Plato"));
    }

    @Test
    @DisplayName("listarStockBajo usa el umbral por defecto de 5")
    void listarStockBajo_umbralPorDefecto() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productoRepository.findByCantidadDisponibleLessThan(5, pageable)).thenReturn(Page.empty(pageable));

        assertThat(service.listarStockBajo(pageable).getContent()).isEmpty();
    }
}
