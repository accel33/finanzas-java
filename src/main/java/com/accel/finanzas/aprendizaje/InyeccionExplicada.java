package com.accel.finanzas.aprendizaje;

import com.accel.finanzas.model.Movimiento;
import com.accel.finanzas.repository.MovimientoRepository;
import com.accel.finanzas.repository.MovimientoRepositoryEnMemoria;
import com.accel.finanzas.service.MovimientoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Material de aprendizaje. NO forma parte de la aplicación: borra este paquete entero cuando ya no
 * te sirva y nada se rompe.
 *
 * <p>Demuestra que la inyección de dependencias no es magia: arma el servicio a mano, sin Spring,
 * sin anotaciones y sin contenedor. Lo único que hace Spring en la aplicación real es exactamente
 * esto, automáticamente. Correr:
 *
 * <pre>
 *   ./mvnw compile
 *   java -cp target/classes com.accel.finanzas.aprendizaje.InyeccionExplicada
 * </pre>
 */
public class InyeccionExplicada {

    public static void main(String[] args) {
        System.out.println("1) Cableado a mano, igual que lo hace Spring al arrancar:");
        MovimientoRepository repositorio = new MovimientoRepositoryEnMemoria();
        MovimientoService servicio = new MovimientoService(repositorio);
        System.out.println("   total con el repositorio de verdad -> " + servicio.resumen());

        System.out.println();
        System.out.println("2) El mismo servicio, con otro repositorio, sin tocar el servicio:");
        MovimientoService servicioFalso = new MovimientoService(new RepositorioDeMentira());
        System.out.println("   total con el repositorio falso     -> " + servicioFalso.resumen());

        System.out.println();
        System.out.println("Eso es para lo que sirve que MovimientoRepository sea una interfaz:");
        System.out.println("el servicio depende del contrato, no de quién lo cumple. En la Etapa 5");
        System.out.println("el repositorio real pasará a PostgreSQL y el servicio no cambiará; en");
        System.out.println("la Etapa 4, en los tests, se le pasará un doble como este.");
    }

    /**
     * Un repositorio de mentira, con datos fijos. En la Etapa 4 esto lo generará Mockito en una
     * línea, pero escribirlo a mano una vez deja claro qué es exactamente un "mock": otra
     * implementación de la misma interfaz.
     */
    static class RepositorioDeMentira implements MovimientoRepository {

        private static final Movimiento UNICO =
                new Movimiento(1L, "Dato de prueba", new BigDecimal("100.00"), LocalDate.now());

        @Override
        public List<Movimiento> buscarTodos() {
            return List.of(UNICO);
        }

        @Override
        public Optional<Movimiento> buscarPorId(Long id) {
            return id == 1L ? Optional.of(UNICO) : Optional.empty();
        }

        @Override
        public Movimiento guardar(Movimiento movimiento) {
            return UNICO;
        }

        @Override
        public boolean eliminar(Long id) {
            return false;
        }
    }
}
