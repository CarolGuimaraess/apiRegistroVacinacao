package com.registro.vacinacao.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registro.vacinacao.exception.TratamentoErros;
import com.registro.vacinacao.service.VacinaService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data

@RestController
@RequestMapping("/vacina")
public class VacinaController {
    @Autowired
    private VacinaService vacinaService;
    @Autowired
    private TratamentoErros tratamentoDeErros;

    @GetMapping("/aplicadas/total")
    public ResponseEntity<?> listarTotalVacinasAplicadas(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam Map<String, String> requestParams) {

        try {
            if (requestParams.size() > 1 || (requestParams.size() == 1 && !requestParams.containsKey("estado"))) {

                int statusCode = HttpServletResponse.SC_BAD_REQUEST;
                vacinaService.registrarLog("GET", "Listar Total de Vacinas Aplicadas", requestParams.toString(), statusCode);

                String mensagem = "Parâmetros não permitidos na solicitação.";
                return tratamentoDeErros.criarRespostaDeErro(HttpStatus.valueOf(statusCode), mensagem);
            }
            List<Map<String, Object>> resposta = Collections.singletonList(vacinaService.listarTotalVacinasAplicadas(estado));

            int statusCode = HttpServletResponse.SC_OK;
            vacinaService.registrarLog("GET", "Listar Total de Vacinas Aplicadas", resposta.toString(), statusCode);

            return ResponseEntity.ok(resposta);
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            vacinaService.registrarLog("GET", "Listar Total de Vacinas Aplicadas", requestParams.toString(), statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            vacinaService.registrarLog("GET", "Listar Total de Vacinas Aplicadas", requestParams.toString(), statusCode);

            return tratamentoDeErros.lidarComErroDoServidor(e);
        }
    }

    @GetMapping("/aplicadas")
    public ResponseEntity<?> listarVacinasAplicadasFabricante(
            @RequestParam(name = "fabricante") String fabricante,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam Map<String, String> requestParams) {
        try {
            if (!requestParams.containsKey("fabricante") || requestParams.size() > 2 || (requestParams.size() == 2 && !requestParams.containsKey("estado"))) {

                int statusCode = HttpServletResponse.SC_BAD_REQUEST;
                vacinaService.registrarLog("GET", "Listar Total de Aplicadas", requestParams.toString(), statusCode);

                String mensagem = "Parâmetros não permitidos na solicitação.";
                return tratamentoDeErros.criarRespostaDeErro(HttpStatus.valueOf(statusCode), mensagem);
            }
            Map<String, Object> resposta = vacinaService.listarVacinasAplicadasFabricante(fabricante, estado);

            int statusCode = HttpServletResponse.SC_OK;
            vacinaService.registrarLog("GET", "Listar Vacinas Aplicadas", resposta.toString(), statusCode);

            return ResponseEntity.ok(resposta);
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            vacinaService.registrarLog("GET", "Listar Vacinas Aplicadas", requestParams.toString(), statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            vacinaService.registrarLog("GET", "Listar Vacinas Aplicadas", requestParams.toString(), statusCode);

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
