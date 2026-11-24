package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.response.ProductoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProductoService {

    ProductoResponse crear(ProductoRequest request);

    Page<ProductoResponse> listar(Pageable pageable);

    ProductoResponse obtener(Long codigoProducto);

    ProductoResponse actualizar(Long codigoProducto, ProductoRequest request);

    void eliminar(Long codigoProducto);

    /** Requerimiento 4: productos con stock por debajo del mínimo configurado. */
    Page<ProductoResponse> listarStockBajo(Pageable pageable);
}
