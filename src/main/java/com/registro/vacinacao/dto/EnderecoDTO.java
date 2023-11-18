package com.registro.vacinacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnderecoDTO {

    private String logradouro;

    private Integer numero;

    private String bairro;

    private String cep;

    private String municipio;

    private String estado;
}

