package com.bazar.api_bazar.mapper;

import com.bazar.api_bazar.dto.response.DetalleVentaResponse;
import com.bazar.api_bazar.dto.response.VentaResponse;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.bazar.api_bazar.support.TestData.cliente;
import static com.bazar.api_bazar.support.TestData.detalle;
import static com.bazar.api_bazar.support.TestData.producto;
import static com.bazar.api_bazar.support.TestData.venta;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VentaMapper")
class VentaMapperTest {

    private final VentaMapper mapper = new VentaMapperImpl();

    @Test
    @DisplayName("toResponse incluye los datos del cliente y los detalles con su subtotal")
    void toResponse_incluyeClienteYDetalles() {
        Producto taza = producto(10L, "Taza", "100.50", 10);
        Producto plato = producto(11L, "Plato", "50.00", 10);
        Venta venta = venta(1L, cliente(2L, "Ana", "Perez", "30123456"), LocalDate.of(2026, 1, 15),
                detalle(taza, 3), detalle(plato, 2));

        VentaResponse respuesta = mapper.toResponse(venta);

        assertThat(respuesta.codigoVenta()).isEqualTo(1L);
        assertThat(respuesta.fechaVenta()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(respuesta.total()).isEqualByComparingTo("401.50");
        assertThat(respuesta.clienteId()).isEqualTo(2L);
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana");
        assertThat(respuesta.clienteApellido()).isEqualTo("Perez");
        assertThat(respuesta.detalles()).hasSize(2);

        DetalleVentaResponse primero = respuesta.detalles().get(0);
        assertThat(primero.productoId()).isEqualTo(10L);
        assertThat(primero.productoNombre()).isEqualTo("Taza");
        assertThat(primero.cantidad()).isEqualTo(3);
        assertThat(primero.precioUnitario()).isEqualByComparingTo("100.50");
        assertThat(primero.subtotal()).isEqualByComparingTo("301.50");
    }

    @Test
    @DisplayName("toDetallesResponse devuelve una lista vacía si no hay detalles")
    void toDetallesResponse_listaVacia() {
        assertThat(mapper.toDetallesResponse(List.of())).isEmpty();
    }
}
