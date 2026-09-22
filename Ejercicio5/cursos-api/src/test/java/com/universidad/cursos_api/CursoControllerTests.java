package com.universidad.cursos_api;

import com.universidad.cursos_api.controller.CursoController;
import com.universidad.cursos_api.service.CursoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CursoControllerTests {
    private MockMvc mvc;
    private static final String ENTRADA = """
            {"nombre":"Curso de prueba","codigo":"PRUEBA-001","creditos":4,"estado":"ACTIVO"}
            """;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new CursoController(new CursoService())).build();
    }

    @Test
    void consultaCincoCursosYBuscaPorCodigoExacto() throws Exception {
        mvc.perform(get("/cursos")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/cursos/codigo/PROG-002")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Programación II"))
                .andExpect(jsonPath("$.codigo").value("PROG-002"))
                .andExpect(jsonPath("$.creditos").value(4))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        mvc.perform(get("/cursos/codigo/prog-002")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado."));
        mvc.perform(get("/cursos/1")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void creaActualizaYEliminaConIdGenerado() throws Exception {
        mvc.perform(post("/cursos").contentType(MediaType.APPLICATION_JSON)
                        .content(ENTRADA.replace("{", "{\"id\":999,")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(6));
        String actualizado = ENTRADA.replace("Curso de prueba", "Curso actualizado")
                .replace("PRUEBA-001", "NUEVO-001").replace("ACTIVO", "INACTIVO")
                .replace(":4", ":5").replace("{", "{\"id\":999,");
        mvc.perform(put("/cursos/6").contentType(MediaType.APPLICATION_JSON).content(actualizado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.nombre").value("Curso actualizado"))
                .andExpect(jsonPath("$.codigo").value("NUEVO-001"))
                .andExpect(jsonPath("$.creditos").value(5))
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(get("/cursos/codigo/PRUEBA-001")).andExpect(status().isNotFound());
        mvc.perform(get("/cursos/codigo/NUEVO-001")).andExpect(status().isOk());
        mvc.perform(put("/cursos/6").contentType(MediaType.APPLICATION_JSON).content(actualizado))
                .andExpect(status().isOk());
        mvc.perform(delete("/cursos/6")).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(delete("/cursos/6")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Curso no encontrado."));
        mvc.perform(get("/cursos/codigo/NUEVO-001")).andExpect(status().isNotFound());
        mvc.perform(post("/cursos").contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void rechazaDuplicadosYActualizacionDeCursoInexistente() throws Exception {
        String duplicado = ENTRADA.replace("PRUEBA-001", "PROG-002");
        mvc.perform(post("/cursos").contentType(MediaType.APPLICATION_JSON).content(duplicado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un curso con ese código."));
        mvc.perform(put("/cursos/2").contentType(MediaType.APPLICATION_JSON).content(duplicado))
                .andExpect(status().isConflict());
        mvc.perform(get("/cursos/codigo/MAT-002")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
        mvc.perform(put("/cursos/999").contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isNotFound());
        mvc.perform(get("/cursos")).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(post("/cursos").contentType(MediaType.APPLICATION_JSON)
                        .content(duplicado.replace("PROG-002", "prog-002")))
                .andExpect(status().isCreated());
    }

    @Test
    void validaCamposTiposYParametrosConErroresJson() throws Exception {
        String[] invalidos = {"{", "{}", "null", "[]", "",
                ENTRADA.replace("Curso de prueba", "   "),
                ENTRADA.replace("PRUEBA-001", "   "),
                ENTRADA.replace(":4", ":0"), ENTRADA.replace(":4", ":-1"),
                ENTRADA.replace(":4", ":4.5"), ENTRADA.replace(":4", ":\"4\""),
                ENTRADA.replace(":4", ":2147483648"), ENTRADA.replace(":4", ":null"),
                ENTRADA.replace("\"PRUEBA-001\"", "123"),
                ENTRADA.replace("ACTIVO", "OTRO"),
                ENTRADA.replace("\"nombre\":\"Curso de prueba\",", "")};
        for (String invalido : invalidos) {
            mvc.perform(post("/cursos").contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Los datos de la solicitud no son válidos."));
            mvc.perform(put("/cursos/1").contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        }
        for (String id : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
            mvc.perform(delete("/cursos/" + id)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").exists());
            mvc.perform(put("/cursos/" + id).contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").exists());
        }
        mvc.perform(get("/cursos/codigo/{codigo}", "   ")).andExpect(status().isBadRequest());
    }

    @Test
    void devuelveArregloVacioAlEliminarTodos() throws Exception {
        for (int id = 1; id <= 5; id++) {
            mvc.perform(delete("/cursos/" + id)).andExpect(status().isNoContent());
        }
        mvc.perform(get("/cursos")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
