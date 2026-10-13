-- Datos de demostración. Contraseña de TODOS los usuarios: Medicita2026*
INSERT INTO especialidad (id, nombre, descripcion) VALUES
 (1,'Medicina General','Atención primaria y control preventivo'),
 (2,'Cardiología','Diagnóstico y tratamiento del corazón'),
 (3,'Pediatría','Salud de niños y adolescentes'),
 (4,'Dermatología','Cuidado de la piel'),
 (5,'Ginecología','Salud de la mujer'),
 (6,'Traumatología','Huesos, músculos y articulaciones');

INSERT INTO sede (id, nombre, direccion, distrito) VALUES
 (1,'Sede Miraflores','Av. Larco 1200','Miraflores'),
 (2,'Sede San Miguel','Av. La Marina 2500','San Miguel');

INSERT INTO usuario (id,email,password,nombres,apellidos,rol) VALUES
 (1,'admin@medicita.pe','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Carla','Mendoza Ríos','ADMIN'),
 (2,'recepcion@medicita.pe','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Lucía','Paredes Soto','RECEPCIONISTA'),
 (3,'dr.vargas@medicita.pe','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Ricardo','Vargas Luna','MEDICO'),
 (4,'dra.salazar@medicita.pe','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Mónica','Salazar Quispe','MEDICO'),
 (5,'dr.huaman@medicita.pe','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Jorge','Huamán Castro','MEDICO'),
 (6,'ana.torres@correo.com','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Ana','Torres Díaz','PACIENTE'),
 (7,'luis.ramos@correo.com','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Luis','Ramos Vega','PACIENTE'),
 (8,'sofia.cruz@correo.com','$2b$10$XXIuwgLSd96loN2xAQZD.OcOk7RSaEOe5.gxqAbhDPAKqt/aYGtZy','Sofía','Cruz Medina','PACIENTE');

INSERT INTO medico (id,usuario_id,especialidad_id,sede_id,cmp) VALUES
 (1,3,2,1,'CMP-45821'),
 (2,4,3,1,'CMP-51204'),
 (3,5,1,2,'CMP-38977');

INSERT INTO paciente (id,usuario_id,dni,telefono,fecha_nacimiento) VALUES
 (1,6,'70123456','987654321','1994-03-12'),
 (2,7,'71234567','976543210','1988-11-02'),
 (3,8,'72345678','965432109','2001-07-25');

INSERT INTO horario_disponible (medico_id,dia_semana,hora_inicio,hora_fin) VALUES
 (1,'LUNES','08:00','13:00'),(1,'MIERCOLES','08:00','13:00'),(1,'VIERNES','08:00','13:00'),
 (2,'MARTES','09:00','14:00'),(2,'JUEVES','09:00','14:00'),
 (3,'LUNES','14:00','19:00'),(3,'MARTES','14:00','19:00'),(3,'JUEVES','14:00','19:00');

-- Citas relativas a la fecha actual para que el dashboard siempre tenga datos
INSERT INTO cita (paciente_id,medico_id,fecha_hora,estado,motivo) VALUES
 (1,1,TIMESTAMP(DATE_SUB(CURDATE(),INTERVAL 6 DAY),'09:00:00'),'ATENDIDA','Control de presión'),
 (2,3,TIMESTAMP(DATE_SUB(CURDATE(),INTERVAL 5 DAY),'15:00:00'),'ATENDIDA','Chequeo general'),
 (3,2,TIMESTAMP(DATE_SUB(CURDATE(),INTERVAL 4 DAY),'10:00:00'),'NO_ASISTIO','Control pediátrico'),
 (1,2,TIMESTAMP(DATE_SUB(CURDATE(),INTERVAL 3 DAY),'11:00:00'),'ATENDIDA','Consulta de rutina'),
 (2,1,TIMESTAMP(DATE_SUB(CURDATE(),INTERVAL 2 DAY),'08:30:00'),'CANCELADA','Dolor de pecho leve'),
 (3,3,TIMESTAMP(DATE_SUB(CURDATE(),INTERVAL 1 DAY),'16:00:00'),'ATENDIDA','Resfrío persistente'),
 (1,1,TIMESTAMP(CURDATE(),'09:30:00'),'CONFIRMADA','Resultados de electrocardiograma'),
 (2,3,TIMESTAMP(CURDATE(),'15:30:00'),'CONFIRMADA','Control de azúcar'),
 (3,2,TIMESTAMP(DATE_ADD(CURDATE(),INTERVAL 1 DAY),'10:30:00'),'PENDIENTE','Vacunación'),
 (1,3,TIMESTAMP(DATE_ADD(CURDATE(),INTERVAL 2 DAY),'17:00:00'),'PENDIENTE','Dolor de espalda'),
 (2,1,TIMESTAMP(DATE_ADD(CURDATE(),INTERVAL 3 DAY),'08:00:00'),'PENDIENTE','Control cardiológico'),
 (3,1,TIMESTAMP(DATE_ADD(CURDATE(),INTERVAL 5 DAY),'11:30:00'),'PENDIENTE','Chequeo preventivo');
