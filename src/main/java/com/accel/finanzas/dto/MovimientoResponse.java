package com.accel.finanzas.dto;

import com.accel.finanzas.model.Movimiento;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoResponse(Long id, String descripcion, BigDecimal monto, LocalDate fecha) {

    public static MovimientoResponse desde(Movimiento movimiento) {
        return new MovimientoResponse(
                movimiento.id(), movimiento.descripcion(), movimiento.monto(), movimiento.fecha());
    }
}
