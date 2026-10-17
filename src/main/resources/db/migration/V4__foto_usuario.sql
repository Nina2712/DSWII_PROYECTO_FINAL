-- Foto de perfil: imagen JPEG pequeña (128x128) guardada como data URL.
-- Se guarda en la base y no en el disco porque el disco de Render es efímero.
ALTER TABLE usuario ADD COLUMN foto VARCHAR(12000);
