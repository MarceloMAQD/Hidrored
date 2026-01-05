package com.hidrored.aplicacion.usuarios;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.hidrored.dominio.usuarios.IUsuarioRepository;
import com.hidrored.dominio.usuarios.modelo.Usuario;

@Service
public class UsuarioApplicationService {
  private final IUsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;

  public UsuarioApplicationService(IUsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public UsuarioDTO registrarUsuario(RegistrarUsuarioCommand command) {
    if (usuarioRepository.findByEmail(command.getEmail()).isPresent()) {
      throw new IllegalStateException("El correo electrónico ya está en uso.");
    }
    String hashedPassword = passwordEncoder.encode(command.getPassword());
    Usuario nuevoUsuario = new Usuario(
        command.getNombre(),
        command.getEmail(),
        command.getTelefono(),
        hashedPassword);
    return UsuarioDTO.fromDomain(usuarioRepository.save(nuevoUsuario));
  }
}