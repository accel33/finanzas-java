package com.accel.finanzas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoRequest(
        @NotBlank(message = "La descripción es obligatoria")
                @Size(max = 120, message = "La descripción no puede pasar de 120 caracteres")
                String descripcion,
        @NotNull(message = "El monto es obligatorio")
                @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
                @Digits(
                        integer = 10,
                        fraction = 2,
                        message = "El monto admite como máximo 2 decimales")
                BigDecimal monto,
        @NotNull(message = "La fecha es obligatoria")
                @PastOrPresent(message = "La fecha no puede estar en el futuro")
                LocalDate fecha) {}
