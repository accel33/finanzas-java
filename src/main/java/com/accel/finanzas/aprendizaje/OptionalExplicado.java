package com.accel.finanzas.aprendizaje;

import com.accel.finanzas.Movimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Material de aprendizaje. NO forma parte de la aplicación: borra este paquete entero cuando ya no
 * te sirva y nada se rompe.
 *
 * <p>Desarma la línea más densa de MovimientoController:
 *
 * <pre>
 *   return Optional.ofNullable(movimientos.get(id))
 *           .map(ResponseEntity::ok)
 *           .orElseGet(() -&gt; ResponseEntity.notFound().build());
 * </pre>
 *
 * Correr:
 *
 * <pre>
 *   ./mvnw compile
 *   java -cp target/classes com.accel.finanzas.aprendizaje.OptionalExplicado
 * </pre>
 */
public class OptionalExplicado {

    private static final Map<Long, Movimiento> ALMACEN = new HashMap<>();

    static {
        ALMACEN.put(1L, new Movimiento(1L, "Almuerzo", new BigDecimal("25.50"), LocalDate.now()));
    }

    public static void main(String[] args) {
        System.out.println("Buscando el id 1 (existe) y el id 99 (no existe)\n");

        System.out.println("1) Con null a secas:");
        System.out.println("   id 1  -> " + conNull(1L));
        System.out.println("   id 99 -> " + conNull(99L));

        System.out.println("2) Con Optional, preguntando si hay algo:");
        System.out.println("   id 1  -> " + conOptionalVerboso(1L));
        System.out.println("   id 99 -> " + conOptionalVerboso(99L));

        System.out.println("3) Con Optional encadenado (lo que usa el controller):");
        System.out.println("   id 1  -> " + conOptionalEncadenado(1L));
        System.out.println("   id 99 -> " + conOptionalEncadenado(99L));
    }

    /**
     * Versión 1: el estilo de siempre. {@code Map.get} devuelve null cuando la clave no existe, y
     * eres tú quien tiene que acordarse de preguntarlo. Si se te olvida, el null viaja por el
     * programa y explota más adelante, lejos de aquí, con un NullPointerException.
     */
    static String conNull(Long id) {
        Movimiento encontrado = ALMACEN.get(id);
        if (encontrado == null) {
            return "404 Not Found";
        }
        return "200 OK con " + encontrado.descripcion();
    }

    /**
     * Versión 2: lo mismo, pero metiendo el resultado en un Optional.
     *
     * <p>Un Optional es una caja que puede venir con algo adentro o vacía. No evita el problema: lo
     * hace <em>visible</em> en el tipo. Un método que devuelve {@code Optional<Movimiento>} te está
     * avisando en su firma que puede no encontrar nada, cosa que {@code Movimiento} a secas nunca
     * te dice.
     */
    static String conOptionalVerboso(Long id) {
        Optional<Movimiento> caja = Optional.ofNullable(ALMACEN.get(id));
        if (caja.isEmpty()) {
            return "404 Not Found";
        }
        Movimiento encontrado = caja.get();
        return "200 OK con " + encontrado.descripcion();
    }

    /**
     * Versión 3: idéntica a la 2, escrita como cadena. Es la forma idiomática y la que está en el
     * controller. Traducción de cada eslabón:
     *
     * <pre>
     *   Optional.ofNullable(x)  "mete x en una caja; si x es null, la caja va vacía"
     *   .map(f)                 "si hay algo, aplícale f y vuelve a encajarlo; si está vacía, no hagas nada"
     *   .orElseGet(() -> y)     "dame lo que hay dentro, o calcula y si está vacía"
     * </pre>
     *
     * <p>La gracia es que el caso "no existe" ya no es un if que puedas olvidar: sin el
     * {@code orElseGet} el código ni siquiera compila, porque te faltaría el valor de retorno.
     *
     * <p>Detalle fino: {@code orElseGet} recibe una función y solo la ejecuta si la caja está
     * vacía. Su hermano {@code orElse} recibe un valor ya calculado, así que lo construye siempre,
     * aunque no haga falta.
     */
    static String conOptionalEncadenado(Long id) {
        return Optional.ofNullable(ALMACEN.get(id))
                .map(encontrado -> "200 OK con " + encontrado.descripcion())
                .orElseGet(() -> "404 Not Found");
    }
}
