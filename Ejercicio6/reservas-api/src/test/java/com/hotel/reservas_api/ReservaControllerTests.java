package com.hotel.reservas_api;

import com.hotel.reservas_api.controller.ReservaController;
import com.hotel.reservas_api.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservaControllerTests {
    private MockMvc mvc;
    private static final String ENTRADA = """
            {"nombreCliente":"Ana López","habitacion":"101",
             "fechaEntrada":"2026-10-10","fechaSalida":"2026-10-12"}
            """;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ReservaController(new ReservaService())).build();
    }

    @Test
    void consultaCincoReservasYBuscaPorId() throws Exception {
        mvc.perform(get("/reservas")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/reservas/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombreCliente").value("Ana López"))
                .andExpect(jsonPath("$.habitacion").value("101"))
                .andExpect(jsonPath("$.fechaEntrada").value("2026-10-10"))
                .andExpect(jsonPath("$.fechaSalida").value("2026-10-12"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void creaYActualizaConIdYEstadoAdministradosPorServidor() throws Exception {
        String cuerpo = ENTRADA.replace("{", "{\"id\":999,\"estado\":\"CANCELADA\",");
        mvc.perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        String actualizado = cuerpo.replace("Ana López", "Pedro Pérez").replace("101", "A-02")
                .replace("2026-10-10", "2026-11-01").replace("2026-10-12", "2026-11-05");
        mvc.perform(put("/reservas/6").contentType(MediaType.APPLICATION_JSON).content(actualizado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.nombreCliente").value("Pedro Pérez"))
                .andExpect(jsonPath("$.habitacion").value("A-02"))
                .andExpect(jsonPath("$.fechaEntrada").value("2026-11-01"))
                .andExpect(jsonPath("$.fechaSalida").value("2026-11-05"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        mvc.perform(get("/reservas/6")).andExpect(jsonPath("$.nombreCliente").value("Pedro Pérez"));
    }

    @Test
    void cancelarConservaDatosYEsIdempotente() throws Exception {
        String cancelada = """
                {"id":1,"nombreCliente":"Ana López","habitacion":"101",
                 "fechaEntrada":"2026-10-10","fechaSalida":"2026-10-12","estado":"CANCELADA"}
                """;
        for (int intento = 0; intento < 2; intento++) {
            mvc.perform(delete("/reservas/1")).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(cancelada));
        }
        mvc.perform(get("/reservas/1")).andExpect(status().isOk()).andExpect(content().json(cancelada));
        mvc.perform(get("/reservas")).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].estado").value("CANCELADA"));
        mvc.perform(put("/reservas/1").contentType(MediaType.APPLICATION_JSON)
                        .content(ENTRADA.replace("Ana López", "Otro cliente")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede actualizar una reserva cancelada."));
        mvc.perform(get("/reservas/1")).andExpect(content().json(cancelada));
    }

    @Test
    void devuelve404SinCrearReservasAlActualizar() throws Exception {
        mvc.perform(get("/reservas/999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Reserva no encontrada."));
        mvc.perform(delete("/reservas/999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Reserva no encontrada."));
        mvc.perform(put("/reservas/999").contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje").exists());
        mvc.perform(get("/reservas")).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void validaCamposFechasYTiposJson() throws Exception {
        String[] invalidos = {"{", "{}", "null", "[]", "",
                ENTRADA.replace("Ana López", "   "), ENTRADA.replace("101", "   "),
                ENTRADA.replace("\"101\"", "101"),
                ENTRADA.replace("\"nombreCliente\":\"Ana López\",", ""),
                ENTRADA.replace("\"2026-10-10\"", "null"),
                ENTRADA.replace("2026-10-10", "2026-02-29"),
                ENTRADA.replace("2026-10-12", "2026-11-31"),
                ENTRADA.replace("2026-10-10", "10/10/2026"),
                ENTRADA.replace("2026-10-10", "2026-1-1"),
                ENTRADA.replace("2026-10-10", "2026-10-10T12:00:00"),
                ENTRADA.replace("2026-10-12", "2026-10-10"),
                ENTRADA.replace("2026-10-12", "2026-10-09")};
        for (String invalido : invalidos) {
            mvc.perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Los datos de la solicitud no son válidos."));
            mvc.perform(put("/reservas/1").contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        }
        mvc.perform(get("/reservas")).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void validaIdEnTodasLasOperacionesIndividuales() throws Exception {
        for (String id : new String[]{"0", "-1", "abc", "1.5", "9223372036854775808"}) {
            mvc.perform(get("/reservas/" + id)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").exists());
            mvc.perform(delete("/reservas/" + id)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").exists());
            mvc.perform(put("/reservas/" + id).contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        }
    }

    @Test
    void permiteFechaBisiestaYReservasSuperpuestasSegunContrato() throws Exception {
        String bisiesta = ENTRADA.replace("2026-10-10", "2028-02-29")
                .replace("2026-10-12", "2028-03-01");
        mvc.perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(bisiesta))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.fechaEntrada").value("2028-02-29"));
        mvc.perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isCreated());
    }
}
