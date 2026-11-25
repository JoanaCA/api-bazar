package com.bazar.api_bazar.dto;

import com.bazar.api_bazar.dto.request.ClienteRequest;
import com.bazar.api_bazar.dto.request.ItemVentaRequest;
import com.bazar.api_bazar.dto.request.ProductoRequest;
import com.bazar.api_bazar.dto.request.VentaRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba las reglas de Bean Validation de los DTOs de entrada, sin levantar Spring.
 */
@DisplayName("Validación de DTOs de entrada")
class ValidacionDtoTest {

    private static ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarFabrica() {
        fabrica.close();
    }

    private static <T> Set<String> camposInvalidos(T dto) {
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static String texto(int largo) {
        return "a".repeat(largo);
    }

    @Nested
    @DisplayName("ProductoRequest")
    class ProductoRequestTest {

        @Test
        @DisplayName("Un producto correcto no tiene errores")
        void productoValido() {
            ProductoRequest request = new ProductoRequest("Taza", "Acme", new BigDecimal("100.50"), 10);

            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("Los límites válidos se aceptan: costo 0, stock 0, nombre de 100 caracteres, costo máximo")
        void limitesValidos() {
            assertThat(validator.validate(new ProductoRequest(texto(100), texto(100), BigDecimal.ZERO, 0))).isEmpty();
            assertThat(validator.validate(new ProductoRequest("A", "B", new BigDecimal("9999999999.99"), 1))).isEmpty();
        }

        static Stream<Arguments> casosInvalidos() {
            BigDecimal ok = new BigDecimal("10.00");
            return Stream.of(
                    Arguments.of("nombre nulo", new ProductoRequest(null, "M", ok, 1), "nombre"),
                    Arguments.of("nombre vacío", new ProductoRequest("", "M", ok, 1), "nombre"),
                    Arguments.of("nombre solo espacios", new ProductoRequest("   ", "M", ok, 1), "nombre"),
                    Arguments.of("nombre de 101 caracteres", new ProductoRequest(texto(101), "M", ok, 1), "nombre"),
                    Arguments.of("marca vacía", new ProductoRequest("N", "", ok, 1), "marca"),
                    Arguments.of("marca de 101 caracteres", new ProductoRequest("N", texto(101), ok, 1), "marca"),
                    Arguments.of("costo nulo", new ProductoRequest("N", "M", null, 1), "costo"),
                    Arguments.of("costo negativo", new ProductoRequest("N", "M", new BigDecimal("-0.01"), 1), "costo"),
                    Arguments.of("costo con 3 decimales", new ProductoRequest("N", "M", new BigDecimal("1.234"), 1), "costo"),
                    Arguments.of("costo con 11 enteros", new ProductoRequest("N", "M", new BigDecimal("12345678901"), 1), "costo"),
                    Arguments.of("stock nulo", new ProductoRequest("N", "M", ok, null), "cantidadDisponible"),
                    Arguments.of("stock negativo", new ProductoRequest("N", "M", ok, -1), "cantidadDisponible")
            );
        }

        @ParameterizedTest(name = "{0} -> error en {2}")
        @MethodSource("casosInvalidos")
        @DisplayName("Los datos inválidos se rechazan indicando el campo")
        void datosInvalidos(String caso, ProductoRequest request, String campoEsperado) {
            assertThat(camposInvalidos(request)).containsExactly(campoEsperado);
        }

        @Test
        @DisplayName("Si hay varios errores se informan todos juntos")
        void variosErrores() {
            ProductoRequest request = new ProductoRequest("", "M", new BigDecimal("-1"), -5);

            assertThat(camposInvalidos(request)).containsExactlyInAnyOrder("nombre", "costo", "cantidadDisponible");
        }
    }

    @Nested
    @DisplayName("ClienteRequest")
    class ClienteRequestTest {

        @ParameterizedTest(name = "DNI válido: {0}")
        @ValueSource(strings = {"1234567", "12345678", "123456789"})
        @DisplayName("Se aceptan DNI de 7 a 9 dígitos")
        void dniValido(String dni) {
            assertThat(validator.validate(new ClienteRequest("Ana", "Perez", dni))).isEmpty();
        }

        @ParameterizedTest(name = "DNI inválido: \"{0}\"")
        @ValueSource(strings = {"", "123456", "1234567890", "abc12345", "12.345.678", "1234 5678", "-1234567"})
        @DisplayName("Se rechazan DNI con longitud incorrecta o caracteres no numéricos")
        void dniInvalido(String dni) {
            assertThat(camposInvalidos(new ClienteRequest("Ana", "Perez", dni))).contains("dni");
        }

        @Test
        @DisplayName("DNI nulo se rechaza")
        void dniNulo() {
            assertThat(camposInvalidos(new ClienteRequest("Ana", "Perez", null))).contains("dni");
        }

        static Stream<Arguments> nombresInvalidos() {
            return Stream.of(
                    Arguments.of("nombre nulo", new ClienteRequest(null, "Perez", "30123456"), "nombre"),
                    Arguments.of("nombre vacío", new ClienteRequest(" ", "Perez", "30123456"), "nombre"),
                    Arguments.of("nombre largo", new ClienteRequest(texto(101), "Perez", "30123456"), "nombre"),
                    Arguments.of("apellido nulo", new ClienteRequest("Ana", null, "30123456"), "apellido"),
                    Arguments.of("apellido vacío", new ClienteRequest("Ana", "", "30123456"), "apellido"),
                    Arguments.of("apellido largo", new ClienteRequest("Ana", texto(101), "30123456"), "apellido")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("nombresInvalidos")
        @DisplayName("Nombre y apellido son obligatorios y de hasta 100 caracteres")
        void nombreYApellido(String caso, ClienteRequest request, String campoEsperado) {
            assertThat(camposInvalidos(request)).containsExactly(campoEsperado);
        }
    }

    @Nested
    @DisplayName("VentaRequest")
    class VentaRequestTest {

        private final ItemVentaRequest itemValido = new ItemVentaRequest(1L, 2);

        @Test
        @DisplayName("Una venta correcta no tiene errores (la fecha es opcional)")
        void ventaValida() {
            assertThat(validator.validate(new VentaRequest(1L, null, List.of(itemValido)))).isEmpty();
            assertThat(validator.validate(new VentaRequest(1L, LocalDate.of(2026, 1, 15), List.of(itemValido)))).isEmpty();
        }

        @Test
        @DisplayName("El cliente es obligatorio")
        void clienteObligatorio() {
            assertThat(camposInvalidos(new VentaRequest(null, null, List.of(itemValido)))).containsExactly("clienteId");
        }

        @Test
        @DisplayName("La lista de items no puede ser nula ni vacía")
        void itemsObligatorios() {
            assertThat(camposInvalidos(new VentaRequest(1L, null, null))).containsExactly("items");
            assertThat(camposInvalidos(new VentaRequest(1L, null, List.of()))).containsExactly("items");
        }

        @Test
        @DisplayName("No se permiten items nulos dentro de la lista")
        void itemNulo() {
            List<ItemVentaRequest> conNulo = Arrays.asList(itemValido, null);

            Set<ConstraintViolation<VentaRequest>> errores = validator.validate(new VentaRequest(1L, null, conNulo));

            assertThat(errores).hasSize(1);
            assertThat(errores.iterator().next().getMessage()).isEqualTo("Los items no pueden ser nulos");
        }

        @ParameterizedTest(name = "cantidad inválida: {0}")
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("La cantidad de cada item debe ser mayor a cero y el error indica qué item falló")
        void cantidadNoPositiva(int cantidad) {
            VentaRequest request = new VentaRequest(1L, null,
                    List.of(itemValido, new ItemVentaRequest(2L, cantidad)));

            assertThat(camposInvalidos(request)).containsExactly("items[1].cantidad");
        }

        @Test
        @DisplayName("El id del producto y la cantidad de cada item son obligatorios")
        void itemIncompleto() {
            VentaRequest request = new VentaRequest(1L, null, List.of(new ItemVentaRequest(null, null)));

            assertThat(camposInvalidos(request))
                    .containsExactlyInAnyOrder("items[0].productoId", "items[0].cantidad");
        }
    }
}
