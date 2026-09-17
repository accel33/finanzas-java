package com.accel.finanzas;

import java.math.BigDecimal;

// Dinero SIEMPRE en BigDecimal, nunca double: double pierde centavos (0.1 + 0.2 = 0.30000000000000004)
public record Movimiento(String descripcion, BigDecimal monto) {}
