package com.accel.finanzas.client;

import com.accel.finanzas.exception.MonedaNoSoportadaException;
import com.accel.finanzas.exception.TipoDeCambioNoDisponibleException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TipoDeCambioClient {

    private final RestClient tipoDeCambioRestClient;

    public BigDecimal tasa(String base, String destino) {
        RespuestaTasas respuesta;
        try {
            respuesta =
                    tipoDeCambioRestClient
                            .get()
                            .uri("/latest/{base}", base)
                            .retrieve()
                            .body(RespuestaTasas.class);
        } catch (RestClientException e) {
            log.warn("Falló la consulta de tipo de cambio {}->{}: {}", base, destino, e.getMessage());
            throw new TipoDeCambioNoDisponibleException(e);
        }

        if (respuesta == null || !"success".equals(respuesta.result())) {
            throw new TipoDeCambioNoDisponibleException(null);
        }

        BigDecimal tasa = respuesta.rates().get(destino);
        if (tasa == null) {
            throw new MonedaNoSoportadaException(destino);
        }
        return tasa;
    }
}
