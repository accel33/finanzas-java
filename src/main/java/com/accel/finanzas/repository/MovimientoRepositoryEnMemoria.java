package com.accel.finanzas.repository;

import com.accel.finanzas.model.Movimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class MovimientoRepositoryEnMemoria implements MovimientoRepository {

    private final Map<Long, Movimiento> almacen = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    public MovimientoRepositoryEnMemoria() {
        guardar(new Movimiento(null, "Almuerzo", new BigDecimal("25.50"), LocalDate.of(2026, 9, 15)));
        guardar(new Movimiento(null, "Taxi", new BigDecimal("12.00"), LocalDate.of(2026, 9, 16)));
        guardar(
                new Movimiento(
                        null, "Menú del viernes", new BigDecimal("18.90"), LocalDate.of(2026, 9, 17)));
    }

    @Override
    public List<Movimiento> buscarTodos() {
        return almacen.values().stream().sorted(Comparator.comparing(Movimiento::id)).toList();
    }

    @Override
    public Optional<Movimiento> buscarPorId(Long id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public Movimiento guardar(Movimiento movimiento) {
        Long id = movimiento.id() != null ? movimiento.id() : siguienteId.getAndIncrement();
        Movimiento guardado =
                new Movimiento(id, movimiento.descripcion(), movimiento.monto(), movimiento.fecha());
        almacen.put(id, guardado);
        return guardado;
    }

    @Override
    public boolean eliminar(Long id) {
        return almacen.remove(id) != null;
    }
}
