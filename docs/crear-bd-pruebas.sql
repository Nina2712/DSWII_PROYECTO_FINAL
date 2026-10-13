-- Ejecutar UNA VEZ en MySQL Workbench conectado como root.
-- Crea el esquema que usan las pruebas automáticas (separado de "medicita").
CREATE DATABASE IF NOT EXISTS medicita_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON medicita_test.* TO 'medicita'@'%';
FLUSH PRIVILEGES;
