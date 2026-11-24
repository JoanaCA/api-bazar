package com.bazar.api_bazar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propiedades propias de la aplicación (prefijo {@code bazar}).
 *
 * @param stock configuración de stock
 */
@ConfigurationProperties(prefix = "bazar")
public record BazarProperties(@DefaultValue Stock stock) {

    /**
     * @param minimo cantidad por debajo de la cual un producto se considera con "stock bajo"
     */
    public record Stock(@DefaultValue("5") int minimo) {
    }
}
