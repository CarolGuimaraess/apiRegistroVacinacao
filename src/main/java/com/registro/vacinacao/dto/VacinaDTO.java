package com.registro.vacinacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VacinaDTO {

    private String fabricante;

    private String lote;

    private LocalDate dataDeValidade;

    private Integer numeroDeDoses;

    private Integer intervaloDeDoses;
}
