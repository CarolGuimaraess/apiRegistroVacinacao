package com.registro.vacinacao.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registro.vacinacao.dto.PacienteDosesAtrasadasDTO;
import com.registro.vacinacao.dto.PacienteDosesDTO;
import com.registro.vacinacao.exception.TratamentoErros;
import com.registro.vacinacao.service.PacienteService;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Data

@RestController
@RequestMapping("/paciente")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;
    @Autowired
    private TratamentoErros tratamentoDeErros;

    @GetMapping("/{pacienteId}/doses")
    public ResponseEntity<?> listarDosesPaciente(@PathVariable String pacienteId) {
        try {
            List<PacienteDosesDTO> dosesInfo = pacienteService.listarDosesDoPaciente(pacienteId);
            if (dosesInfo.isEmpty()) {

                int statusCode = HttpServletResponse.SC_NOT_FOUND;
                pacienteService.registrarLog("GET", "Listar doses de Pacientes", pacienteId, statusCode);
                String mensagem = "Doses não encontrada.";
                return tratamentoDeErros.criarRespostaDeErro(HttpStatus.valueOf(statusCode), mensagem);
            }

            int statusCode = HttpServletResponse.SC_OK;
            pacienteService.registrarLog("GET", "Listar doses de Pacientes", dosesInfo.toString(), statusCode);

            return ResponseEntity.ok(dosesInfo);
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            pacienteService.registrarLog("GET", "Listar doses de Pacientes", pacienteId, statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            pacienteService.registrarLog("GET", "Listar doses de Pacientes", pacienteId, statusCode);

            return tratamentoDeErros.lidarComErroDoServidor(e);
        }
    }

    @GetMapping("/doses/atrasadas")
    public ResponseEntity<?> listarPacientesComDosesAtrasadas(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam Map<String, String> requestParams) {
        try {
            if (requestParams.size() > 1 || (requestParams.size() == 1 && !requestParams.containsKey("estado"))) {
                int statusCode = HttpServletResponse.SC_BAD_REQUEST;
                pacienteService.registrarLog("GET", "Listar Pacientes com Doses Atrasadas", requestParams.toString(), statusCode);
                String mensagem = "Parâmetros não permitidos na solicitação.";
                return tratamentoDeErros.criarRespostaDeErro(HttpStatus.valueOf(statusCode), mensagem);
            }
            @NotNull List<PacienteDosesAtrasadasDTO> resposta = pacienteService.listarPacientesComDosesAtrasadas(estado);

            int statusCode = HttpServletResponse.SC_OK;
            pacienteService.registrarLog("GET", "Listar Pacientes com Doses Atrasadas", resposta.toString(), statusCode);

            return ResponseEntity.ok(resposta);
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            pacienteService.registrarLog("GET", "listar Pacientes Com Doses Atrasadas", requestParams.toString(), statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            pacienteService.registrarLog("GET", "Listar doses de Pacientes", requestParams.toString(), statusCode);

            return tratamentoDeErros.lidarComErroDoServidor(e);
        }
    }

    private String extrairMensagemDeErro(String responseBody) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            return jsonNode.path("mensagem").asText();
        } catch (JsonProcessingException e) {
            return responseBody;
        }
    }
}
