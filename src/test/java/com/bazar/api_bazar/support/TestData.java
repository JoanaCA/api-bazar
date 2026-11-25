package com.bazar.api_bazar.support;

import com.bazar.api_bazar.dto.request.ClienteRequest;
import com.bazar.api_bazar.dto.request.ItemVentaRequest;
import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.request.VentaRequest;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.model.DetalleVenta;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fábricas de datos de prueba (patrón Object Mother): evitan repetir el armado de objetos en cada test.
 */
public final class TestData {

    private TestData() {
    }

    public static Producto producto(Long id, String nombre, String costo, int stock) {
        Producto producto = new Producto();
        producto.setCodigoProducto(id);
        producto.setNombre(nombre);
        producto.setMarca("Acme");
        producto.setCosto(new BigDecimal(costo));
        producto.setCantidadDisponible(stock);
        return producto;
    }

    public static Cliente cliente(Long id, String nombre, String apellido, String dni) {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(id);
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setDni(dni);
        return cliente;
    }

    /** Detalle que copia el costo vigente del producto como precio unitario. */
    public static DetalleVenta detalle(Producto producto, int cantidad) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(producto.getCosto());
        return detalle;
    }

    /** Venta ya armada con sus detalles y el total calculado, tal como queda después de registrarse. */
    public static Venta venta(Long codigo, Cliente cliente, LocalDate fecha, DetalleVenta... detalles) {
        Venta venta = new Venta();
        venta.setCodigoVenta(codigo);
        venta.setCliente(cliente);
        venta.setFechaVenta(fecha);
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta detalle : detalles) {
            detalle.setVenta(venta);
            venta.getDetalles().add(detalle);
            total = total.add(detalle.getSubtotal());
        }
        venta.setTotal(total);
        return venta;
    }

    public static ProductoRequest productoRequest() {
        return new ProductoRequest("Taza", "Acme", new BigDecimal("100.50"), 10);
    }

    public static ClienteRequest clienteRequest() {
        return new ClienteRequest("Ana", "Perez", "30123456");
    }

    public static ItemVentaRequest item(Long productoId, Integer cantidad) {
        return new ItemVentaRequest(productoId, cantidad);
    }

    public static VentaRequest ventaRequest(Long clienteId, LocalDate fecha, ItemVentaRequest... items) {
        return new VentaRequest(clienteId, fecha, List.of(items));
    }
}
