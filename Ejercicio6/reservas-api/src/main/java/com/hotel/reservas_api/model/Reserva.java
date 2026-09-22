package com.hotel.reservas_api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class Reserva {
    private Long id;
    private String nombreCliente;
    private String habitacion;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
    private LocalDate fechaEntrada;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
    private LocalDate fechaSalida;
    private String estado;

    public Reserva() {
    }

    public Reserva(Long id, String nombreCliente, String habitacion,
                   LocalDate fechaEntrada, LocalDate fechaSalida, String estado) {
        this.id = id;
        this.nombreCliente = nombreCliente;
        this.habitacion = habitacion;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getHabitacion() { return habitacion; }
    public void setHabitacion(String habitacion) { this.habitacion = habitacion; }
    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }
    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
