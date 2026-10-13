package com.medicita.exception;

/** Se lanza cuando se viola una regla de negocio (horario ocupado, cancelación tardía, etc.). */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
