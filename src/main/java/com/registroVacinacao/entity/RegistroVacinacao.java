package com.registroVacinacao.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "registro")
public class RegistroVacinacao {
    @Id
    private String id;

    @NotBlank(message = "Nome não pode ser nulo e não pode estar em branco.")
    private String nomeProfissional;

    @NotBlank(message = "Sobrenome não pode ser nulo e não pode estar em branco.")
    private String sobrenomeProfissional;

    @NotNull(message = "Data não pode estar em branco.")
    @Past(message = "A data de vacinação não pode ser maior que a data atual.")
    private LocalDate dataVacinacao;

    @NotBlank(message = "CPF não pode ser nulo e não pode estar em branco.")
    @CPF
    private String cpfProfissional;

    @NotBlank(message = "Paciente não pode ser nulo e não pode estar em branco.")
    private String identificacaoPaciente;

    @NotBlank(message = "Vacina não pode ser nula e não pode estar em branco.")
    private String identificacaoVacina;

    @NotBlank(message = "Dose não pode ser nula e não pode estar em branco.")
    private String identificacaoDose;
}

