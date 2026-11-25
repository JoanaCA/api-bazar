-- Esquema inicial de api-bazar (coincide con las entidades JPA).
-- Los nombres de columnas siguen la convención de Spring Boot: camelCase -> snake_case.

CREATE TABLE cliente (
    id_cliente BIGINT       NOT NULL AUTO_INCREMENT,
    nombre     VARCHAR(100) NOT NULL,
    apellido   VARCHAR(100) NOT NULL,
    dni        VARCHAR(15)  NOT NULL,
    PRIMARY KEY (id_cliente),
    CONSTRAINT uk_cliente_dni UNIQUE (dni)
) ENGINE = InnoDB;

CREATE TABLE producto (
    codigo_producto     BIGINT         NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(100)   NOT NULL,
    marca               VARCHAR(100)   NOT NULL,
    costo               DECIMAL(12, 2) NOT NULL,
    cantidad_disponible INT            NOT NULL,
    version             BIGINT         NOT NULL,
    PRIMARY KEY (codigo_producto),
    -- Defensa en profundidad: aunque falle la logica de la aplicacion, la base no admite stock ni costo negativos.
    CONSTRAINT ck_producto_costo CHECK (costo >= 0),
    CONSTRAINT ck_producto_stock CHECK (cantidad_disponible >= 0)
) ENGINE = InnoDB;

CREATE TABLE venta (
    codigo_venta BIGINT         NOT NULL AUTO_INCREMENT,
    fecha_venta  DATE           NOT NULL,
    total        DECIMAL(14, 2) NOT NULL,
    id_cliente   BIGINT         NOT NULL,
    PRIMARY KEY (codigo_venta),
    CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    -- Acelera el reporte de ventas por dia.
    INDEX idx_venta_fecha (fecha_venta)
) ENGINE = InnoDB;

CREATE TABLE detalle_venta (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    codigo_venta    BIGINT         NOT NULL,
    codigo_producto BIGINT         NOT NULL,
    cantidad        INT            NOT NULL,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_venta FOREIGN KEY (codigo_venta) REFERENCES venta (codigo_venta),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (codigo_producto) REFERENCES producto (codigo_producto),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0)
) ENGINE = InnoDB;
