package com.accel.finanzas.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TipoDeCambioProperties.class)
public class TipoDeCambioConfig {

    @Bean
    RestClient tipoDeCambioRestClient(
            RestClient.Builder builder, TipoDeCambioProperties propiedades) {
        HttpClient http =
                HttpClient.newBuilder().connectTimeout(propiedades.timeoutConexion()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(propiedades.timeoutLectura());

        return builder.baseUrl(propiedades.url()).requestFactory(fabrica).build();
    }
}
