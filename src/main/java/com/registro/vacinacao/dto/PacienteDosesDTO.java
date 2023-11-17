package com.registro.vacinacao.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;

public class PacienteDosesDTO {
    public LocalDate dataVacinacao;
    public JsonNode paciente;
    public String identificacaoDose;
}