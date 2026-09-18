package com.accel.finanzas.controller;

import com.accel.finanzas.dto.MovimientoRequest;
import com.accel.finanzas.dto.MovimientoResponse;
import com.accel.finanzas.dto.ResumenResponse;
import com.accel.finanzas.model.Movimiento;
import com.accel.finanzas.service.MovimientoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService servicio;

    @GetMapping
    public List<MovimientoResponse> listar() {
        return servicio.listar().stream().map(MovimientoResponse::desde).toList();
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return ResumenResponse.desde(servicio.resumen());
    }

    @GetMapping("/{id}")
    public MovimientoResponse obtener(@PathVariable Long id) {
        return MovimientoResponse.desde(servicio.obtener(id));
    }

    @PostMapping
    public ResponseEntity<MovimientoResponse> crear(@Valid @RequestBody MovimientoRequest peticion) {
        Movimiento creado = servicio.crear(aModelo(peticion));
        return ResponseEntity.created(URI.create("/movimientos/" + creado.id()))
                .body(MovimientoResponse.desde(creado));
    }

    @PutMapping("/{id}")
    public MovimientoResponse reemplazar(
            @PathVariable Long id, @Valid @RequestBody MovimientoRequest peticion) {
        return MovimientoResponse.desde(servicio.reemplazar(id, aModelo(peticion)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    private static Movimiento aModelo(MovimientoRequest peticion) {
        return new Movimiento(null, peticion.descripcion(), peticion.monto(), peticion.fecha());
    }
}
