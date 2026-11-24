package com.bazar.api_bazar.service;

import com.bazar.api_bazar.config.BazarProperties;
import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.response.ProductoResponse;
import com.bazar.api_bazar.exception.RecursoNoEncontradoException;
import com.bazar.api_bazar.exception.ReglaDeNegocioException;
import com.bazar.api_bazar.mapper.ProductoMapper;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.repository.ProductoRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService implements IProductoService {

    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final ProductoMapper productoMapper;
    private final BazarProperties properties;

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = productoMapper.toEntity(request);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    public Page<ProductoResponse> listar(Pageable pageable) {
        return productoRepository.findAll(pageable).map(productoMapper::toResponse);
    }

    @Override
    public ProductoResponse obtener(Long codigoProducto) {
        return productoMapper.toResponse(buscar(codigoProducto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long codigoProducto, ProductoRequest request) {
        Producto producto = buscar(codigoProducto);
        productoMapper.actualizar(request, producto);
        return productoMapper.toResponse(producto);
    }

    @Override
    @Transactional
    public void eliminar(Long codigoProducto) {
        Producto producto = buscar(codigoProducto);
        if (ventaRepository.existsByDetallesProductoCodigoProducto(codigoProducto)) {
            throw new ReglaDeNegocioException(
                    "No se puede eliminar el producto %d porque forma parte de ventas registradas"
                            .formatted(codigoProducto));
        }
        productoRepository.delete(producto);
    }

    @Override
    public Page<ProductoResponse> listarStockBajo(Pageable pageable) {
        return productoRepository
                .findByCantidadDisponibleLessThan(properties.stock().minimo(), pageable)
                .map(productoMapper::toResponse);
    }

    private Producto buscar(Long codigoProducto) {
        return productoRepository.findById(codigoProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("producto", codigoProducto));
    }
}
