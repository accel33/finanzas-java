package com.accel.finanzas.client;

import com.accel.finanzas.exception.TipoDeCambioNoDisponibleException;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TipoDeCambioClient {

    private final RestClient tipoDeCambioRestClient;

    @Cacheable(cacheNames = "tasas", sync = true)
    public Map<String, BigDecimal> tasas(String base) {
        log.info("Consultando al proveedor las tasas de {}", base);
        RespuestaTasas respuesta;
        try {
            respuesta =
                    tipoDeCambioRestClient
                            .get()
                            .uri("/latest/{base}", base)
                            .retrieve()
                            .body(RespuestaTasas.class);
        } catch (RestClientException e) {
            log.warn("Falló la consulta de tipo de cambio de {}: {}", base, e.getMessage());
            throw new TipoDeCambioNoDisponibleException(e);
        }

        if (respuesta == null || !"success".equals(respuesta.result())) {
            throw new TipoDeCambioNoDisponibleException(null);
        }
        return respuesta.rates();
    }
}
