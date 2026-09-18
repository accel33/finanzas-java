package com.accel.finanzas.exception;

public class MovimientoNoEncontradoException extends RuntimeException {

    public MovimientoNoEncontradoException(Long id) {
        super("No existe un movimiento con id " + id);
    }
}
