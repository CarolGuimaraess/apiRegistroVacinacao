package com.registro.vacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registro.vacinacao.dto.InfoPacienteDTO;
import com.registro.vacinacao.dto.InfoVacinaDTO;
import com.registro.vacinacao.dto.PacienteDosesAtrasadasDTO;
import com.registro.vacinacao.dto.PacienteDosesDTO;
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
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    }

    public @NotNull List<PacienteDosesAtrasadasDTO> listarPacientesComDosesAtrasadas(String estado) {
        JsonNode dadosPacientes = pacienteClientService.listarTodosPacientes();
        JsonNode dadosVacinas = vacinaClientService.listarTodasVacinas();
        List<RegistroVacinacao> dadosRegistroVacinacao = registroVacinacaoService.listarRegistroVacinacao();

        return calcularPacientesComDosesAtrasadas(dadosPacientes, dadosVacinas, dadosRegistroVacinacao, estado);

    }

    private @NotNull List<PacienteDosesAtrasadasDTO> calcularPacientesComDosesAtrasadas(@NotNull JsonNode dadosPacientes, JsonNode dadosVacina, List<RegistroVacinacao> dadosRegistroVacinacao, String estado) {
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


    private List<RegistroVacinacao> getRegistrosDoPacienteParaVacina(@NotNull List<RegistroVacinacao> dadosRegistroVacinacao, String pacienteId, String identificacaoVacina) {
        return dadosRegistroVacinacao.stream().filter(registro -> registro.getIdentificacaoPaciente().equals(pacienteId) && registro.getIdentificacaoVacina().equals(identificacaoVacina)).collect(Collectors.toList());
    }

    private List<LocalDate> getDatasDasDosesAtrasadas(@NotNull List<RegistroVacinacao> registrosDoPaciente) {
        return registrosDoPaciente.stream().map(RegistroVacinacao::getDataVacinacao).collect(Collectors.toList());
    }

    private @NotNull InfoPacienteDTO infoPaciente(@NotNull JsonNode pacienteNode) {
        InfoPacienteDTO pacienteInfo = new InfoPacienteDTO();
        pacienteInfo.nome = pacienteNode.get("nome").asText();
        pacienteInfo.idade = calcularIdade(pacienteNode.get("dataDeNascimento").asText());
        pacienteInfo.bairro = pacienteNode.get("enderecos").get(0).get("bairro").asText();
        pacienteInfo.municipio = pacienteNode.get("enderecos").get(0).get("municipio").asText();
        pacienteInfo.estado = pacienteNode.get("enderecos").get(0).get("estado").asText();
        return pacienteInfo;
    }

    private @NotNull InfoVacinaDTO infoVacina(@NotNull JsonNode vacina) {
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

}