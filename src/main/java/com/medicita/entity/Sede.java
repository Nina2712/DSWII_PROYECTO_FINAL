package com.medicita.entity;

import com.medicita.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "sede")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(of = "id", callSuper = false)
public class Sede extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, length = 100)
    @ToString.Include
    private String nombre;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false, length = 80)
    private String distrito;
}
