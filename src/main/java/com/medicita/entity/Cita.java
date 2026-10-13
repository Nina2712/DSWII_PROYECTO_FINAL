package com.medicita.entity;

import com.medicita.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(of = "id", callSuper = false)
public class Cita extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Column(name = "fecha_hora", nullable = false)
    @ToString.Include
    private LocalDateTime fechaHora;

    @Builder.Default
    @Column(name = "duracion_min", nullable = false)
    private int duracionMin = 30;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado = EstadoCita.PENDIENTE;

    @Column(length = 255)
    private String motivo;

    @Column(length = 1000)
    private String observaciones;
}
