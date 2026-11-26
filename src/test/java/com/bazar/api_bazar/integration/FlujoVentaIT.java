package com.bazar.api_bazar.integration;

import com.bazar.api_bazar.repository.ClienteRepository;
import com.bazar.api_bazar.repository.ProductoRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import com.bazar.api_bazar.support.AbstractMySqlTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre el flujo principal de la API (HTTP -> service -> MySQL real) para validar que las piezas funcionan juntas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoVentaIT extends AbstractMySqlTest {

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

    @Test
    @DisplayName("El contexto de Spring arranca contra MySQL con el esquema de Flyway validado por Hibernate")
    void contextLoads() {
    }

    @Test
    @DisplayName("Flujo completo: crear, vender, consultar reportes, anular y comprobar el stock")
    void flujoCompletoDeVentas() throws Exception {
        long clienteId = idDe(crear("/api/v1/clientes",
                "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"dni\":\"30123456\"}"), "idCliente");
        long productoId = idDe(crear("/api/v1/productos",
                "{\"nombre\":\"Taza\",\"marca\":\"Acme\",\"costo\":100.50,\"cantidadDisponible\":10}"), "codigoProducto");

        // Venta válida: descuenta stock y calcula el total en el backend
        String ventaJson = "{\"clienteId\":%d,\"fechaVenta\":\"2026-01-15\",\"items\":[{\"productoId\":%d,\"cantidad\":3}]}"
                .formatted(clienteId, productoId);
        MvcResult venta = mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(ventaJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(301.50))
                .andReturn();
        long ventaId = idDe(venta, "codigoVenta");

        mockMvc.perform(get("/api/v1/productos/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(7));

        // Stock insuficiente -> 409
        String excesoJson = "{\"clienteId\":%d,\"items\":[{\"productoId\":%d,\"cantidad\":100}]}"
                .formatted(clienteId, productoId);
        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(excesoJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Stock insuficiente"));

        // Producto inexistente en la venta -> 404
        String inexistenteJson = "{\"clienteId\":%d,\"items\":[{\"productoId\":99999,\"cantidad\":1}]}".formatted(clienteId);
        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(inexistenteJson))
                .andExpect(status().isNotFound());

        // Validación de entrada -> 400 con detalle por campo
        mockMvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"marca\":\"X\",\"costo\":-1,\"cantidadDisponible\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.costo").exists())
                .andExpect(jsonPath("$.errores.cantidadDisponible").exists());

        // Recurso inexistente -> 404
        mockMvc.perform(get("/api/v1/productos/99999")).andExpect(status().isNotFound());

        // Requerimientos 5, 6 y 7
        mockMvc.perform(get("/api/v1/ventas/{id}/productos", ventaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidad").value(3));
        mockMvc.perform(get("/api/v1/ventas/reportes/dia/2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadVentas").value(1))
                .andExpect(jsonPath("$.montoTotal").value(301.50));
        mockMvc.perform(get("/api/v1/ventas/reportes/dia/2020-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadVentas").value(0))
                .andExpect(jsonPath("$.montoTotal").value(0));
        mockMvc.perform(get("/api/v1/ventas/reportes/mayor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadProductos").value(3))
                .andExpect(jsonPath("$.apellidoCliente").value("Perez"));

        // Requerimiento 4: con 7 unidades no está en stock bajo (umbral 5)
        mockMvc.perform(get("/api/v1/productos/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // No se puede borrar un cliente ni un producto con ventas
        mockMvc.perform(delete("/api/v1/clientes/{id}", clienteId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/productos/{id}", productoId)).andExpect(status().isConflict());

        // Anular la venta devuelve el stock
        mockMvc.perform(delete("/api/v1/ventas/{id}", ventaId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/productos/{id}", productoId))
                .andExpect(jsonPath("$.cantidadDisponible").value(10));
    }

    @Test
    @DisplayName("Editar venta reajusta el stock, actualiza stock-bajo y revierte si falta stock")
    void editarVenta_reajustaStockYSiFallaRevierte() throws Exception {
        long clienteId = idDe(crear("/api/v1/clientes",
                "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"dni\":\"30123456\"}"), "idCliente");
        long productoId = idDe(crear("/api/v1/productos",
                "{\"nombre\":\"Taza\",\"marca\":\"Acme\",\"costo\":100.00,\"cantidadDisponible\":10}"), "codigoProducto");

        long ventaId = idDe(crear("/api/v1/ventas",
                "{\"clienteId\":%d,\"fechaVenta\":\"2026-01-15\",\"items\":[{\"productoId\":%d,\"cantidad\":3}]}"
                        .formatted(clienteId, productoId)), "codigoVenta");

        // Editar de 3 a 6 unidades -> stock pasa de 7 a 4 (entra en stock bajo < 5)
        mockMvc.perform(put("/api/v1/ventas/{id}", ventaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":%d,\"items\":[{\"productoId\":%d,\"cantidad\":6}]}"
                                .formatted(clienteId, productoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(600.00))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(6));

        mockMvc.perform(get("/api/v1/productos/{id}", productoId))
                .andExpect(jsonPath("$.cantidadDisponible").value(4));
        mockMvc.perform(get("/api/v1/productos/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].codigoProducto").value(productoId));

        // Intentar editar a 11 (solo hay 4 + 6 = 10): debe devolver 409 y no alterar ni stock ni venta
        mockMvc.perform(put("/api/v1/ventas/{id}", ventaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":%d,\"items\":[{\"productoId\":%d,\"cantidad\":11}]}"
                                .formatted(clienteId, productoId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/productos/{id}", productoId))
                .andExpect(jsonPath("$.cantidadDisponible").value(4));
        mockMvc.perform(get("/api/v1/ventas/{id}", ventaId))
                .andExpect(jsonPath("$.total").value(600.00))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(6));
    }

    @Test
    @DisplayName("Si falla el stock del segundo producto en una venta, se revierte el descuento del primero")
    void crearVenta_siFallaSegundoProductoRevierteElPrimero() throws Exception {
        long clienteId = idDe(crear("/api/v1/clientes",
                "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"dni\":\"30123456\"}"), "idCliente");
        long prodA = idDe(crear("/api/v1/productos",
                "{\"nombre\":\"Taza\",\"marca\":\"Acme\",\"costo\":100.00,\"cantidadDisponible\":10}"), "codigoProducto");
        long prodB = idDe(crear("/api/v1/productos",
                "{\"nombre\":\"Plato\",\"marca\":\"Acme\",\"costo\":50.00,\"cantidadDisponible\":1}"), "codigoProducto");

        String json = "{\"clienteId\":%d,\"items\":[{\"productoId\":%d,\"cantidad\":3},{\"productoId\":%d,\"cantidad\":5}]}"
                .formatted(clienteId, prodA, prodB);

        mockMvc.perform(post("/api/v1/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());

        assertThat(ventaRepository.count()).isZero();
        assertThat(productoRepository.findById(prodA).orElseThrow().getCantidadDisponible()).isEqualTo(10);
        assertThat(productoRepository.findById(prodB).orElseThrow().getCantidadDisponible()).isEqualTo(1);
    }

    @Test
    @DisplayName("Editar cliente con el DNI de otro devuelve 409 y conserva el DNI original")
    void editarCliente_dniDuplicado_devuelve409() throws Exception {
        crear("/api/v1/clientes", "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"dni\":\"30123456\"}");
        long luisId = idDe(crear("/api/v1/clientes",
                "{\"nombre\":\"Luis\",\"apellido\":\"Gomez\",\"dni\":\"28999888\"}"), "idCliente");

        mockMvc.perform(put("/api/v1/clientes/{id}", luisId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luis\",\"apellido\":\"Gomez\",\"dni\":\"30123456\"}"))
                .andExpect(status().isConflict());

        assertThat(clienteRepository.findById(luisId).orElseThrow().getDni()).isEqualTo("28999888");
    }

    private MvcResult crear(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private long idDe(MvcResult resultado, String campo) throws Exception {
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$." + campo);
        return id.longValue();
    }
}
