package com.medicita.entity;

import com.medicita.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "medico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(of = "id", callSuper = false)
public class Medico extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false, unique = true, length = 10)
    @ToString.Include
    private String cmp;
}
