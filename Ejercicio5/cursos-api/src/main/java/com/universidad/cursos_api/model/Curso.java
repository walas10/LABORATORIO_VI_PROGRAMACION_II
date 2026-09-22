package com.universidad.cursos_api.model;

public class Curso {
    private Long id;
    private String nombre;
    private String codigo;
    private Integer creditos;
    private String estado;

    public Curso() {
    }

    public Curso(Long id, String nombre, String codigo, Integer creditos, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.codigo = codigo;
        this.creditos = creditos;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Integer getCreditos() { return creditos; }
    public void setCreditos(Integer creditos) { this.creditos = creditos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
