package com.hidrored.dominio.usuarios;

import com.hidrored.dominio.usuarios.modelo.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface IUsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
}