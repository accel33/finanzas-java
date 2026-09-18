package com.accel.finanzas.repository;

import com.accel.finanzas.model.Movimiento;
import java.util.List;
import java.util.Optional;

public interface MovimientoRepository {

    List<Movimiento> buscarTodos();

    Optional<Movimiento> buscarPorId(Long id);

    Movimiento guardar(Movimiento movimiento);

    boolean eliminar(Long id);
}
