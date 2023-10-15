package com.registroVacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.repository.RegistroVacinacaoRepository;
import com.registroVacinacao.wbservice.PacienteWBService;
import com.registroVacinacao.wbservice.VacinaWBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VacinaService {
    private final RegistroVacinacaoService registroVacinacaoService;
    private final PacienteWBService pacienteWBService;
    private final VacinaWBService vacinaWBService;

    @Autowired
    public VacinaService(RegistroVacinacaoService registroVacinacaoService, PacienteWBService pacienteWBService, VacinaWBService vacinaWBService, CacheManager cacheManager) {
        this.registroVacinacaoService = registroVacinacaoService;
        this.pacienteWBService = pacienteWBService;
        this.vacinaWBService = vacinaWBService;
        this.cacheManager = cacheManager;
    }

    private final CacheManager cacheManager;

    @Cacheable("registroVacinacaoCache")
    public List<Map<String, Object>> listarDosesDoPaciente(String pacienteId) {
        JsonNode dadosPacientes = pacienteWBService.buscarPaciente(pacienteId);

        List<RegistroVacinacao> todosOsRegistros = registroVacinacaoService.listarRegistroVacinacao();

        List<RegistroVacinacao> registrosDoPaciente = todosOsRegistros
                .stream()
                .filter(registro -> pacienteId.equals(registro.getIdentificacaoPaciente()))
                .collect(Collectors.toList());

        List<Map<String, Object>> dosesInfo = new ArrayList<>();

        for (RegistroVacinacao registro : registrosDoPaciente) {
            LocalDate dataVacinacao = registro.getDataVacinacao();
            String identificacaoDose = registro.getIdentificacaoDose();

            Map<String, Object> doseInfo = new HashMap<>();
            doseInfo.put("pacienteInfo", dadosPacientes);
            doseInfo.put("dataVacinação", dataVacinacao);
            doseInfo.put("identificacaoDose", identificacaoDose);

            dosesInfo.add(doseInfo);
        }

        return dosesInfo;
    }

    public Map<String, Object>  listarTotalVacinasAplicadas(String estado) {
        try {
            JsonNode dadosPacientes = pacienteWBService.listaTodosPacientes();
            List<RegistroVacinacao> todosOsRegistros = registroVacinacaoService.listarRegistroVacinacao();

            if (dadosPacientes == null) {
                throw new RuntimeException("Erro ao obter dados da API Paciente.");
            }

            int totalVacinasAplicadas = calcularTotalVacinasAplicadas(dadosPacientes, todosOsRegistros, estado);

            Map<String, Object> resposta = new HashMap<>();
            resposta.put("totalVacinasAplicadas", totalVacinasAplicadas);

            return resposta;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar o total de vacinas aplicadas: " + e.getMessage());
        }
    }

    private int calcularTotalVacinasAplicadas(JsonNode dadosPacientes, List<RegistroVacinacao> todosOsRegistros, String estado) {
        int totalVacinasAplicadas = 0;

        for (JsonNode pacienteNode : dadosPacientes) {
            JsonNode enderecosNode = pacienteNode.path("enderecos");
            if (enderecosNode.isArray()) {
                for (JsonNode enderecoNode : enderecosNode) {
                    JsonNode estadoNode = enderecoNode.path("estado");
                    if (estadoNode.isTextual()) {
                        if (estado == null || estado.isEmpty() || estadoNode.asText().trim().equalsIgnoreCase(estado.trim())) {
                            String pacienteId = pacienteNode.get("id").asText();
                            long registrosParaPaciente = todosOsRegistros.stream()
                                    .filter(registro -> registro.getIdentificacaoPaciente().equals(pacienteId))
                                    .count();
                            totalVacinasAplicadas += (int) registrosParaPaciente;
                        }
                    }
                }
            }
        }

        return totalVacinasAplicadas;
    }

    public ResponseEntity<?> listarPacientesComDosesAtrasadas(String estado, Map<String, String> parametrosRequisicao) {
        if (parametrosRequisicao.size() > 1 || (parametrosRequisicao.size() == 1 && !parametrosRequisicao.containsKey("estado"))) {
            return ResponseEntity.badRequest().body("Erro: Parâmetros não permitidos na solicitação.");
        }

        try {
            JsonNode dadosPacientes = pacienteWBService.listaTodosPacientes();
            JsonNode dadosVacina = vacinaWBService.listarTodasVacinas();
            List<RegistroVacinacao> todosOsRegistros = registroVacinacaoService.listarRegistroVacinacao();

            if (dadosPacientes == null || dadosVacina == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao obter dados da API externa.");
            }

            List<Map<String, Object>> pacientesComDosesAtrasadas = calcularPacientesComDosesAtrasadas(dadosPacientes, dadosVacina, todosOsRegistros, estado);

            return ResponseEntity.ok(pacientesComDosesAtrasadas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao listar pacientes com doses atrasadas: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> calcularPacientesComDosesAtrasadas(JsonNode dadosPacientes, JsonNode dadosVacina, List<RegistroVacinacao> todosOsRegistros, String estado) {
        List<Map<String, Object>> pacientesComDosesAtrasadas = new ArrayList<>();

        for (JsonNode pacienteNode : dadosPacientes) {
            String pacienteId = pacienteNode.get("id").asText();

            for (JsonNode vacina : dadosVacina) {
                int numeroDeDoses = vacina.get("numeroDeDoses").asInt();
                int intervaloDeDoses = vacina.get("intervaloDeDoses").asInt();
                String identificacaoVacina = vacina.get("id").asText();

                List<RegistroVacinacao> registrosDoPacienteParaVacina = todosOsRegistros.stream()
                        .filter(registro -> registro.getIdentificacaoPaciente().equals(pacienteId)
                                && registro.getIdentificacaoVacina().equals(identificacaoVacina))
                        .collect(Collectors.toList());

                if (registrosDoPacienteParaVacina.size() < numeroDeDoses && !registrosDoPacienteParaVacina.isEmpty()) {
                    RegistroVacinacao ultimaDose = registrosDoPacienteParaVacina.get(registrosDoPacienteParaVacina.size() - 1);
                    LocalDate dataDaUltimaDose = ultimaDose.getDataVacinacao();
                    LocalDate dataDaProximaDose = dataDaUltimaDose.plusDays(intervaloDeDoses);

                    if (dataDaProximaDose.isBefore(LocalDate.now())) {
                        List<LocalDate> datasDasDosesAtrasadas = new ArrayList<>();
                        for (RegistroVacinacao registro : registrosDoPacienteParaVacina) {
                            datasDasDosesAtrasadas.add(registro.getDataVacinacao());
                        }
                        Map<String, Object> pacienteComDosesAtrasadas = new HashMap<>();
                        pacienteComDosesAtrasadas.put("paciente", InfoPaciente(pacienteNode));
                        pacienteComDosesAtrasadas.put("doses", InfoDoses(datasDasDosesAtrasadas));
                        pacienteComDosesAtrasadas.put("vacina", InfoVacina(vacina));
                        pacientesComDosesAtrasadas.add(pacienteComDosesAtrasadas);
                    }

                }
            }
        }

        return pacientesComDosesAtrasadas;
    }

    private Map<String, Object> InfoPaciente(JsonNode pacienteNode) {
        Map<String, Object> pacienteInfo = new HashMap<>();
        pacienteInfo.put("nome", pacienteNode.get("nome").asText());
        pacienteInfo.put("idade", calcularIdade(pacienteNode.get("dataDeNascimento").asText()));
        pacienteInfo.put("bairro", pacienteNode.get("enderecos").get(0).get("bairro").asText());
        pacienteInfo.put("municipio", pacienteNode.get("enderecos").get(0).get("municipio").asText());
        pacienteInfo.put("estado", pacienteNode.get("enderecos").get(0).get("estado").asText());
        return pacienteInfo;
    }


    private Map<String, Object> InfoVacina(JsonNode vacina) {
        Map<String, Object> vacinaInfo = new HashMap<>();
        vacinaInfo.put("fabricante", vacina.get("fabricante").asText());
        vacinaInfo.put("total_de_doses", vacina.get("numeroDeDoses").asInt());
        vacinaInfo.put("intervalo_entre_doses", vacina.get("intervaloDeDoses").asInt());
        return vacinaInfo;
    }

    private Map<String, Object> InfoDoses(List<LocalDate> datasDasDosesAtrasadas) {
        Map<String, Object> dosesInfo = new HashMap<>();
        dosesInfo.put("doses", datasDasDosesAtrasadas);
        return dosesInfo;
    }

    private int calcularIdade(String dataNascimento) {
        LocalDate dataNasc = LocalDate.parse(dataNascimento);
        LocalDate dataAtual = LocalDate.now();
        return Period.between(dataNasc, dataAtual).getYears();
    }
}