package com.hidrored.dominio.usuarios.modelo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
@Getter
@NoArgsConstructor
public class Usuario {
    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String email;
    private String telefono;
    private String password;

    public Usuario(String nombre, String email, String telefono, String password) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.password = password;
    }
}