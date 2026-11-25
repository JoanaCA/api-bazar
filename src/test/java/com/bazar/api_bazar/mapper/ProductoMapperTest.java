package com.bazar.api_bazar.mapper;

import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.response.ProductoResponse;
import com.bazar.api_bazar.model.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.bazar.api_bazar.support.TestData.producto;
import static com.bazar.api_bazar.support.TestData.productoRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductoMapper")
class ProductoMapperTest {

    private final ProductoMapper mapper = new ProductoMapperImpl();

    @Test
    @DisplayName("toEntity copia los datos del request y deja sin id ni versión")
    void toEntity_copiaCamposYNoAsignaIdNiVersion() {
        Producto entidad = mapper.toEntity(productoRequest());

        assertThat(entidad.getNombre()).isEqualTo("Taza");
        assertThat(entidad.getMarca()).isEqualTo("Acme");
        assertThat(entidad.getCosto()).isEqualByComparingTo("100.50");
        assertThat(entidad.getCantidadDisponible()).isEqualTo(10);
        assertThat(entidad.getCodigoProducto()).isNull();
        assertThat(entidad.getVersion()).isNull();
    }

    @Test
    @DisplayName("toResponse expone el código y los datos del producto")
    void toResponse_copiaTodosLosCampos() {
        ProductoResponse respuesta = mapper.toResponse(producto(5L, "Plato", "50.25", 8));

        assertThat(respuesta.codigoProducto()).isEqualTo(5L);
        assertThat(respuesta.nombre()).isEqualTo("Plato");
        assertThat(respuesta.marca()).isEqualTo("Acme");
        assertThat(respuesta.costo()).isEqualByComparingTo("50.25");
        assertThat(respuesta.cantidadDisponible()).isEqualTo(8);
    }

    @Test
    @DisplayName("actualizar modifica los datos pero nunca el id ni la versión")
    void actualizar_noPisaIdNiVersion() {
        Producto existente = producto(7L, "Viejo", "1.00", 1);
        existente.setVersion(3L);
        ProductoRequest nuevo = new ProductoRequest("Nuevo", "Otra", new BigDecimal("2.50"), 20);

        mapper.actualizar(nuevo, existente);

        assertThat(existente.getNombre()).isEqualTo("Nuevo");
        assertThat(existente.getMarca()).isEqualTo("Otra");
        assertThat(existente.getCosto()).isEqualByComparingTo("2.50");
        assertThat(existente.getCantidadDisponible()).isEqualTo(20);
        assertThat(existente.getCodigoProducto()).isEqualTo(7L);
        assertThat(existente.getVersion()).isEqualTo(3L);
    }
}
