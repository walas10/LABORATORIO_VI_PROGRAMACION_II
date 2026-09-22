package com.universidad.cursos_api.controller;

import com.universidad.cursos_api.model.Curso;
import com.universidad.cursos_api.service.CursoService;
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
@RequestMapping(value = "/cursos", produces = MediaType.APPLICATION_JSON_VALUE)
public class CursoController {
    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Curso> crearCurso(@RequestBody JsonNode cuerpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.crearCurso(leerEntrada(cuerpo)));
    }

    @GetMapping
    public ResponseEntity<List<Curso>> consultarCursos() {
        return ResponseEntity.ok(cursoService.consultarCursos());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Curso> consultarCursoPorCodigo(@PathVariable("codigo") String codigo) {
        return ResponseEntity.ok(cursoService.consultarCursoPorCodigo(codigo));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Curso> actualizarCurso(@PathVariable("id") Long id,
                                                @RequestBody JsonNode cuerpo) {
        return ResponseEntity.ok(cursoService.actualizarCurso(id, leerEntrada(cuerpo)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCurso(@PathVariable("id") Long id) {
        cursoService.eliminarCurso(id);
        return ResponseEntity.noContent().build();
    }

    // Comprueba los tipos JSON sin convertir números a texto ni truncar decimales.
    // Las reglas del curso y el almacenamiento se validan en el servicio.
    private Curso leerEntrada(JsonNode cuerpo) {
        if (cuerpo == null || !cuerpo.isObject()) {
            throw new IllegalArgumentException();
        }
        for (String campo : List.of("nombre", "codigo", "estado")) {
            if (!cuerpo.hasNonNull(campo) || !cuerpo.get(campo).isString()) {
                throw new IllegalArgumentException();
            }
        }
        JsonNode creditos = cuerpo.get("creditos");
        if (creditos == null || !creditos.isIntegralNumber() || !creditos.canConvertToInt()) {
            throw new IllegalArgumentException();
        }
        return new Curso(null, cuerpo.get("nombre").asString(), cuerpo.get("codigo").asString(),
                creditos.intValue(), cuerpo.get("estado").asString());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> solicitudInvalida() {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Los datos de la solicitud no son válidos."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> cursoNoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Curso no encontrado."));
    }

    @ExceptionHandler(CursoService.CodigoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> codigoDuplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "Ya existe un curso con ese código."));
    }
}
