package com.bazar.api_bazar.service;

import com.bazar.api_bazar.dto.MayorVentaDTO;
import com.bazar.api_bazar.dto.VentasDiaDTO;
import com.bazar.api_bazar.model.Producto;
import com.bazar.api_bazar.model.Venta;
import com.bazar.api_bazar.repository.ProductoRepository;
import com.bazar.api_bazar.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class VentaService implements IVentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;


    @Override
    public void saveVenta(Venta venta) {
        //ventaRepository.save(venta);
        Double totalCalculado = 0.0;

        for (Producto productoItem : venta.getListaProductos()) {

            Producto productoEnBD = productoRepository.findById(productoItem.getCodigo_producto()).orElse(null);

            if (productoEnBD != null) {
                totalCalculado += productoEnBD.getCosto();
            }
        }

        venta.setTotal(totalCalculado);

        ventaRepository.save(venta);
    }

    @Override
    public List<Venta> getVentas() {
        return ventaRepository.findAll();
    }

    @Override
    public Venta getVenta(Long codigo_venta) {
        return ventaRepository.findById(codigo_venta).orElse(null);
    }

    @Override
    public void deleteVenta(Long codigo_venta) {
        ventaRepository.deleteById(codigo_venta);
    }

    @Override
    public void editVenta(Venta venta) {
        this.saveVenta(venta);
    }

    //requerimiento 5
    @Override
    public List<Producto> getProductosByVenta(Long codigo_venta) {
        Venta venta = this.getVenta(codigo_venta);
        return venta.getListaProductos();
    }

    // requerimiento 6
    @Override
    public VentasDiaDTO getTotalesPorDia(LocalDate fecha_venta) {
        List<Venta> ventasDelDia = ventaRepository.findByFechaVenta(fecha_venta);

        Double montoTotal = 0.0;
        for (Venta venta : ventasDelDia) {
            montoTotal += venta.getTotal();
        }

        Integer cantidadVentas = ventasDelDia.size();

        return new VentasDiaDTO(montoTotal, cantidadVentas);
    }

    //requerimitno 7 - Obtener el codigo_venta, el total, la cantidad de productos, el nombre del cliente y el
    //apellido del cliente de la venta con el monto más alto de todas.
    @Override
    public MayorVentaDTO getMayorVenta() {
        Optional<Venta> ventaOptional = ventaRepository.findTopByOrderByTotalDesc();

        if (ventaOptional.isPresent()) {
            Venta venta = ventaOptional.get();

            MayorVentaDTO mayorVentaDTO = new MayorVentaDTO();
            mayorVentaDTO.setCodigo_venta(venta.getCodigoVenta());
            mayorVentaDTO.setTotal(venta.getTotal());
            mayorVentaDTO.setCantidad_productos(venta.getListaProductos().size());
            mayorVentaDTO.setNombre_cliente(venta.getUnCliente().getNombre());
            mayorVentaDTO.setApellido_cliente(venta.getUnCliente().getApellido());

            return mayorVentaDTO;
        }

        return null;
    }

}
