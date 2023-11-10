package com.registroVacinacao.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @NotBlank(message = "Nome não pode estar em branco.")
    @NotNull(message = "Nome não pode ser nulo.")
    private String nomeProfissional;

    @NotBlank(message = "Sobrenome não pode estar em branco.")
    @NotNull(message = "Sobrenome não pode ser nulo.")
    private String sobrenomeProfissional;

    @NotNull(message = "Data não pode estar em branco.")
    @Past(message = "A data de vacinação não pode ser maior que a data atual.")
    private LocalDate dataVacinacao;

    @NotBlank(message = "CPF não pode estar em branco.")
    @NotNull(message = "CPF não pode ser nulo.")
    private String cpfProfissional;

    @NotBlank(message = "Paciente não pode estar em branco.")
    @NotNull(message = "Paciente não pode ser nulo.")
    private String identificacaoPaciente;

    @NotBlank(message = "Vacina não pode estar em branco.")
    @NotNull(message = "Vacina não pode ser nula.")
    private String identificacaoVacina;

    @NotBlank(message = "Dose não pode estar em branco.")
    @NotNull(message = "Dose não pode ser nula.")
    private String identificacaoDose;
}

