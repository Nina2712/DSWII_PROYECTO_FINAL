CREATE TABLE usuario (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(120) NOT NULL,
    password       VARCHAR(100) NOT NULL,
    nombres        VARCHAR(80)  NOT NULL,
    apellidos      VARCHAR(80)  NOT NULL,
    rol            VARCHAR(20)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE especialidad (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(80)  NOT NULL,
    descripcion    VARCHAR(255),
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_especialidad_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sede (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(100) NOT NULL,
    direccion      VARCHAR(200) NOT NULL,
    distrito       VARCHAR(80)  NOT NULL,
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE paciente (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT      NOT NULL,
    dni              VARCHAR(8)  NOT NULL,
    telefono         VARCHAR(15),
    fecha_nacimiento DATE,
    creado_en        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_paciente_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_paciente_dni UNIQUE (dni),
    CONSTRAINT fk_paciente_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE medico (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL,
    especialidad_id BIGINT      NOT NULL,
    sede_id         BIGINT      NOT NULL,
    cmp             VARCHAR(10) NOT NULL,
    creado_en       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_medico_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_medico_cmp UNIQUE (cmp),
    CONSTRAINT fk_medico_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_medico_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidad (id),
    CONSTRAINT fk_medico_sede FOREIGN KEY (sede_id) REFERENCES sede (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE horario_disponible (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    medico_id      BIGINT      NOT NULL,
    dia_semana     VARCHAR(12) NOT NULL,
    hora_inicio    TIME        NOT NULL,
    hora_fin       TIME        NOT NULL,
    creado_en      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_horario_medico FOREIGN KEY (medico_id) REFERENCES medico (id),
    CONSTRAINT ck_horario_rango CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- slot_activo es NULL cuando la cita se cancela o no asiste:
-- el horario vuelve a quedar libre, pero dos citas activas no pueden chocar.
CREATE TABLE cita (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id    BIGINT       NOT NULL,
    medico_id      BIGINT       NOT NULL,
    fecha_hora     DATETIME     NOT NULL,
    duracion_min   INT          NOT NULL DEFAULT 30,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    motivo         VARCHAR(255),
    observaciones  VARCHAR(1000),
    slot_activo    DATETIME GENERATED ALWAYS AS
                   (IF(estado IN ('CANCELADA','NO_ASISTIO'), NULL, fecha_hora)) STORED,
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cita_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id),
    CONSTRAINT fk_cita_medico FOREIGN KEY (medico_id) REFERENCES medico (id),
    CONSTRAINT uk_cita_medico_slot UNIQUE (medico_id, slot_activo),
    INDEX idx_cita_fecha (fecha_hora),
    INDEX idx_cita_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
