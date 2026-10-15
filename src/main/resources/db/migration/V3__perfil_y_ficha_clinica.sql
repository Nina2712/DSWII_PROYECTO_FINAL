-- Ficha básica del paciente (la completa él mismo en "Mi perfil") y datos de contacto/presentación del médico.
ALTER TABLE paciente
    ADD COLUMN direccion                    VARCHAR(200),
    ADD COLUMN tipo_seguro                  VARCHAR(20),
    ADD COLUMN alergias                     VARCHAR(500),
    ADD COLUMN contacto_emergencia_nombre   VARCHAR(100),
    ADD COLUMN contacto_emergencia_telefono VARCHAR(15);

ALTER TABLE medico
    ADD COLUMN telefono    VARCHAR(15),
    ADD COLUMN descripcion VARCHAR(500);
