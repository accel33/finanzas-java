package com.accel.finanzas;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("hola mundo somos amigos");

        var config = new Properties();
        try (InputStream in = Main.class.getResourceAsStream("/app.properties")) {
            config.load(in);
        }

        System.out.println("=== " + config.getProperty("app.nombre") + " ===");

        var movimientos =
                List.of(
                        new Movimiento("Almuerzo", new BigDecimal("25.50")),
                        new Movimiento("Taxi", new BigDecimal("12.00")),
                        new Movimiento("Menú del viernes", new BigDecimal("18.90")));

        movimientos.forEach(m -> System.out.println(" - " + m.descripcion() + ": S/ " + m.monto()));

        var total =
                movimientos.stream()
                        .map(Movimiento::monto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("Total gastado: S/ " + total);
    }
}
