package com.accel.finanzas.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Movimiento(Long id, String descripcion, BigDecimal monto, LocalDate fecha) {}
