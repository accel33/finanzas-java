package com.accel.finanzas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringJUnitConfig(TipoDeCambioCacheTest.Contexto.class)
class TipoDeCambioCacheTest {

    @Autowired private TipoDeCambioClient cliente;

    @Autowired private MockRestServiceServer proveedor;

    @Test
    @DisplayName("el bean que Spring inyecta no es tu clase: es un proxy que la envuelve")
    void elClienteEsUnProxy() {
        assertThat(AopUtils.isCglibProxy(cliente)).isTrue();
        assertThat(cliente.getClass().getName()).contains("SpringCGLIB");
    }

    @Test
    @DisplayName("dos consultas de la misma base hacen UNA sola llamada HTTP")
    void laSegundaConsultaSaleDeLaCache() {
        proveedor
                .expect(once(), requestTo("https://tasas.test/latest/PEN"))
                .andRespond(
                        withSuccess(
                                """
                                {"result":"success","base_code":"PEN","rates":{"USD":0.30}}
                                """,
                                MediaType.APPLICATION_JSON));

        cliente.tasas("PEN");
        cliente.tasas("PEN");

        proveedor.verify();
    }

    @Configuration
    @EnableCaching
    static class Contexto {

        private final RestClient.Builder builder =
                RestClient.builder().baseUrl("https://tasas.test");

        @Bean
        MockRestServiceServer proveedor() {
            return MockRestServiceServer.bindTo(builder).build();
        }

        @Bean
        TipoDeCambioClient cliente(MockRestServiceServer proveedor) {
            return new TipoDeCambioClient(builder.build());
        }

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager("tasas");
        }
    }
}
