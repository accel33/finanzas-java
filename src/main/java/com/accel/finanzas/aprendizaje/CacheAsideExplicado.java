package com.accel.finanzas.aprendizaje;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Material de aprendizaje. NO forma parte de la aplicación.
 *
 * <p>Dos cosas escritas a mano, sin Spring:
 *
 * <ol>
 *   <li>El patrón Cache Aside, paso a paso. Es lo que hace {@code @Cacheable} por dentro.
 *   <li>Un proxy casero, para ver por qué {@code @Cacheable} no funciona cuando un método lo llama
 *       otro método de la misma clase. La misma trampa aplica a {@code @Transactional}.
 * </ol>
 *
 * <pre>
 *   ./mvnw compile
 *   java -cp target/classes com.accel.finanzas.aprendizaje.CacheAsideExplicado
 * </pre>
 */
public class CacheAsideExplicado {

    interface Proveedor {
        Map<String, BigDecimal> tasas(String base);

        BigDecimal tasa(String base, String destino);
    }

    /** El proveedor "real". Cuenta cuántas veces lo consultan de verdad. */
    static class ProveedorReal implements Proveedor {
        int consultas = 0;

        @Override
        public Map<String, BigDecimal> tasas(String base) {
            consultas++;
            System.out.println("        (consulta real al proveedor #" + consultas + ")");
            return Map.of("USD", new BigDecimal("0.30"), "EUR", new BigDecimal("0.26"));
        }

        @Override
        public BigDecimal tasa(String base, String destino) {
            return tasas(base).get(destino);
        }
    }

    /**
     * Cache Aside en sus cuatro pasos. El nombre viene de que la caché está "al lado": la
     * aplicación la consulta y la llena ella misma; la caché no sabe nada de la fuente.
     */
    static Map<String, BigDecimal> cacheAside(
            String base, Map<String, Map<String, BigDecimal>> cache, Proveedor fuente) {
        Map<String, BigDecimal> enCache = cache.get(base);
        if (enCache != null) {
            System.out.println("      HIT  -> sale de la caché");
            return enCache;
        }
        System.out.println("      MISS -> no estaba, voy a la fuente");
        Map<String, BigDecimal> deLaFuente = fuente.tasas(base);
        cache.put(base, deLaFuente);
        return deLaFuente;
    }

    /**
     * Un proxy hecho a mano: envuelve al objeto real y se interpone en las llamadas. Spring genera
     * uno así, en tiempo de ejecución, para cada bean con {@code @Cacheable} o
     * {@code @Transactional}. Es una subclase que tú nunca escribes.
     */
    static class ProxyConCache implements Proveedor {
        private final Proveedor real;
        private final Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();

        ProxyConCache(Proveedor real) {
            this.real = real;
        }

        @Override
        public Map<String, BigDecimal> tasas(String base) {
            return cacheAside(base, cache, real);
        }

        @Override
        public BigDecimal tasa(String base, String destino) {
            return real.tasa(base, destino);
        }
    }

    public static void main(String[] args) {
        System.out.println("1) Cache Aside a mano, tres consultas de la misma base:");
        Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();
        ProveedorReal fuente = new ProveedorReal();
        for (int i = 1; i <= 3; i++) {
            System.out.println("   consulta " + i + ":");
            cacheAside("PEN", cache, fuente);
        }
        System.out.println("   -> el proveedor se consultó " + fuente.consultas + " vez\n");

        System.out.println("   La versión compacta del mismo patrón es una sola línea:");
        System.out.println("   cache.computeIfAbsent(base, fuente::tasas)\n");

        System.out.println("2) A través del proxy, llamando a tasas() desde FUERA:");
        ProveedorReal real = new ProveedorReal();
        Proveedor proxy = new ProxyConCache(real);
        proxy.tasas("PEN");
        proxy.tasas("PEN");
        System.out.println("   -> consultas reales: " + real.consultas + "   (la caché funciona)\n");

        System.out.println("3) A través del proxy, pero llamando a tasa(), que por DENTRO llama a tasas():");
        ProveedorReal real2 = new ProveedorReal();
        Proveedor proxy2 = new ProxyConCache(real2);
        proxy2.tasa("PEN", "USD");
        proxy2.tasa("PEN", "USD");
        System.out.println("   -> consultas reales: " + real2.consultas + "   (la caché NO funciona)\n");

        System.out.println("¿Por qué? El proxy solo ve las llamadas que pasan por él. Cuando tasa()");
        System.out.println("llama a tasas(), lo hace sobre 'this', que es el objeto real, no el proxy.");
        System.out.println("La llamada nunca sale del objeto, así que el proxy no se entera.");
    }
}
