package com.accel.finanzas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accel.finanzas.exception.MovimientoNoEncontradoException;
import com.accel.finanzas.model.Movimiento;
import com.accel.finanzas.service.MovimientoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MovimientoController.class)
class MovimientoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MovimientoService servicio;

    @Test
    @DisplayName("GET de un id inexistente devuelve 404 en formato ProblemDetail")
    void devuelve404ConProblemDetail() throws Exception {
        given(servicio.obtener(99L)).willThrow(new MovimientoNoEncontradoException(99L));

        mockMvc.perform(get("/movimientos/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Movimiento no encontrado"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST con datos inválidos devuelve 400 y detalla cada campo")
    void rechazaDatosInvalidos() throws Exception {
        String cuerpo =
                """
                {"descripcion":"","monto":"-5","fecha":"2030-01-01"}
                """;

        mockMvc.perform(post("/movimientos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.descripcion").exists())
                .andExpect(jsonPath("$.errores.monto").exists())
                .andExpect(jsonPath("$.errores.fecha").exists());
    }

    @Test
    @DisplayName("POST válido devuelve 201 con la cabecera Location")
    void creaYDevuelve201() throws Exception {
        given(servicio.crear(any()))
                .willReturn(
                        new Movimiento(
                                7L, "Café", new BigDecimal("8.50"), LocalDate.of(2026, 9, 17)));

        String cuerpo =
                """
                {"descripcion":"Café","monto":"8.50","fecha":"2026-09-17"}
                """;

        mockMvc.perform(post("/movimientos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/movimientos/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("DELETE de un id inexistente devuelve 404")
    void eliminarInexistenteDevuelve404() throws Exception {
        willThrow(new MovimientoNoEncontradoException(99L)).given(servicio).eliminar(99L);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                                "/movimientos/99"))
                .andExpect(status().isNotFound());
    }
}
