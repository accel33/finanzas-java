package com.accel.finanzas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.accel.finanzas.exception.TipoDeCambioNoDisponibleException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TipoDeCambioClientTest {

    private static final String RESPUESTA_REAL =
            """
            {"result":"success","provider":"https://www.exchangerate-api.com",
             "base_code":"PEN","time_last_update_utc":"Fri, 18 Sep 2026 00:02:31 +0000",
             "rates":{"PEN":1,"USD":0.296909,"EUR":0.258589}}
            """;

    private MockRestServiceServer proveedor;
    private TipoDeCambioClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://tasas.test");
        proveedor = MockRestServiceServer.bindTo(builder).build();
        cliente = new TipoDeCambioClient(builder.build());
    }

    @Test
    @DisplayName("lee todas las tasas del JSON del proveedor, sin perder decimales")
    void leeLasTasas() {
        proveedor
                .expect(requestTo("https://tasas.test/latest/PEN"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(RESPUESTA_REAL, MediaType.APPLICATION_JSON));

        Map<String, BigDecimal> tasas = cliente.tasas("PEN");

        assertThat(tasas.get("USD")).isEqualByComparingTo("0.296909");
        assertThat(tasas).containsKeys("PEN", "USD", "EUR");
        proveedor.verify();
    }

    @Test
    @DisplayName("un 500 del proveedor se convierte en TipoDeCambioNoDisponible")
    void proveedorConError() {
        proveedor.expect(requestTo("https://tasas.test/latest/PEN")).andRespond(withServerError());

        assertThatThrownBy(() -> cliente.tasas("PEN"))
                .isInstanceOf(TipoDeCambioNoDisponibleException.class);
    }
}
