package com.registroVacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registroVacinacao.entity.Log;
import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.clientsService.PacienteService;
import com.registroVacinacao.clientsService.VacinaService;
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
public class PacienteVacinaService {
    private final RegistroVacinacaoService registroVacinacaoService;
    private final PacienteService pacienteService;
    private final VacinaService vacinaService;

    @Autowired
    public PacienteVacinaService(RegistroVacinacaoService registroVacinacaoService, PacienteService pacienteService, VacinaService vacinaService, CacheManager cacheManager) {
        this.registroVacinacaoService = registroVacinacaoService;
        this.pacienteService = pacienteService;
        this.vacinaService = vacinaService;
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

    @Cacheable("registroVacinacaoCache")
    public List<Map<String, Object>> listarDosesDoPaciente(String pacienteId) {
        try {
            List<RegistroVacinacao> registrosDoPaciente = registroVacinacaoService.listarRegistroVacinacao().stream()
                    .filter(registro -> pacienteId.equals(registro.getIdentificacaoPaciente()))
                    .collect(Collectors.toList());

            JsonNode dadosPacientes = pacienteService.buscarPaciente(pacienteId);

            return registrosDoPaciente.stream()
                    .map(registro -> {
                        Map<String, Object> doseInfo = new HashMap<>();
                        doseInfo.put("paciente", dadosPacientes);
                        doseInfo.put("dataVacinacao", registro.getDataVacinacao());
                        doseInfo.put("identificacaoDose", registro.getIdentificacaoDose());
                        return doseInfo;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar as doses do paciente: " + e.getMessage());
        }
    }

    public Map<String, Object> listarTotalVacinasAplicadas(String estado) {
        try {
            JsonNode dadosPacientes = pacienteService.listarTodosPacientes();
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
                            long registrosParaPaciente = dadosRegistroVacinacao.stream().filter(registro -> registro.getIdentificacaoPaciente().equals(pacienteId)).count();
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
            JsonNode dadosPacientes = pacienteService.listarTodosPacientes();
            JsonNode dadosVacinas = vacinaService.listarTodasVacinas();
            List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

            return calcularPacientesComDosesAtrasadas(dadosPacientes, dadosVacinas, dadosRegistroVacinacao, estado);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar pacientes com doses atrasadas: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> calcularPacientesComDosesAtrasadas(JsonNode dadosPacientes, JsonNode dadosVacina, List<RegistroVacinacao> dadosRegistroVacinacao, String estado) {
        List<Map<String, Object>> pacientesComDosesAtrasadas = new ArrayList<>();

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

        return pacientesComDosesAtrasadas;
    }

    private boolean estadoValido(JsonNode pacienteNode, String estado) {
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

    private List<RegistroVacinacao> getRegistrosDoPacienteParaVacina(List<RegistroVacinacao> dadosRegistroVacinacao, String pacienteId, String identificacaoVacina) {
        return dadosRegistroVacinacao.stream().filter(registro -> registro.getIdentificacaoPaciente().equals(pacienteId) && registro.getIdentificacaoVacina().equals(identificacaoVacina)).collect(Collectors.toList());
    }

    private List<LocalDate> getDatasDasDosesAtrasadas(List<RegistroVacinacao> registrosDoPaciente) {
        return registrosDoPaciente.stream().map(RegistroVacinacao::getDataVacinacao).collect(Collectors.toList());
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
        JsonNode dadosVacinas = vacinaService.listarTodasVacinas();
        List<Map<String, Object>> registrosComPacientes = combinarRegistroComPaciente();

        int totalPessoasVacinadas = calcularTotalPessoasVacinadas(registrosComPacientes, dadosVacinas, fabricante, estado);

        Map<String, Object> resultado = new HashMap<>();
        Map<String, Object> vacinaInfo = new HashMap<>();
        vacinaInfo.put("fabricante", fabricante);
        vacinaInfo.put("doses_aplicadas", totalPessoasVacinadas);
        resultado.put("vacina", vacinaInfo);

        return resultado;
    }

    private int calcularTotalPessoasVacinadas(List<Map<String, Object>> registrosComPacientes, JsonNode dadosVacinas, String fabricante, String estado) {
        return (int) StreamSupport.stream(dadosVacinas.spliterator(), false).filter(vacina -> fabricante.equals(vacina.get("fabricante").asText())).map(vacina -> vacina.get("id").asText()).flatMap(id -> registrosComPacientes.stream().filter(registroComPaciente -> {
            JsonNode pacienteNode = (JsonNode) registroComPaciente.get("paciente");
            JsonNode enderecosNode = pacienteNode.path("enderecos");
            return enderecosNode.isArray() && enderecosNode.elements().hasNext();
        }).flatMap(registroComPaciente -> {
            JsonNode pacienteNode = (JsonNode) registroComPaciente.get("paciente");
            return StreamSupport.stream(pacienteNode.path("enderecos").spliterator(), false).filter(enderecoNode -> estado == null || estado.equals(enderecoNode.path("estado").asText())).filter(enderecoNode -> id.equals(((RegistroVacinacao) registroComPaciente.get("registroVacinacao")).getIdentificacaoVacina()));
        })).count();
    }

    private List<Map<String, Object>> combinarRegistroComPaciente() {
        JsonNode dadosPacientes = pacienteService.listarTodosPacientes();
        List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

        return dadosRegistroVacinacao.stream().map(registro -> {
            String pacienteId = registro.getIdentificacaoPaciente();

            JsonNode pacienteCorrespondente = StreamSupport.stream(dadosPacientes.spliterator(), false).filter(pacienteNode -> pacienteNode.path("id").asText().equals(pacienteId)).findFirst().orElse(null);

            if (pacienteCorrespondente != null) {
                Map<String, Object> registroComPaciente = new HashMap<>();
                registroComPaciente.put("registroVacinacao", registro);
                registroComPaciente.put("paciente", pacienteCorrespondente);

                return registroComPaciente;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }


}