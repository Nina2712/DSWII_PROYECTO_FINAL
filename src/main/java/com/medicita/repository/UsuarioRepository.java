package com.medicita.repository;

import com.medicita.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import com.medicita.enums.Rol;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByRolInOrderByNombresAscApellidosAsc(Collection<Rol> roles);

    long countByRolAndActivoTrue(Rol rol);
}
