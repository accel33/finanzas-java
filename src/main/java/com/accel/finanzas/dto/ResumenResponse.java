package com.accel.finanzas.dto;

import com.accel.finanzas.model.Resumen;
import java.math.BigDecimal;

public record ResumenResponse(int cantidad, BigDecimal total) {

    public static ResumenResponse desde(Resumen resumen) {
        return new ResumenResponse(resumen.cantidad(), resumen.total());
    }
}
