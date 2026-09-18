package com.accel.finanzas.service;

import com.accel.finanzas.client.TipoDeCambioClient;
import com.accel.finanzas.exception.MonedaNoSoportadaException;
import com.accel.finanzas.exception.MovimientoNoEncontradoException;
import com.accel.finanzas.model.Movimiento;
import com.accel.finanzas.model.Resumen;
import com.accel.finanzas.model.ResumenConvertido;
import com.accel.finanzas.repository.MovimientoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private static final String MONEDA_BASE = "PEN";

    private final MovimientoRepository repositorio;
    private final TipoDeCambioClient tipoDeCambio;

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

    public ResumenConvertido resumenEn(String moneda) {
        Resumen resumen = resumen();
        String destino = moneda.toUpperCase(Locale.ROOT);
        Map<String, BigDecimal> tasas = tipoDeCambio.tasas(MONEDA_BASE);
        BigDecimal tasa = tasas.get(destino);
        if (tasa == null) {
            throw new MonedaNoSoportadaException(destino);
        }
        BigDecimal convertido = resumen.total().multiply(tasa).setScale(2, RoundingMode.HALF_EVEN);
        return new ResumenConvertido(
                resumen.cantidad(), resumen.total(), destino, tasa, convertido);
    }
}
