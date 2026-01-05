package com.hidrored.presentacion.usuarios;

import com.hidrored.aplicacion.usuarios.RegistrarUsuarioCommand;
import com.hidrored.aplicacion.usuarios.UsuarioApplicationService;
import com.hidrored.aplicacion.usuarios.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite conexión desde el frontend
public class UsuarioController {
    private final UsuarioApplicationService usuarioService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registrar(@RequestBody RegistrarUsuarioCommand command) {
        return ResponseEntity.ok(usuarioService.registrarUsuario(command));
    }
}