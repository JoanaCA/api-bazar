package com.bazar.api_bazar.controller;

import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.service.IProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductoController {

    @Autowired
    private IProductoService productoService;

    // Crear un producto.
    @PostMapping("/productos/crear")
    public ResponseEntity<String> crearProducto(@RequestBody Producto producto) {
        productoService.saveProducto(producto);
        return new ResponseEntity<>("Producto creado correctamente", HttpStatus.CREATED); //201
    }

    // Traer todos los productos.
    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> traerProductos(){
        List <Producto> productos = productoService.getProductos();
        return new ResponseEntity<>(productos, HttpStatus.OK);
    }

    // Traer un producto en particular.
    @GetMapping("/productos/{codigo_producto}")
    public ResponseEntity<Producto> traerProducto(@PathVariable Long codigo_producto) {
        Producto producto = productoService.getProducto(codigo_producto);
        return new ResponseEntity<>(producto, HttpStatus.OK);
    }

    // Eliminar producto.
    @DeleteMapping("/productos/eliminar/{codigo_producto}")
    public ResponseEntity<String> eliminarProducto(@PathVariable Long codigo_producto) {
        productoService.deleteProducto(codigo_producto);
        return new ResponseEntity<>("Producto eliminado correctamente", HttpStatus.OK);
    }

    // Editar producto.
    @PutMapping("/productos/editar/{codigo_producto}")
    public ResponseEntity<Producto> editarProducto(@PathVariable Long codigo_producto, @RequestBody Producto productoEditado) {
        productoEditado.setCodigo_producto(codigo_producto);
        productoService.editProducto(productoEditado);

        Producto productoActualizado = productoService.getProducto(codigo_producto);
        return new ResponseEntity<>(productoActualizado, HttpStatus.OK);
    }

    // Requerimiento 4 - Obtener todos los productos cuya cantidad_disponible sea menor a 5
    @GetMapping("/productos/falta_stock")
    public ResponseEntity<List<Producto>> traerProductosFaltaStock() {
        List<Producto> productosFaltaStock = productoService.getProductosFaltaStock();
        return new ResponseEntity<>(productosFaltaStock, HttpStatus.OK);
    }
}
