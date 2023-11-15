package com.registro.vacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registro.vacinacao.entity.Log;
import com.registro.vacinacao.entity.RegistroVacinacao;
import com.registro.vacinacao.service.client.PacienteClientService;
import com.registro.vacinacao.service.client.VacinaClientService;
import dto.InfoPacienteDTO;
import dto.InfoVacinaDTO;
import dto.PacienteDosesAtrasadasDTO;
import dto.PacienteDosesDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class PacienteService {
    private final RegistroVacinacaoService registroVacinacaoService;
    private final PacienteClientService pacienteClientService;
    private final VacinaClientService vacinaClientService;

    @Autowired
    public PacienteService(RegistroVacinacaoService registroVacinacaoService, PacienteClientService pacienteClientService, VacinaClientService vacinaClientService, CacheManager cacheManager) {
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

    private final CacheManager cacheManager;

    @Cacheable(value = "registroVacinacaoCache", key = "#pacienteId")
    public List<PacienteDosesDTO> listarDosesDoPaciente(String pacienteId) {
        try {
            List<RegistroVacinacao> registrosDoPaciente = registroVacinacaoService.listarRegistroVacinacao().stream()
                    .filter(registro -> pacienteId.equals(registro.getIdentificacaoPaciente()))
                    .collect(Collectors.toList());

            JsonNode dadosPacientes = pacienteClientService.buscarPaciente(pacienteId);

            return registrosDoPaciente.stream()
                    .map(registro -> {
                        PacienteDosesDTO reg = new PacienteDosesDTO();
                        reg.dataVacinacao = registro.getDataVacinacao();
                        reg.paciente = dadosPacientes;
                        reg.identificacaoDose = registro.getIdentificacaoDose();
                        return reg;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar as doses do paciente: " + e.getMessage());
        }
    }

    public @NotNull List<PacienteDosesAtrasadasDTO> listarPacientesComDosesAtrasadas(String estado) {
        try {
            JsonNode dadosPacientes = pacienteClientService.listarTodosPacientes();
            JsonNode dadosVacinas = vacinaClientService.listarTodasVacinas();
            List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

            return calcularPacientesComDosesAtrasadas(dadosPacientes, dadosVacinas, dadosRegistroVacinacao, estado);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar pacientes com doses atrasadas: " + e.getMessage());
        }
    }

    private @NotNull List<PacienteDosesAtrasadasDTO> calcularPacientesComDosesAtrasadas(JsonNode dadosPacientes, JsonNode dadosVacina, List<RegistroVacinacao> dadosRegistroVacinacao, String estado) {
        List<PacienteDosesAtrasadasDTO> pacientesComDosesAtrasadas = new ArrayList<>();

        for (JsonNode pacienteNode : dadosPacientes) {
            if (estadoValido(pacienteNode, estado)) {
                String pacienteId = pacienteNode.get("id").asText();

                for (JsonNode vacina : dadosVacina) {
                    int numeroDeDoses = vacina.get("numeroDeDoses").asInt();
                    int intervaloDeDoses = vacina.get("intervaloDeDoses").asInt();
                    String identificacaoVacina = vacina.get("id").asText();

                    List<RegistroVacinacao> registrosDoPacienteParaVacina = getRegistrosDoPacienteParaVacina(dadosRegistroVacinacao, pacienteId, identificacaoVacina);

                    if (registrosDoPacienteParaVacina.size() < numeroDeDoses && !registrosDoPacienteParaVacina.isEmpty()) {
                        RegistroVacinacao ultimaDose = registrosDoPacienteParaVacina.get(registrosDoPacienteParaVacina.size() - 1);
                        LocalDate dataDaUltimaDose = ultimaDose.getDataVacinacao();
                        LocalDate dataDaProximaDose = dataDaUltimaDose.plusDays(intervaloDeDoses);

                        if (dataDaProximaDose.isBefore(LocalDate.now())) {
                            List<LocalDate> datasDasDosesAtrasadas = getDatasDasDosesAtrasadas(registrosDoPacienteParaVacina);

                            PacienteDosesAtrasadasDTO pacienteDTO = new PacienteDosesAtrasadasDTO();
                            pacienteDTO.paciente = (InfoPacienteDTO) infoPaciente(pacienteNode);

                            // Ajuste nesta linha: crie a lista de datas diretamente.
                            pacienteDTO.dosesAtrasadas = datasDasDosesAtrasadas.stream()
                                    .map(LocalDate::toString)
                                    .collect(Collectors.toList());

                            pacienteDTO.vacina = (InfoVacinaDTO) infoVacina(vacina);

                            pacientesComDosesAtrasadas.add(pacienteDTO);
                        }
                    }
                }
            }
        }

        return pacientesComDosesAtrasadas;
    }


    private List<RegistroVacinacao> getRegistrosDoPacienteParaVacina(List<RegistroVacinacao> dadosRegistroVacinacao, String pacienteId, String identificacaoVacina) {
        return dadosRegistroVacinacao.stream().filter(registro -> registro.getIdentificacaoPaciente().equals(pacienteId) && registro.getIdentificacaoVacina().equals(identificacaoVacina)).collect(Collectors.toList());
    }

    private List<LocalDate> getDatasDasDosesAtrasadas(List<RegistroVacinacao> registrosDoPaciente) {
        return registrosDoPaciente.stream().map(RegistroVacinacao::getDataVacinacao).collect(Collectors.toList());
    }

    private InfoPacienteDTO infoPaciente(JsonNode pacienteNode) {
        InfoPacienteDTO pacienteInfo = new InfoPacienteDTO();
        pacienteInfo.nome = pacienteNode.get("nome").asText();
        pacienteInfo.idade = calcularIdade(pacienteNode.get("dataDeNascimento").asText());
        pacienteInfo.bairro = pacienteNode.get("enderecos").get(0).get("bairro").asText();
        pacienteInfo.municipio = pacienteNode.get("enderecos").get(0).get("municipio").asText();
        pacienteInfo.estado = pacienteNode.get("enderecos").get(0).get("estado").asText();
        return pacienteInfo;
    }

    private InfoVacinaDTO infoVacina(JsonNode vacina) {
        InfoVacinaDTO vacinaInfo = new InfoVacinaDTO();
        vacinaInfo.fabricante = vacina.get("fabricante").asText();
        vacinaInfo.totalDeDoses = vacina.get("numeroDeDoses").asInt();
        vacinaInfo.intervaloEntreDoses = vacina.get("intervaloDeDoses").asInt();
        return vacinaInfo;
    }

    private int calcularIdade(String dataNascimento) {
        LocalDate dataNasc = LocalDate.parse(dataNascimento);
        LocalDate dataAtual = LocalDate.now();
        return Period.between(dataNasc, dataAtual).getYears();
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