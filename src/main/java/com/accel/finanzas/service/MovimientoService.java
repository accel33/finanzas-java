package com.accel.finanzas.service;

import com.accel.finanzas.exception.MovimientoNoEncontradoException;
import com.accel.finanzas.model.Movimiento;
import com.accel.finanzas.model.Resumen;
import com.accel.finanzas.repository.MovimientoRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository repositorio;

    public List<Movimiento> listar() {
        return repositorio.buscarTodos();
    }

    public Movimiento obtener(Long id) {
        return repositorio
                .buscarPorId(id)
                .orElseThrow(() -> new MovimientoNoEncontradoException(id));
    }

    public Movimiento crear(Movimiento nuevo) {
        Movimiento creado =
                repositorio.guardar(
                        new Movimiento(null, nuevo.descripcion(), nuevo.monto(), nuevo.fecha()));
        log.info("Movimiento creado id={} monto={}", creado.id(), creado.monto());
        return creado;
    }

    public Movimiento reemplazar(Long id, Movimiento datos) {
        obtener(id);
        return repositorio.guardar(
                new Movimiento(id, datos.descripcion(), datos.monto(), datos.fecha()));
    }

    public void eliminar(Long id) {
        if (!repositorio.eliminar(id)) {
            throw new MovimientoNoEncontradoException(id);
        }
        log.info("Movimiento eliminado id={}", id);
    }

    public Resumen resumen() {
        List<Movimiento> movimientos = repositorio.buscarTodos();
        BigDecimal total =
                movimientos.stream()
                        .map(Movimiento::monto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resumen(movimientos.size(), total);
    }
}
