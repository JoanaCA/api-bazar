package com.bazar.api_bazar.service;
import com.bazar.api_bazar.model.Producto;
import java.util.List;

public interface IProductoService {

    void saveProducto(Producto producto);
    List<Producto> getProductos();
    Producto getProducto(Long codigo_producto);
    void deleteProducto(Long codigo_producto);
    void editProducto(Producto producto);
    List<Producto> getProductosFaltaStock();
}
