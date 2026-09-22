package com.universidad.cursos_api.service;

import com.universidad.cursos_api.model.Curso;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
public class CursoService {
    private static final Pattern TEXTO = Pattern.compile("\\S");
    private final List<Curso> cursos = new ArrayList<>();
    private long siguienteId = 6;

    public CursoService() {
        cursos.add(new Curso(1L, "Programación II", "PROG-002", 4, "ACTIVO"));
        cursos.add(new Curso(2L, "Matemática II", "MAT-002", 5, "ACTIVO"));
        cursos.add(new Curso(3L, "Física I", "FIS-001", 4, "ACTIVO"));
        cursos.add(new Curso(4L, "Estadística I", "EST-001", 3, "INACTIVO"));
        cursos.add(new Curso(5L, "Microeconomía", "ECO-001", 3, "ACTIVO"));
    }

    public synchronized Curso crearCurso(Curso entrada) {
        validarCurso(entrada);
        validarCodigoUnico(entrada.getCodigo(), null);
        Curso nuevo = copiar(entrada);
        nuevo.setId(siguienteId++);
        cursos.add(nuevo);
        return copiar(nuevo);
    }

    public synchronized List<Curso> consultarCursos() {
        // Las copias evitan que otras capas modifiquen la lista o sus elementos.
        return cursos.stream().map(this::copiar).toList();
    }

    public synchronized Curso consultarCursoPorCodigo(String codigo) {
        validarTexto(codigo);
        return cursos.stream().filter(curso -> curso.getCodigo().equals(codigo))
                .findFirst().map(this::copiar).orElseThrow(NoSuchElementException::new);
    }

    public synchronized Curso actualizarCurso(Long id, Curso entrada) {
        validarId(id);
        validarCurso(entrada);
        Curso actual = buscarPorId(id);
        validarCodigoUnico(entrada.getCodigo(), id);
        Curso actualizado = copiar(entrada);
        actualizado.setId(id);
        cursos.set(cursos.indexOf(actual), actualizado);
        return copiar(actualizado);
    }

    public synchronized void eliminarCurso(Long id) {
        validarId(id);
        cursos.remove(buscarPorId(id));
    }

    private Curso buscarPorId(Long id) {
        return cursos.stream().filter(curso -> curso.getId().equals(id))
                .findFirst().orElseThrow(NoSuchElementException::new);
    }

    private void validarCodigoUnico(String codigo, Long idActual) {
        if (cursos.stream().anyMatch(curso -> curso.getCodigo().equals(codigo)
                && !curso.getId().equals(idActual))) {
            throw new CodigoDuplicadoException();
        }
    }

    private void validarCurso(Curso curso) {
        if (curso == null) {
            throw new IllegalArgumentException();
        }
        validarTexto(curso.getNombre());
        validarTexto(curso.getCodigo());
        if (curso.getCreditos() == null || curso.getCreditos() < 1
                || !("ACTIVO".equals(curso.getEstado()) || "INACTIVO".equals(curso.getEstado()))) {
            throw new IllegalArgumentException();
        }
    }

    private void validarTexto(String texto) {
        if (texto == null || !TEXTO.matcher(texto).find()) {
            throw new IllegalArgumentException();
        }
    }

    private void validarId(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException();
        }
    }

    private Curso copiar(Curso curso) {
        return new Curso(curso.getId(), curso.getNombre(), curso.getCodigo(),
                curso.getCreditos(), curso.getEstado());
    }

    public static class CodigoDuplicadoException extends RuntimeException {
    }
}
