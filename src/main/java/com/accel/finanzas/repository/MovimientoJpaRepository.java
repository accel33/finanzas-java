package com.accel.finanzas.repository;

import com.accel.finanzas.model.MovimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoJpaRepository extends JpaRepository<MovimientoEntity, Long> {}
