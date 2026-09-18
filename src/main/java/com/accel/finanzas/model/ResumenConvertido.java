package com.accel.finanzas.model;

import java.math.BigDecimal;

public record ResumenConvertido(
        int cantidad,
        BigDecimal totalSoles,
        String moneda,
        BigDecimal tasa,
        BigDecimal totalConvertido) {}
