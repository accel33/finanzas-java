package com.accel.finanzas.aprendizaje;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Laboratorio de timeouts. NO forma parte de la aplicación.
 *
 * <p>Imita a la API de tipo de cambio, pero tarda lo que tú le digas en responder. Sirve para ver
 * con tus propios ojos qué hace tu aplicación cuando una dependencia externa se pone lenta, que es
 * el fallo más peligroso de todos: una dependencia caída responde error al instante; una lenta
 * retiene tus hilos mientras espera.
 *
 * <pre>
 *   # terminal 1: un "proveedor" que tarda 10 segundos
 *   java -cp target/classes com.accel.finanzas.aprendizaje.ServidorLento 10
 *
 *   # terminal 2: la app apuntando a él
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments=--tipo-de-cambio.url=http://localhost:9099
 *
 *   # terminal 3: con timeout de 3 s, responde 503 a los 3 s en vez de esperar 10
 *   curl -w '\n%{time_total}s\n' http://localhost:8080/movimientos/resumen/USD
 * </pre>
 *
 * <p>Cada petición se atiende en un <em>virtual thread</em> (Java 21): hilos baratísimos de la JVM,
 * así que puede haber miles esperando sin agotar nada. Tomcat, en cambio, usa por defecto un pool
 * de 200 hilos de sistema operativo; si 200 peticiones se quedan esperando a un proveedor lento, la
 * petición 201 ya no tiene quién la atienda. Por eso existen los timeouts.
 */
public class ServidorLento {

    public static void main(String[] args) throws Exception {
        int segundos = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        byte[] cuerpo =
                """
                {"result":"success","base_code":"PEN","time_last_update_utc":"servidor lento",\
                "rates":{"PEN":1,"USD":0.30,"EUR":0.26}}"""
                        .getBytes(StandardCharsets.UTF_8);

        AtomicInteger peticiones = new AtomicInteger();
        HttpServer servidor = HttpServer.create(new InetSocketAddress(9099), 0);
        servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        servidor.createContext(
                "/latest/",
                intercambio -> {
                    System.out.println("  proveedor: petición #" + peticiones.incrementAndGet());
                    try {
                        Thread.sleep(segundos * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    intercambio.getResponseHeaders().add("Content-Type", "application/json");
                    intercambio.sendResponseHeaders(200, cuerpo.length);
                    try (OutputStream salida = intercambio.getResponseBody()) {
                        salida.write(cuerpo);
                    }
                });
        servidor.start();
        System.out.println(
                "Proveedor lento en http://localhost:9099/latest/PEN (responde en " + segundos + " s)");
    }
}
