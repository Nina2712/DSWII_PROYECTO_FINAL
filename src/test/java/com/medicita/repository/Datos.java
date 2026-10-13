package com.medicita.repository;

import com.medicita.entity.Cita;
import com.medicita.entity.Especialidad;
import com.medicita.entity.Medico;
import com.medicita.entity.Paciente;
import com.medicita.entity.Sede;
import com.medicita.entity.Usuario;
import com.medicita.enums.EstadoCita;
import com.medicita.enums.Rol;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fábrica de datos de prueba: persiste directamente con el EntityManager (sin pasar por los repositorios). */
final class Datos {

    static final LocalDateTime F1 = LocalDateTime.of(2030, 1, 7, 10, 0);
    static final LocalDateTime F2 = LocalDateTime.of(2030, 1, 8, 10, 0);
    static final LocalDateTime F3 = LocalDateTime.of(2030, 1, 9, 10, 0);

    private Datos() {
    }

    static Usuario usuario(TestEntityManager em, String email, Rol rol) {
        return em.persistAndFlush(Usuario.builder()
                .email(email)
                .password("$2a$10$hashDePruebaNoReal")
                .nombres("Nombre")
                .apellidos("Apellido")
                .rol(rol)
                .build());
    }

    static Especialidad especialidad(TestEntityManager em, String nombre) {
        return em.persistAndFlush(Especialidad.builder().nombre(nombre).descripcion("Descripción de prueba").build());
    }

    static Sede sede(TestEntityManager em) {
        return em.persistAndFlush(Sede.builder()
                .nombre("Sede de prueba").direccion("Av. Prueba 123").distrito("Miraflores").build());
    }

    static Paciente paciente(TestEntityManager em, String email, String dni) {
        Usuario usuario = usuario(em, email, Rol.PACIENTE);
        return em.persistAndFlush(Paciente.builder()
                .usuario(usuario).dni(dni).telefono("987654321")
                .fechaNacimiento(LocalDate.of(1995, 5, 20)).build());
    }

    static Medico medico(TestEntityManager em, String email, String cmp, Especialidad especialidad, Sede sede) {
        Usuario usuario = usuario(em, email, Rol.MEDICO);
        return em.persistAndFlush(Medico.builder()
                .usuario(usuario).especialidad(especialidad).sede(sede).cmp(cmp).build());
    }

    static Cita cita(TestEntityManager em, Paciente paciente, Medico medico,
                     LocalDateTime fechaHora, EstadoCita estado) {
        return em.persistAndFlush(Cita.builder()
                .paciente(paciente).medico(medico).fechaHora(fechaHora)
                .estado(estado).motivo("Control").build());
    }
}
