package com.bazar.api_bazar.controller;
import com.bazar.api_bazar.model.Cliente;
import com.bazar.api_bazar.service.IClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ClienteController {

    @Autowired
    private IClienteService clienteService;

    @PostMapping("/clientes/crear")
    public ResponseEntity<String> crearCliente(@RequestBody Cliente cliente) {
        clienteService.saveCliente(cliente);
        return new ResponseEntity<>("Cliente creado correctamente", HttpStatus.CREATED); //201
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<Cliente>> traerClientes() {
        List<Cliente> clientes = clienteService.getClientes();
        return new ResponseEntity<>(clientes, HttpStatus.OK); //200
    }

    @GetMapping("/clientes/{id_cliente}")
    public ResponseEntity<Cliente> traerCliente(@PathVariable Long id_cliente) {
        Cliente cliente = clienteService.getCliente(id_cliente);
        return new ResponseEntity<>(cliente, HttpStatus.OK); // Devuelve 200
    }

    @DeleteMapping("/clientes/eliminar/{id_cliente}")
    public ResponseEntity<String> eliminarCliente(@PathVariable Long id_cliente) {
        clienteService.deleteCliente(id_cliente);
        return new ResponseEntity<>("Cliente eliminado correctamente", HttpStatus.OK); // Devuelve 200
    }

    @PutMapping("/clientes/editar/{id_cliente}")
    public ResponseEntity<Cliente> editarCliente(@PathVariable Long id_cliente, @RequestBody Cliente clienteEditado) {
        clienteEditado.setId_cliente(id_cliente);
        clienteService.editCliente(clienteEditado);

        Cliente clienteActualizado = clienteService.getCliente(id_cliente);
        return new ResponseEntity<>(clienteActualizado, HttpStatus.OK); // Devuelve 200
    }
}
