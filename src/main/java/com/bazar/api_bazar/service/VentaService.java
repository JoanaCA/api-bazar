package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.request.ItemVentaRequest;
import com.bazar.api_bazar.dto.request.VentaRequest;
import com.bazar.api_bazar.dto.response.DetalleVentaResponse;
import com.bazar.api_bazar.dto.response.MayorVentaResponse;
import com.bazar.api_bazar.dto.response.VentaResponse;
import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.StockInsuficienteException;
import com.bazar.api_bazar.mapper.VentaMapper;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.model.DetalleVenta;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;
import com.bazar.api_bazar.repository.ClienteRepository;
import com.bazar.api_bazar.repository.ProductoRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaService implements IVentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final VentaMapper ventaMapper;

    @Override
    @Transactional
    public VentaResponse crear(VentaRequest request) {
        Venta venta = new Venta();
        venta.setCliente(buscarCliente(request.clienteId()));
        venta.setFechaVenta(request.fechaVenta() != null ? request.fechaVenta() : LocalDate.now());
        aplicarItems(venta, request.items());
        return ventaMapper.toResponse(ventaRepository.save(venta));
    }

    @Override
    public Page<VentaResponse> listar(Pageable pageable) {
        return ventaRepository.findAll(pageable).map(ventaMapper::toResponse);
    }

    @Override
    public VentaResponse obtener(Long codigoVenta) {
        return ventaMapper.toResponse(buscar(codigoVenta));
    }

    @Override
    @Transactional
    public VentaResponse actualizar(Long codigoVenta, VentaRequest request) {
        Venta venta = buscar(codigoVenta);

        // Se deshace el efecto de la venta original antes de aplicar la nueva.
        venta.restituirStock();
        venta.getDetalles().clear();

        venta.setCliente(buscarCliente(request.clienteId()));
        if (request.fechaVenta() != null) {
            venta.setFechaVenta(request.fechaVenta());
        }
        aplicarItems(venta, request.items());
        return ventaMapper.toResponse(venta);
    }

    @Override
    @Transactional
    public void eliminar(Long codigoVenta) {
        Venta venta = buscar(codigoVenta);
        venta.restituirStock();
        ventaRepository.delete(venta);
    }

    @Override
    public List<DetalleVentaResponse> listarProductos(Long codigoVenta) {
        return ventaMapper.toDetallesResponse(buscar(codigoVenta).getDetalles());
    }

    @Override
    public VentasDiaResponse totalesPorDia(LocalDate fecha) {
        return ventaRepository.totalesPorDia(fecha);
    }

    @Override
    public MayorVentaResponse mayorVenta() {
        Venta venta = ventaRepository.findTopByOrderByTotalDesc()
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ventas registradas"));

        int unidades = venta.getDetalles().stream().mapToInt(DetalleVenta::getCantidad).sum();
        return new MayorVentaResponse(
                venta.getCodigoVenta(),
                venta.getTotal(),
                unidades,
                venta.getCliente().getNombre(),
                venta.getCliente().getApellido());
    }

    /**
     * Valida existencia y stock de cada producto, descuenta el stock, copia el precio vigente
     * y calcula el total. Si algo falla se lanza una excepción y la transacción se revierte completa.
     */
    private void aplicarItems(Venta venta, List<ItemVentaRequest> items) {
        // Si el mismo producto viene repetido se suman las cantidades.
        Map<Long, Integer> cantidadesPorProducto = new LinkedHashMap<>();
        items.forEach(item -> cantidadesPorProducto.merge(item.productoId(), item.cantidad(), Integer::sum));

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entrada : cantidadesPorProducto.entrySet()) {
            Producto producto = productoRepository.findById(entrada.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException("producto", entrada.getKey()));

            int solicitado = entrada.getValue();
            if (producto.getCantidadDisponible() < solicitado) {
                throw new StockInsuficienteException(
                        producto.getNombre(), producto.getCantidadDisponible(), solicitado);
            }
            producto.setCantidadDisponible(producto.getCantidadDisponible() - solicitado);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(solicitado);
            detalle.setPrecioUnitario(producto.getCosto());
            venta.getDetalles().add(detalle);

            total = total.add(detalle.getSubtotal());
        }
        venta.setTotal(total);
    }

    private Venta buscar(Long codigoVenta) {
        return ventaRepository.findById(codigoVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("venta", codigoVenta));
    }

    private Cliente buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("cliente", idCliente));
    }
}
