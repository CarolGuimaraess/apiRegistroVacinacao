package com.registro.vacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registro.vacinacao.entity.Log;
import com.registro.vacinacao.entity.RegistroVacinacao;
import com.registro.vacinacao.service.client.PacienteClientService;
import com.registro.vacinacao.service.client.VacinaClientService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class VacinaService {
    private final RegistroVacinacaoService registroVacinacaoService;
    private final PacienteClientService pacienteClientService;
    private final VacinaClientService vacinaClientService;
    private final CacheManager cacheManager;

    @Autowired
    public VacinaService(RegistroVacinacaoService registroVacinacaoService, PacienteClientService pacienteClientService, VacinaClientService vacinaClientService, CacheManager cacheManager) {
        this.registroVacinacaoService = registroVacinacaoService;
        this.pacienteClientService = pacienteClientService;
        this.vacinaClientService = vacinaClientService;
        this.cacheManager = cacheManager;
    }

    @Autowired
    private MongoTemplate mongoTemplate;


    public void registrarLog(String metodo, String acao, String mensagem, int statusCode) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String data = dateFormat.format(new Date());

        Log log = new Log();
        log.setTimestamp(data);
        log.setLevel("INFO");
        log.setMethod(metodo);
        log.setAction(acao);
        log.setStatusCode(statusCode);

        log.setMessage(mensagem);

        mongoTemplate.insert(log, "log");
    }
    @Cacheable("registroVacinacaoCache")
    public Map<String, Object> listarTotalVacinasAplicadas(String estado) {
        List<Map<String, Object>> registrosComPacientes = combinarRegistroComPaciente();

        int totalVacinasAplicadas = calcularTotalVacinasAplicadas(registrosComPacientes, estado);

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("totalVacinasAplicadas", totalVacinasAplicadas);

        return resposta;
    }

    private int calcularTotalVacinasAplicadas(@NotNull List<Map<String, Object>> registrosComPacientes, String estado) {
        int totalVacinasAplicadas = 0;

        for (Map<String, Object> registroComPaciente : registrosComPacientes) {
            JsonNode pacienteNode = (JsonNode) registroComPaciente.get("paciente");

            if (estadoValido(pacienteNode, estado)) {
                RegistroVacinacao registro = (RegistroVacinacao) registroComPaciente.get("registroVacinacao");
                if (registro != null) {
                    totalVacinasAplicadas++;
                }
            }
        }

        return totalVacinasAplicadas;
    }

    public Map<String, Object> listarVacinasAplicadasFabricante(String fabricante, String estado) {
        JsonNode dadosVacinas = vacinaClientService.listarTodasVacinas();
        List<Map<String, Object>> registrosComPacientes = combinarRegistroComPaciente();

        int totalPessoasVacinadas = calcularTotalPessoasVacinadas(registrosComPacientes, dadosVacinas, fabricante, estado);

        Map<String, Object> resultado = new HashMap<>();
        Map<String, Object> vacinaInfo = new HashMap<>();
        vacinaInfo.put("fabricante", fabricante);
        vacinaInfo.put("doses_aplicadas", totalPessoasVacinadas);
        resultado.put("vacina", vacinaInfo);

        return resultado;
    }

    private int calcularTotalPessoasVacinadas(List<Map<String, Object>> registrosComPacientes, @NotNull JsonNode dadosVacinas, String fabricante, String estado) {
        return (int) StreamSupport.stream(dadosVacinas.spliterator(), false)
                .filter(vacina -> fabricante.equals(vacina.get("fabricante").asText()))
                .map(vacina -> vacina.get("id").asText())
                .flatMap(id -> registrosComPacientes.stream()
                        .filter(registroComPaciente -> {
                            JsonNode pacienteNode = (JsonNode) registroComPaciente.get("paciente");
                            return estadoValido(pacienteNode, estado);
                        })
                ).count();
    }

    private boolean estadoValido(@NotNull JsonNode pacienteNode, String estado) {
        JsonNode enderecosNode = pacienteNode.path("enderecos");
        if (enderecosNode.isArray()) {
            for (JsonNode enderecoNode : enderecosNode) {
                JsonNode estadoNode = enderecoNode.path("estado");
                if (estadoNode.isTextual() && (estado == null || estado.isEmpty() || estadoNode.asText().trim().equalsIgnoreCase(estado.trim()))) {
                    return true;
                }
            }
        }
        return false;
    }

    List<Map<String, Object>> combinarRegistroComPaciente() {
        JsonNode dadosPacientes = pacienteClientService.listarTodosPacientes();
        List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

        return dadosRegistroVacinacao.stream()
                .map(registro -> {
                    String pacienteId = registro.getIdentificacaoPaciente();

                    Optional<JsonNode> pacienteCorrespondente = StreamSupport.stream(dadosPacientes.spliterator(), false)
                            .filter(pacienteNode -> pacienteNode.path("id").asText().equals(pacienteId))
                            .findFirst();

                    return pacienteCorrespondente.map(paciente -> {
                        Map<String, Object> registroComPaciente = new HashMap<>();
                        registroComPaciente.put("registroVacinacao", registro);
                        registroComPaciente.put("paciente", paciente);
                        return registroComPaciente;
                    });
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}