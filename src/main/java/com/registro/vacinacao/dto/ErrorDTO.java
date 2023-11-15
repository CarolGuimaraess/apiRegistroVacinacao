package com.registro.vacinacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDTO {
    public int status;
    public String mensagem;
}
