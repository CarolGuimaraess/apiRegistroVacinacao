package com.registro.vacinacao.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PacienteDTO {

    private String id;

    private String nome;

    private String sobrenome;

    private String cpf;

    private LocalDate dataDeNascimento;

    private String genero;

    private List<String> contatos;

    private List<EnderecoDTO> enderecos;


}
