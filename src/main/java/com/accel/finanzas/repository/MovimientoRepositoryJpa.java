package com.accel.finanzas.repository;

import com.accel.finanzas.model.Movimiento;
import com.accel.finanzas.model.MovimientoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class MovimientoRepositoryJpa implements MovimientoRepository {

    private final MovimientoJpaRepository jpa;

    public MovimientoRepositoryJpa(MovimientoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Movimiento> buscarTodos() {
        return jpa.findAll(Sort.by("id")).stream().map(MovimientoRepositoryJpa::aModelo).toList();
    }

    @Override
    public Optional<Movimiento> buscarPorId(Long id) {
        return jpa.findById(id).map(MovimientoRepositoryJpa::aModelo);
    }

    @Override
    public Movimiento guardar(Movimiento movimiento) {
        return aModelo(jpa.save(aEntidad(movimiento)));
    }

    @Override
    public boolean eliminar(Long id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    private static Movimiento aModelo(MovimientoEntity entidad) {
        return new Movimiento(
                entidad.getId(), entidad.getDescripcion(), entidad.getMonto(), entidad.getFecha());
    }

    private static MovimientoEntity aEntidad(Movimiento movimiento) {
        return new MovimientoEntity(
                movimiento.id(), movimiento.descripcion(), movimiento.monto(), movimiento.fecha());
    }
}
