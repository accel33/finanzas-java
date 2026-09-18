package com.accel.finanzas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.accel.finanzas.exception.MovimientoNoEncontradoException;
import com.accel.finanzas.model.Movimiento;
import com.accel.finanzas.model.Resumen;
import com.accel.finanzas.repository.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

    @Mock private MovimientoRepository repositorio;

    @Captor private ArgumentCaptor<Movimiento> capturado;

    private MovimientoService servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new MovimientoService(repositorio);
    }

    @Test
    @DisplayName("obtener lanza excepción cuando el id no existe")
    void obtenerLanzaExcepcionCuandoNoExiste() {
        given(repositorio.buscarPorId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L))
                .isInstanceOf(MovimientoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("el resumen suma los montos sin perder centavos")
    void resumenSumaLosMontos() {
        given(repositorio.buscarTodos())
                .willReturn(
                        List.of(
                                movimiento(1L, "Almuerzo", "25.50"),
                                movimiento(2L, "Taxi", "12.00"),
                                movimiento(3L, "Café", "0.01")));

        Resumen resumen = servicio.resumen();

        assertThat(resumen.cantidad()).isEqualTo(3);
        assertThat(resumen.total()).isEqualByComparingTo(new BigDecimal("37.51"));
    }

    @Test
    @DisplayName("crear ignora el id que llegue y deja que lo asigne el repositorio")
    void crearIgnoraElIdRecibido() {
        given(repositorio.guardar(any())).willReturn(movimiento(1L, "Café", "8.50"));

        servicio.crear(movimiento(999L, "Café", "8.50"));

        verify(repositorio).guardar(capturado.capture());
        assertThat(capturado.getValue().id()).isNull();
    }

    @Test
    @DisplayName("eliminar lanza excepción si el repositorio no borró nada")
    void eliminarLanzaExcepcionSiNoExiste() {
        given(repositorio.eliminar(99L)).willReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(99L))
                .isInstanceOf(MovimientoNoEncontradoException.class);
    }

    private static Movimiento movimiento(Long id, String descripcion, String monto) {
        return new Movimiento(id, descripcion, new BigDecimal(monto), LocalDate.of(2026, 9, 17));
    }
}
