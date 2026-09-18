package com.accel.finanzas.exception;

public class TipoDeCambioNoDisponibleException extends RuntimeException {

    public TipoDeCambioNoDisponibleException(Throwable causa) {
        super("El servicio de tipo de cambio no está disponible", causa);
    }
}
