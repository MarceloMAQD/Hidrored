package com.hidrored.aplicacion.usuarios;

import com.hidrored.dominio.usuarios.modelo.Usuario;
import lombok.Getter;

@Getter
public class UsuarioDTO {
  private final String id;
  private final String nombre;
  private final String email;
  private final String telefono;

  private UsuarioDTO(String id, String nombre, String email, String telefono) {
    this.id = id;
    this.nombre = nombre;
    this.email = email;
    this.telefono = telefono;
  }

  public static UsuarioDTO fromDomain(Usuario usuario) {
    return new UsuarioDTO(
        usuario.getId(),
        usuario.getNombre(),
        usuario.getEmail(),
        usuario.getTelefono());
  }
}