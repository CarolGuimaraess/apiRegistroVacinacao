package com.registro.vacinacao.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registro.vacinacao.dto.ErrorDTO;
import com.registro.vacinacao.entity.RegistroVacinacao;
import com.registro.vacinacao.exception.TratamentoErros;
import com.registro.vacinacao.service.RegistroVacinacaoService;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data

@RestController
@RequestMapping("/registro-vacinacao")
public class RegistroVacinacaoController {

    @Autowired
    RegistroVacinacaoService registroVacinacaoService;
    @Autowired
    private final TratamentoErros tratamentoDeErros;

    @GetMapping
    public ResponseEntity<List<RegistroVacinacao>> listarRegistroVacinacao() {
        int statusCode = HttpServletResponse.SC_OK;
        registroVacinacaoService.registrarLog("GET", "Listar Registro de Vacinação", registroVacinacaoService.listarRegistroVacinacao().toString(), statusCode);
        return ResponseEntity.ok().body(registroVacinacaoService.listarRegistroVacinacao());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarRegistroVacinacao(@PathVariable String id) {
        try {
            RegistroVacinacao registroVacinacao = registroVacinacaoService.buscarRegistroVacinacao(id);
            if (registroVacinacao != null) {
                int statusCode = HttpServletResponse.SC_OK;
                registroVacinacaoService.registrarLog("GET", "Listar Registro de Vacinação por id", registroVacinacao.toString(), statusCode);

                return ResponseEntity.ok().body(registroVacinacao);
            } else {
                int statusCode = HttpServletResponse.SC_NOT_FOUND;
                registroVacinacaoService.registrarLog("GET", "Listar Registro de Vacinação por id", "Registro de Vacinação não encontrado", statusCode);

                return tratamentoDeErros.criarRespostaDeErro(HttpStatus.NOT_FOUND, "Registro de Vacinação não encontrado");
            }

        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("GET", "Listar Registro de Vacinação por id", e.getMessage(), statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("GET", "Listar Registro de Vacinação por id", e.getMessage(), statusCode);

            return tratamentoDeErros.lidarComErroDoServidor(e);
        }
    }

    @PostMapping
    public ResponseEntity<?> criarRegistroVacinacao(@RequestBody @Valid RegistroVacinacao registroVacinacao, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                List<String> erros = bindingResult
                        .getAllErrors()
                        .stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.toList());

                int statusCode = HttpServletResponse.SC_BAD_REQUEST;
                registroVacinacaoService.registrarLog("POST", "Criar Registro de Vacinação", erros.toString(), statusCode);

                return ResponseEntity.badRequest().body(erros.toArray());
            }

            Map<String, Object> resultado = registroVacinacaoService.criarRegistroVacinacao(registroVacinacao);
            int statusCode = (int) resultado.get("status");
            return HttpStatus.OK.value() == statusCode
                    ? ResponseEntity.created(null).body(resultado.get("registroVacinacao"))
                    : ResponseEntity.status(statusCode).body(new ErrorDTO((String) resultado.get("mensagem")));        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("POST", "Criar Registro de Vacinação", registroVacinacao.toString(), statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("POST", "Criar Registro de Vacinação", registroVacinacao.toString(), statusCode);

            return tratamentoDeErros.lidarComErroDoServidor(e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarRegistroVacinacao(
            @PathVariable String id,
            @RequestBody @Valid RegistroVacinacao registroVacinacao,
            @NotNull BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            int statusCode = HttpServletResponse.SC_BAD_REQUEST;
            registroVacinacaoService.registrarLog("PUT", "Erro ao atualizar Registro de Vacinação", "Objeto: " + registroVacinacao, statusCode);

            return ResponseEntity.badRequest().body(bindingResult
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage).toArray());
        }

        try {
            Map<String, Object> respostaServico = registroVacinacaoService.atualizarRegistroVacinacao(id, registroVacinacao);

            int statusCode = (int) respostaServico.get("status");
            registroVacinacaoService.registrarLog("PUT", "Atualizar Registro de Vacinação", respostaServico.get("mensagem").toString(), statusCode);

            return ResponseEntity.status(statusCode).body(
                    respostaServico.get("status").equals(HttpStatus.OK.value()) ?
                            respostaServico.get("registroVacinacao") : respostaServico
            );

        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("PUT", "Atualizar Registro de Vacinação", e.getMessage(), statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("PUT", "Atualizar Registro de Vacinação", e.getMessage(), statusCode);
            return tratamentoDeErros.lidarComErroDoServidor(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirRegistroVacinacao(@PathVariable String id) {
        try {
            Map<String, Object> resultado = registroVacinacaoService.excluirRegistroVacinacao(id);

            if (resultado.get("status").equals(HttpStatus.NO_CONTENT.value())) {
                int statusCode = HttpServletResponse.SC_NO_CONTENT;
                registroVacinacaoService.registrarLog("DELETE", "Deletar Registro de Vacinação", id, statusCode);
                return ResponseEntity.noContent().build();
            } else {
                int statusCode = HttpServletResponse.SC_BAD_REQUEST; // ou outro código de status apropriado
                registroVacinacaoService.registrarLog("DELETE", "Deletar Registro de Vacinação", id, statusCode);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
            }
        } catch (HttpClientErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("DELETE", "Deletar Registro de Vacinação", id, statusCode);
            String errorMessage = extrairMensagemDeErro(e.getResponseBodyAsString());

            return tratamentoDeErros.criarRespostaDeErro(e.getStatusCode(), errorMessage);
        } catch (HttpServerErrorException e) {
            int statusCode = e.getRawStatusCode();
            registroVacinacaoService.registrarLog("DELETE", "Deletar Registro de Vacinação", id, statusCode);
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