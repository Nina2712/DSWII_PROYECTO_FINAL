package com.medicita.exception;

/** Petición incompleta o incoherente (responde 400). */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
