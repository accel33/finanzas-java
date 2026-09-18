package com.accel.finanzas.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(MovimientoNoEncontradoException.class)
    public ProblemDetail noEncontrado(MovimientoNoEncontradoException excepcion) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excepcion.getMessage());
        problema.setTitle("Movimiento no encontrado");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail datosInvalidos(MethodArgumentNotValidException excepcion) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : excepcion.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Revisa los campos enviados");
        problema.setTitle("Datos inválidos");
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler(TipoDeCambioNoDisponibleException.class)
    public ProblemDetail tipoDeCambioNoDisponible(TipoDeCambioNoDisponibleException excepcion) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo obtener el tipo de cambio. Intenta de nuevo en unos segundos.");
        problema.setTitle("Tipo de cambio no disponible");
        return problema;
    }

    @ExceptionHandler(MonedaNoSoportadaException.class)
    public ProblemDetail monedaNoSoportada(MonedaNoSoportadaException excepcion) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excepcion.getMessage());
        problema.setTitle("Moneda no soportada");
        return problema;
    }
}
