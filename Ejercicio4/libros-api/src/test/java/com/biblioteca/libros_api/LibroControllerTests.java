package com.biblioteca.libros_api;

import com.biblioteca.libros_api.controller.LibroController;
import com.biblioteca.libros_api.service.LibroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LibroControllerTests {
    private MockMvc mvc;

    private static final String ENTRADA = """
            {"titulo":"Libro de prueba","autor":"Ana López","isbn":"001-234",
             "anioPublicacion":2020,"estado":"DISPONIBLE"}
            """;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new LibroController(new LibroService())).build();
    }

    @Test
    void consultaCincoLibrosYBuscaPorTituloExacto() throws Exception {
        mvc.perform(get("/libros"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/libros/titulo/{titulo}", "El principito"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isbn").value("9780156012195"));
        mvc.perform(get("/libros/titulo/{titulo}", "el principito"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Libro no encontrado."));
        mvc.perform(get("/libros/titulo/{titulo}", "Cien años de soledad"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void registraActualizaYEliminaConIdGenerado() throws Exception {
        mvc.perform(post("/libros").contentType(MediaType.APPLICATION_JSON)
                        .content(ENTRADA.replace("{", "{\"id\":999,")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.isbn").value("001-234"));
        mvc.perform(put("/libros/6").contentType(MediaType.APPLICATION_JSON)
                        .content(ENTRADA.replace("DISPONIBLE", "PRESTADO").replace("{", "{\"id\":999,")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.estado").value("PRESTADO"));
        mvc.perform(get("/libros/titulo/{titulo}", "Libro de prueba"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PRESTADO"));
        mvc.perform(delete("/libros/6"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(delete("/libros/6")).andExpect(status().isNotFound());
        mvc.perform(get("/libros/titulo/{titulo}", "Libro de prueba"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/libros").contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void rechazaTitulosDuplicadosSinModificarRegistros() throws Exception {
        String duplicado = ENTRADA.replace("Libro de prueba", "El principito");
        mvc.perform(post("/libros").contentType(MediaType.APPLICATION_JSON).content(duplicado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un libro con ese título."));
        mvc.perform(put("/libros/2").contentType(MediaType.APPLICATION_JSON).content(duplicado))
                .andExpect(status().isConflict());
        mvc.perform(get("/libros/titulo/{titulo}", "Don Quijote de la Mancha"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2));
        mvc.perform(put("/libros/999").contentType(MediaType.APPLICATION_JSON).content(ENTRADA))
                .andExpect(status().isNotFound());
    }

    @Test
    void devuelveErroresJsonParaDatosInvalidos() throws Exception {
        String[] invalidos = {"{", "{}", "null", "[]", "",
                ENTRADA.replace("Libro de prueba", "   "),
                ENTRADA.replace("2020", "0"), ENTRADA.replace("2020", "10000"),
                ENTRADA.replace("2020", "2020.5"), ENTRADA.replace("2020", "\"2020\""),
                ENTRADA.replace("\"001-234\"", "1234"),
                ENTRADA.replace("DISPONIBLE", "OTRO"),
                ENTRADA.replace("\"autor\":\"Ana López\",", "")};
        for (String invalido : invalidos) {
            mvc.perform(post("/libros").contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Los datos de la solicitud no son válidos."));
        }
        for (String id : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
            mvc.perform(delete("/libros/" + id)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").exists());
        }
        mvc.perform(get("/libros/titulo/{titulo}", "   ")).andExpect(status().isBadRequest());
        mvc.perform(put("/libros/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelveArregloVacioAlEliminarTodos() throws Exception {
        for (int id = 1; id <= 5; id++) {
            mvc.perform(delete("/libros/" + id)).andExpect(status().isNoContent());
        }
        mvc.perform(get("/libros")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
