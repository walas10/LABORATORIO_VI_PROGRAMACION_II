package com.biblioteca.libros_api.service;

import com.biblioteca.libros_api.model.Libro;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
public class LibroService {
    private static final Pattern TEXTO = Pattern.compile("\\S");
    private final List<Libro> libros = new ArrayList<>();
    private long siguienteId = 6;

    public LibroService() {
        libros.add(new Libro(1L, "El principito", "Antoine de Saint-Exupéry", "9780156012195", 1943, "DISPONIBLE"));
        libros.add(new Libro(2L, "Don Quijote de la Mancha", "Miguel de Cervantes", "9788420412146", 1605, "DISPONIBLE"));
        libros.add(new Libro(3L, "Cien años de soledad", "Gabriel García Márquez", "9780307474728", 1967, "PRESTADO"));
        libros.add(new Libro(4L, "1984", "George Orwell", "9780451524935", 1949, "DISPONIBLE"));
        libros.add(new Libro(5L, "La metamorfosis", "Franz Kafka", "9788420651368", 1915, "PRESTADO"));
    }

    public synchronized Libro registrarLibro(Libro entrada) {
        validarLibro(entrada);
        validarTituloUnico(entrada.getTitulo(), null);
        Libro nuevo = copiar(entrada);
        nuevo.setId(siguienteId++);
        libros.add(nuevo);
        return copiar(nuevo);
    }

    public synchronized List<Libro> consultarLibros() {
        // Las copias impiden modificar el almacenamiento desde otras capas.
        return libros.stream().map(this::copiar).toList();
    }

    public synchronized Libro consultarLibroPorTitulo(String titulo) {
        validarTexto(titulo);
        return libros.stream()
                .filter(libro -> libro.getTitulo().equals(titulo))
                .findFirst().map(this::copiar)
                .orElseThrow(NoSuchElementException::new);
    }

    public synchronized Libro actualizarLibro(Long id, Libro entrada) {
        validarId(id);
        validarLibro(entrada);
        Libro actual = buscarPorId(id);
        validarTituloUnico(entrada.getTitulo(), id);
        Libro actualizado = copiar(entrada);
        actualizado.setId(id);
        libros.set(libros.indexOf(actual), actualizado);
        return copiar(actualizado);
    }

    public synchronized void eliminarLibro(Long id) {
        validarId(id);
        libros.remove(buscarPorId(id));
    }

    private Libro buscarPorId(Long id) {
        return libros.stream().filter(libro -> libro.getId().equals(id))
                .findFirst().orElseThrow(NoSuchElementException::new);
    }

    private void validarTituloUnico(String titulo, Long idActual) {
        if (libros.stream().anyMatch(libro -> libro.getTitulo().equals(titulo)
                && !libro.getId().equals(idActual))) {
            throw new TituloDuplicadoException();
        }
    }

    private void validarLibro(Libro libro) {
        if (libro == null) {
            throw new IllegalArgumentException();
        }
        validarTexto(libro.getTitulo());
        validarTexto(libro.getAutor());
        validarTexto(libro.getIsbn());
        if (libro.getAnioPublicacion() == null || libro.getAnioPublicacion() < 1
                || libro.getAnioPublicacion() > 9999
                || !("DISPONIBLE".equals(libro.getEstado()) || "PRESTADO".equals(libro.getEstado()))) {
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

    private Libro copiar(Libro libro) {
        return new Libro(libro.getId(), libro.getTitulo(), libro.getAutor(),
                libro.getIsbn(), libro.getAnioPublicacion(), libro.getEstado());
    }

    public static class TituloDuplicadoException extends RuntimeException {
    }
}
