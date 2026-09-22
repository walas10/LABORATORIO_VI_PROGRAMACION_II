package com.hotel.reservas_api.controller;

import com.hotel.reservas_api.model.Reserva;
import com.hotel.reservas_api.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping(value = "/reservas", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReservaController {
    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Reserva> crearReserva(@RequestBody JsonNode cuerpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crearReserva(leerEntrada(cuerpo)));
    }

    @GetMapping
    public ResponseEntity<List<Reserva>> consultarReservas() {
        return ResponseEntity.ok(reservaService.consultarReservas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> consultarReservaPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(reservaService.consultarReservaPorId(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Reserva> actualizarReserva(@PathVariable("id") Long id,
                                                    @RequestBody JsonNode cuerpo) {
        return ResponseEntity.ok(reservaService.actualizarReserva(id, leerEntrada(cuerpo)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Reserva> cancelarReserva(@PathVariable("id") Long id) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id));
    }

    // Comprueba los tipos y el formato JSON. El servicio aplica las reglas de la reserva.
    private Reserva leerEntrada(JsonNode cuerpo) {
        if (cuerpo == null || !cuerpo.isObject()) {
            throw new IllegalArgumentException();
        }
        for (String campo : List.of("nombreCliente", "habitacion", "fechaEntrada", "fechaSalida")) {
            if (!cuerpo.hasNonNull(campo) || !cuerpo.get(campo).isString()) {
                throw new IllegalArgumentException();
            }
        }
        return new Reserva(null, cuerpo.get("nombreCliente").asString(), cuerpo.get("habitacion").asString(),
                leerFecha(cuerpo.get("fechaEntrada").asString()),
                leerFecha(cuerpo.get("fechaSalida").asString()), null);
    }

    private LocalDate leerFecha(String fecha) {
        if (!fecha.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException();
        }
        return LocalDate.parse(fecha);
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> solicitudInvalida() {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Los datos de la solicitud no son válidos."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> reservaNoEncontrada() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Reserva no encontrada."));
    }

    @ExceptionHandler(ReservaService.ReservaCanceladaException.class)
    public ResponseEntity<Map<String, String>> reservaCancelada() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", "No se puede actualizar una reserva cancelada."));
    }
}
