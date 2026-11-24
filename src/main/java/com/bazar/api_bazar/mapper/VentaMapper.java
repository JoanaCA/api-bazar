package com.bazar.api_bazar.mapper;

import com.bazar.api_bazar.dto.response.DetalleVentaResponse;
import com.bazar.api_bazar.dto.response.VentaResponse;
import com.bazar.api_bazar.model.DetalleVenta;
import com.bazar.api_bazar.model.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VentaMapper {

    @Mapping(target = "clienteId", source = "cliente.idCliente")
    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "clienteApellido", source = "cliente.apellido")
    VentaResponse toResponse(Venta venta);

    @Mapping(target = "productoId", source = "producto.codigoProducto")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    DetalleVentaResponse toResponse(DetalleVenta detalle);

    List<DetalleVentaResponse> toDetallesResponse(List<DetalleVenta> detalles);
}
