package com.accel.finanzas;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movimientos")
public class MovimientoController {

    record Resumen(int cantidad, BigDecimal total) {}

    private final Map<Long, Movimiento> movimientos = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    public MovimientoController() {
        guardarNuevo("Almuerzo", new BigDecimal("25.50"), LocalDate.of(2026, 9, 15));
        guardarNuevo("Taxi", new BigDecimal("12.00"), LocalDate.of(2026, 9, 16));
        guardarNuevo("Menú del viernes", new BigDecimal("18.90"), LocalDate.of(2026, 9, 17));
    }

    @GetMapping
    public List<Movimiento> listar() {
        return movimientos.values().stream().sorted(Comparator.comparing(Movimiento::id)).toList();
    }

    @GetMapping("/resumen")
    public Resumen resumen() {
        BigDecimal total =
                movimientos.values().stream()
                        .map(Movimiento::monto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resumen(movimientos.size(), total);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movimiento> obtener(@PathVariable Long id) {
        return Optional.ofNullable(movimientos.get(id))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Movimiento> crear(@RequestBody Movimiento entrada) {
        Movimiento creado = guardarNuevo(entrada.descripcion(), entrada.monto(), entrada.fecha());
        return ResponseEntity.created(URI.create("/movimientos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movimiento> reemplazar(
            @PathVariable Long id, @RequestBody Movimiento entrada) {
        if (!movimientos.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        Movimiento actualizado =
                new Movimiento(id, entrada.descripcion(), entrada.monto(), entrada.fecha());
        movimientos.put(id, actualizado);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (movimientos.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private Movimiento guardarNuevo(String descripcion, BigDecimal monto, LocalDate fecha) {
        long id = siguienteId.getAndIncrement();
        Movimiento nuevo = new Movimiento(id, descripcion, monto, fecha);
        movimientos.put(id, nuevo);
        return nuevo;
    }
}
