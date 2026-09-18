package com.accel.finanzas;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Movimiento(Long id, String descripcion, BigDecimal monto, LocalDate fecha) {}
