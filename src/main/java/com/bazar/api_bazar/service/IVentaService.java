package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.request.VentaRequest;
import com.bazar.api_bazar.dto.response.DetalleVentaResponse;
import com.bazar.api_bazar.dto.response.MayorVentaResponse;
import com.bazar.api_bazar.dto.response.VentaResponse;
import com.bazar.api_bazar.dto.response.VentasDiaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface IVentaService {

    VentaResponse crear(VentaRequest request);

    Page<VentaResponse> listar(Pageable pageable);

    VentaResponse obtener(Long codigoVenta);

    VentaResponse actualizar(Long codigoVenta, VentaRequest request);

    /** Anula la venta y devuelve las unidades al stock. */
    void eliminar(Long codigoVenta);

    /** Requerimiento 5: productos de una venta. */
    List<DetalleVentaResponse> listarProductos(Long codigoVenta);

    /** Requerimiento 6: monto total y cantidad de ventas de un día. */
    VentasDiaResponse totalesPorDia(LocalDate fecha);

    /** Requerimiento 7: venta de mayor monto. */
    MayorVentaResponse mayorVenta();
}
