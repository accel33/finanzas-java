package com.accel.finanzas.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tipo-de-cambio")
public record TipoDeCambioProperties(
        String url, Duration timeoutConexion, Duration timeoutLectura) {}
