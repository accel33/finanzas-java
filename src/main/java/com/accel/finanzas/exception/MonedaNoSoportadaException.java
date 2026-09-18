package com.accel.finanzas.exception;

public class MonedaNoSoportadaException extends RuntimeException {

    public MonedaNoSoportadaException(String moneda) {
        super("La moneda " + moneda + " no está soportada");
    }
}
