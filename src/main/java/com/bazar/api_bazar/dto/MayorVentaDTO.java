package com.bazar.api_bazar.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MayorVentaDTO implements Serializable {

    private Long codigo_venta;
    private Double total;
    private Integer cantidad_productos;
    private String nombre_cliente;
    private String apellido_cliente;
}
