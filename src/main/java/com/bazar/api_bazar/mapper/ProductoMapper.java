package com.bazar.api_bazar.mapper;

import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.response.ProductoResponse;
import com.bazar.api_bazar.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "codigoProducto", ignore = true)
    @Mapping(target = "version", ignore = true)
    Producto toEntity(ProductoRequest request);

    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "codigoProducto", ignore = true)
    @Mapping(target = "version", ignore = true)
    void actualizar(ProductoRequest request, @MappingTarget Producto producto);
}
