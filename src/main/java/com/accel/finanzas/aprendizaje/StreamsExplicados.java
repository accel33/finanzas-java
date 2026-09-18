package com.accel.finanzas.aprendizaje;

import com.accel.finanzas.model.Movimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Material de aprendizaje. NO forma parte de la aplicación: borra este paquete entero cuando ya no
 * te sirva y nada se rompe.
 *
 * <p>Calcula el mismo total cuatro veces, de lo más explícito a lo más compacto. La última versión
 * es la que estaba en el viejo Main.java. Correr:
 *
 * <pre>
 *   ./mvnw compile
 *   java -cp target/classes com.accel.finanzas.aprendizaje.StreamsExplicados
 * </pre>
 */
public class StreamsExplicados {

    private static final List<Movimiento> MOVIMIENTOS =
            List.of(
                    new Movimiento(1L, "Almuerzo", new BigDecimal("25.50"), LocalDate.of(2026, 9, 15)),
                    new Movimiento(2L, "Taxi", new BigDecimal("12.00"), LocalDate.of(2026, 9, 16)),
                    new Movimiento(
                            3L, "Menú del viernes", new BigDecimal("18.90"), LocalDate.of(2026, 9, 17)));

    public static void main(String[] args) {
        System.out.println("1) Bucle de toda la vida:      S/ " + conBucle(MOVIMIENTOS));
        System.out.println("2) Stream con lambdas:         S/ " + conLambdas(MOVIMIENTOS));
        System.out.println("3) Stream con method refs:     S/ " + conMethodReferences(MOVIMIENTOS));
        System.out.println();
        System.out.println("4) Lo mismo, viendo cada paso del acumulador:");
        System.out.println("   total = S/ " + pasoAPaso(MOVIMIENTOS));
    }

    /**
     * Versión 1: el bucle imperativo. Dice CÓMO hacerlo: crea una caja, recórrela, ve sumando.
     *
     * <p>Detalle clave: BigDecimal es inmutable. {@code total.add(x)} NO modifica total, devuelve un
     * BigDecimal nuevo. Por eso hay que reasignar. Si escribes solo {@code total.add(...)} sin el
     * {@code total =}, el resultado se pierde y el método devuelve cero.
     */
    static BigDecimal conBucle(List<Movimiento> movimientos) {
        BigDecimal total = BigDecimal.ZERO;
        for (Movimiento m : movimientos) {
            total = total.add(m.monto());
        }
        return total;
    }

    /**
     * Versión 2: el mismo algoritmo como stream, pero con cada paso separado y con los tipos
     * escritos a mano (sin {@code var}) para que se vea qué entra y qué sale.
     *
     * <p>Un stream no es una colección: es una tubería por la que pasan los elementos uno a uno. Se
     * consume una sola vez; si lo intentas reutilizar, Java lanza IllegalStateException.
     */
    static BigDecimal conLambdas(List<Movimiento> movimientos) {
        // map = TRANSFORMAR cada elemento. Entran Movimiento, salen BigDecimal.
        // La lambda (Movimiento m) -> m.monto() es una función anónima: recibe m, devuelve su monto.
        List<BigDecimal> montos = movimientos.stream().map((Movimiento m) -> m.monto()).toList();

        // reduce = PLEGAR una lista a un solo valor, de izquierda a derecha.
        //   1er argumento: el valor inicial (también el resultado si la lista está vacía).
        //   2do argumento: cómo combinar lo acumulado con el siguiente elemento.
        BigDecimal total =
                montos.stream()
                        .reduce(
                                BigDecimal.ZERO,
                                (BigDecimal acumulado, BigDecimal siguiente) ->
                                        acumulado.add(siguiente));

        return total;
    }

    /**
     * Versión 3: idéntica a la 2, pero cambiando cada lambda por una <em>method reference</em>.
     *
     * <p>Son puro azúcar sintáctico, el compilador genera lo mismo:
     *
     * <pre>
     *   (Movimiento m) -> m.monto()                  se escribe   Movimiento::monto
     *   (acumulado, siguiente) -> acumulado.add(s)   se escribe   BigDecimal::add
     * </pre>
     *
     * <p>La segunda confunde al principio: {@code BigDecimal::add} parece que le falta el objeto
     * sobre el que se llama. Lo que dice es "toma el primer argumento como el objeto y el segundo
     * como el parámetro", o sea exactamente {@code acumulado.add(siguiente)}.
     */
    static BigDecimal conMethodReferences(List<Movimiento> movimientos) {
        return movimientos.stream().map(Movimiento::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Versión 4: el bucle otra vez, pero imprimiendo el acumulador en cada vuelta. Esto es
     * literalmente lo que hace el reduce de la versión 3, solo que a la vista.
     */
    static BigDecimal pasoAPaso(List<Movimiento> movimientos) {
        BigDecimal acumulado = BigDecimal.ZERO;
        int paso = 0;

        List<String> bitacora = new ArrayList<>();
        for (Movimiento m : movimientos) {
            BigDecimal antes = acumulado;
            acumulado = acumulado.add(m.monto());
            paso++;
            bitacora.add(
                    "   paso "
                            + paso
                            + ": "
                            + antes
                            + " + "
                            + m.monto()
                            + " = "
                            + acumulado
                            + "   ("
                            + m.descripcion()
                            + ")");
        }

        bitacora.forEach(System.out::println);
        return acumulado;
    }
}
