package com.accel.finanzas.dto;

import com.accel.finanzas.model.ResumenConvertido;
import java.math.BigDecimal;

public record ResumenConvertidoResponse(
        int cantidad,
        BigDecimal totalSoles,
        String moneda,
        BigDecimal tasa,
        BigDecimal totalConvertido) {

    public static ResumenConvertidoResponse desde(ResumenConvertido resumen) {
        return new ResumenConvertidoResponse(
                resumen.cantidad(),
                resumen.totalSoles(),
                resumen.moneda(),
                resumen.tasa(),
                resumen.totalConvertido());
    }
}
