package com.hotel.reservas_api.service;

import com.hotel.reservas_api.model.Reserva;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
public class ReservaService {
    private static final Pattern TEXTO = Pattern.compile("\\S");
    private final List<Reserva> reservas = new ArrayList<>();
    private long siguienteId = 6;

    public ReservaService() {
        reservas.add(new Reserva(1L, "Ana López", "101", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), "CONFIRMADA"));
        reservas.add(new Reserva(2L, "Carlos Pérez", "102", LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 14), "CONFIRMADA"));
        reservas.add(new Reserva(3L, "María García", "103", LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 15), "CONFIRMADA"));
        reservas.add(new Reserva(4L, "Luis Ramírez", "201", LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 16), "CONFIRMADA"));
        reservas.add(new Reserva(5L, "Sofía Martínez", "202", LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 17), "CONFIRMADA"));
    }

    public synchronized Reserva crearReserva(Reserva entrada) {
        validarReserva(entrada);
        Reserva nueva = copiar(entrada);
        nueva.setId(siguienteId++);
        nueva.setEstado("CONFIRMADA");
        reservas.add(nueva);
        return copiar(nueva);
    }

    public synchronized List<Reserva> consultarReservas() {
        // Las copias impiden modificar el almacenamiento desde otras capas.
        return reservas.stream().map(this::copiar).toList();
    }

    public synchronized Reserva consultarReservaPorId(Long id) {
        validarId(id);
        return copiar(buscarPorId(id));
    }

    public synchronized Reserva actualizarReserva(Long id, Reserva entrada) {
        validarId(id);
        validarReserva(entrada);
        Reserva actual = buscarPorId(id);
        if ("CANCELADA".equals(actual.getEstado())) {
            throw new ReservaCanceladaException();
        }
        Reserva actualizada = copiar(entrada);
        actualizada.setId(id);
        actualizada.setEstado(actual.getEstado());
        reservas.set(reservas.indexOf(actual), actualizada);
        return copiar(actualizada);
    }

    public synchronized Reserva cancelarReserva(Long id) {
        validarId(id);
        Reserva reserva = buscarPorId(id);
        reserva.setEstado("CANCELADA");
        return copiar(reserva);
    }

    private Reserva buscarPorId(Long id) {
        return reservas.stream().filter(reserva -> reserva.getId().equals(id))
                .findFirst().orElseThrow(NoSuchElementException::new);
    }

    private void validarReserva(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException();
        }
        validarTexto(reserva.getNombreCliente());
        validarTexto(reserva.getHabitacion());
        if (reserva.getFechaEntrada() == null || reserva.getFechaSalida() == null
                || !reserva.getFechaSalida().isAfter(reserva.getFechaEntrada())) {
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

    private Reserva copiar(Reserva reserva) {
        return new Reserva(reserva.getId(), reserva.getNombreCliente(), reserva.getHabitacion(),
                reserva.getFechaEntrada(), reserva.getFechaSalida(), reserva.getEstado());
    }

    public static class ReservaCanceladaException extends RuntimeException {
    }
}
