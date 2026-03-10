package com.bazar.api_bazar.controller;

import com.bazar.api_bazar.dto.MayorVentaDTO;
import com.bazar.api_bazar.dto.VentasDiaDTO;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;
import com.bazar.api_bazar.service.IVentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class VentaController {

    @Autowired
    private IVentaService ventaService;

    // Crear una venta
    @PostMapping("/ventas/crear")
    public ResponseEntity<String> crearVenta(@RequestBody Venta venta) {
        ventaService.saveVenta(venta);
        return new ResponseEntity<>("Venta creada correctamente", HttpStatus.CREATED);
    }

    // Traer lista completa
    @GetMapping("/ventas")
    public ResponseEntity<List<Venta>> traerVentas() {
        List<Venta> ventas = ventaService.getVentas();
        return new ResponseEntity<>(ventas, HttpStatus.OK);
    }

    // traer una venta en particular
    @GetMapping("/ventas/{codigo_venta}")
    public ResponseEntity<Venta> traerVenta(@PathVariable Long codigo_venta) {
        Venta venta = ventaService.getVenta(codigo_venta);
        return new ResponseEntity<>(venta, HttpStatus.OK);
    }

    // Eliminar venta
    @DeleteMapping("/ventas/eliminar/{codigo_venta}")
    public ResponseEntity<String> eliminarVenta(@PathVariable Long codigo_venta) {
        ventaService.deleteVenta(codigo_venta);
        return new ResponseEntity<>("Venta eliminada correctamente", HttpStatus.OK);
    }

    // Editar venta
    @PutMapping("/ventas/editar/{codigo_venta}")
    public ResponseEntity<Venta> editarVenta(@PathVariable Long codigo_venta, @RequestBody Venta ventaEditada) {
        ventaEditada.setCodigoVenta(codigo_venta);
        ventaService.editVenta(ventaEditada);

        Venta ventaActualizada = ventaService.getVenta(codigo_venta);
        return new ResponseEntity<>(ventaActualizada, HttpStatus.OK);
    }

    // Requerimiento 5 Obtener la lista de productos de una determinada venta
    @GetMapping("/ventas/productos/{codigo_venta}")
    public ResponseEntity<List<Producto>> traerProductosDeVenta(@PathVariable Long codigo_venta) {
        List<Producto> productos = ventaService.getProductosByVenta(codigo_venta);
        return new ResponseEntity<>(productos, HttpStatus.OK);
    }

    //requerimiento 6 - Obtener la sumatoria del monto y también cantidad total de ventas de un determinado día
    @GetMapping("/ventas/dia/{fecha_venta}")
    public ResponseEntity<VentasDiaDTO> getTotalesPorFecha(@PathVariable LocalDate fecha_venta) {
        VentasDiaDTO totales = ventaService.getTotalesPorDia(fecha_venta);
        return new ResponseEntity<>(totales, HttpStatus.OK);
    }

    //requerimiento 7 - Obtener el codigo_venta, el total, la cantidad de productos, el nombre del cliente y el
    //apellido del cliente de la venta con el monto más alto de todas.
    @GetMapping("/ventas/mayor_venta")
    public ResponseEntity<MayorVentaDTO> getMayorVenta() {
        MayorVentaDTO mayorVenta = ventaService.getMayorVenta();

        if (mayorVenta != null) {
            return new ResponseEntity<>(mayorVenta, HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
