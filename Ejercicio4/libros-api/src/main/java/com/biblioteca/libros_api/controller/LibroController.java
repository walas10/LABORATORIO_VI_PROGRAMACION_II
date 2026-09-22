package com.biblioteca.libros_api.controller;

import com.biblioteca.libros_api.model.Libro;
import com.biblioteca.libros_api.service.LibroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping(value = "/libros", produces = MediaType.APPLICATION_JSON_VALUE)
public class LibroController {
    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Libro> registrarLibro(@RequestBody JsonNode cuerpo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(libroService.registrarLibro(leerEntrada(cuerpo)));
    }

    @GetMapping
    public ResponseEntity<List<Libro>> consultarLibros() {
        return ResponseEntity.ok(libroService.consultarLibros());
    }

    @GetMapping("/titulo/{titulo}")
    public ResponseEntity<Libro> consultarLibroPorTitulo(@PathVariable("titulo") String titulo) {
        return ResponseEntity.ok(libroService.consultarLibroPorTitulo(titulo));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Libro> actualizarLibro(@PathVariable("id") Long id,
                                                @RequestBody JsonNode cuerpo) {
        return ResponseEntity.ok(libroService.actualizarLibro(id, leerEntrada(cuerpo)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarLibro(@PathVariable("id") Long id) {
        libroService.eliminarLibro(id);
        return ResponseEntity.noContent().build();
    }

    // Verifica los tipos JSON sin convertir números en textos ni truncar decimales.
    // Las reglas del libro y el almacenamiento pertenecen al servicio.
    private Libro leerEntrada(JsonNode cuerpo) {
        if (cuerpo == null || !cuerpo.isObject()) {
            throw new IllegalArgumentException();
        }
        for (String campo : List.of("titulo", "autor", "isbn", "estado")) {
            if (!cuerpo.hasNonNull(campo) || !cuerpo.get(campo).isString()) {
                throw new IllegalArgumentException();
            }
        }
        JsonNode anio = cuerpo.get("anioPublicacion");
        if (anio == null || !anio.isIntegralNumber() || !anio.canConvertToInt()) {
            throw new IllegalArgumentException();
        }
        return new Libro(null, cuerpo.get("titulo").asString(), cuerpo.get("autor").asString(),
                cuerpo.get("isbn").asString(), anio.intValue(), cuerpo.get("estado").asString());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> solicitudInvalida() {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Los datos de la solicitud no son válidos."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> libroNoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Libro no encontrado."));
    }

    @ExceptionHandler(LibroService.TituloDuplicadoException.class)
    public ResponseEntity<Map<String, String>> tituloDuplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "Ya existe un libro con ese título."));
    }
}
