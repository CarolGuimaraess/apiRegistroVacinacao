package com.registroVacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.wbservice.PacienteWBService;
import com.registroVacinacao.wbservice.VacinaWBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PacienteVacinaService {
    private final RegistroVacinacaoService registroVacinacaoService;
    private final PacienteWBService pacienteWBService;
    private final VacinaWBService vacinaWBService;

    @Autowired
    public PacienteVacinaService(RegistroVacinacaoService registroVacinacaoService, PacienteWBService pacienteWBService, VacinaWBService vacinaWBService, CacheManager cacheManager) {
        this.registroVacinacaoService = registroVacinacaoService;
        this.pacienteWBService = pacienteWBService;
        this.vacinaWBService = vacinaWBService;
        this.cacheManager = cacheManager;
    }

    private final CacheManager cacheManager;

    @Cacheable("registroVacinacaoCache")
    public List<Map<String, Object>> listarDosesDoPaciente(String pacienteId) {
        JsonNode dadosPacientes = pacienteWBService.buscarPaciente(pacienteId);

        List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

        List<RegistroVacinacao> registrosDoPaciente = dadosRegistroVacinacao
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

    public Map<String, Object> listarTotalVacinasAplicadas(String estado) {
        try {
            JsonNode dadosPacientes = pacienteWBService.listarTodosPacientes();
            List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

            int totalVacinasAplicadas = calcularTotalVacinasAplicadas(dadosPacientes, dadosRegistroVacinacao, estado);

            Map<String, Object> resposta = new HashMap<>();
            resposta.put("totalVacinasAplicadas", totalVacinasAplicadas);

            return resposta;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar o total de vacinas aplicadas: " + e.getMessage());
        }
    }

    private int calcularTotalVacinasAplicadas(JsonNode dadosPacientes, List<RegistroVacinacao> dadosRegistroVacinacao, String estado) {
        int totalVacinasAplicadas = 0;

        for (JsonNode pacienteNode : dadosPacientes) {
            JsonNode enderecosNode = pacienteNode.path("enderecos");
            if (enderecosNode.isArray()) {
                for (JsonNode enderecoNode : enderecosNode) {
                    JsonNode estadoNode = enderecoNode.path("estado");
                    if (estadoNode.isTextual()) {
                        if (estado == null || estado.isEmpty() || estadoNode.asText().trim().equalsIgnoreCase(estado.trim())) {
                            String pacienteId = pacienteNode.get("id").asText();
                            long registrosParaPaciente = dadosRegistroVacinacao.stream()
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

    public List<Map<String, Object>> listarPacientesComDosesAtrasadas(String estado) {

        try {
            JsonNode dadosPacientes = pacienteWBService.listarTodosPacientes();
            JsonNode dadosVacinas = vacinaWBService.listarTodasVacinas();
            List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

            List<Map<String, Object>> pacientesComDosesAtrasadas = calcularPacientesComDosesAtrasadas(dadosPacientes, dadosVacinas, dadosRegistroVacinacao, estado);

            return pacientesComDosesAtrasadas;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar pacientes com doses atrasadas: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> calcularPacientesComDosesAtrasadas(JsonNode dadosPacientes, JsonNode dadosVacina, List<RegistroVacinacao> dadosRegistroVacinacao, String estado) {
        List<Map<String, Object>> pacientesComDosesAtrasadas = new ArrayList<>();

        for (JsonNode pacienteNode : dadosPacientes) {
            JsonNode enderecosNode = pacienteNode.path("enderecos");
            if (enderecosNode.isArray()) {
                for (JsonNode enderecoNode : enderecosNode) {
                    JsonNode estadoNode = enderecoNode.path("estado");
                    if (estadoNode.isTextual() && (estado == null || estado.isEmpty() || estadoNode.asText().trim().equalsIgnoreCase(estado.trim()))) {
                        String pacienteId = pacienteNode.get("id").asText();

                        for (JsonNode vacina : dadosVacina) {
                            int numeroDeDoses = vacina.get("numeroDeDoses").asInt();
                            int intervaloDeDoses = vacina.get("intervaloDeDoses").asInt();
                            String identificacaoVacina = vacina.get("id").asText();

                            List<RegistroVacinacao> registrosDoPacienteParaVacina = dadosRegistroVacinacao.stream()
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
                                    pacienteComDosesAtrasadas.put("doses_atrasada", InfoDoses(datasDasDosesAtrasadas));
                                    pacienteComDosesAtrasadas.put("vacina", InfoVacina(vacina));
                                    pacientesComDosesAtrasadas.add(pacienteComDosesAtrasadas);
                                }

                            }
                        }
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

    public Map<String, Object> listarVacinasAplicadasFabricante(String fabricante, String estado) {
        JsonNode dadosVacinas = vacinaWBService.listarTodasVacinas();
        List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

        int totalPessoasVacinas = 0;

        for (JsonNode vacina : dadosVacinas) {
            if (fabricante.equals(vacina.findValue("fabricante").asText())) {
                String id = vacina.findValue("id").asText();
                totalPessoasVacinas += (int) dadosRegistroVacinacao.stream()
                        .filter(registro -> id.equals(registro.getIdentificacaoVacina()))
                        .count();
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        Map<String, Object> vacinaInfo = new HashMap<>();
        vacinaInfo.put("fabricante", fabricante);
        vacinaInfo.put("doses_aplicadas", totalPessoasVacinas);
        resultado.put("vacina", vacinaInfo);

        return resultado;
    }
}