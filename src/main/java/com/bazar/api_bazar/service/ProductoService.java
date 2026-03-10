package com.bazar.api_bazar.service;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService implements IProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public void saveProducto(Producto producto) {
        productoRepository.save(producto);
    }

    @Override
    public List<Producto> getProductos() {
        return productoRepository.findAll();
    }

    @Override
    public Producto getProducto(Long codigo_producto) {
        return productoRepository.findById(codigo_producto).orElse(null);
    }

    @Override
    public void deleteProducto(Long codigo_producto) {
        productoRepository.deleteById(codigo_producto);
    }

    @Override
    public void editProducto(Producto producto) {
        this.saveProducto(producto);
    }

    //requerimienot 5
    @Override
    public List<Producto> getProductosFaltaStock() {
        return productoRepository.findByCantidadDisponibleLessThan(5.0);
    }
}
