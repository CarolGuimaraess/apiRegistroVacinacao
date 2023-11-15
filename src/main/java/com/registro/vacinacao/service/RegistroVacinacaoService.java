package com.registro.vacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registro.vacinacao.entity.Log;
import com.registro.vacinacao.entity.RegistroVacinacao;
import com.registro.vacinacao.repository.RegistroVacinacaoRepository;
import com.registro.vacinacao.service.client.PacienteClientService;
import com.registro.vacinacao.service.client.VacinaClientService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RegistroVacinacaoService {
    private final PacienteClientService pacienteClientService;
    private final VacinaClientService vacinaClientService;
    @Autowired
    RegistroVacinacaoRepository registroVacinacaoRepository;

    @Autowired
    public RegistroVacinacaoService(PacienteClientService pacienteClientService, VacinaClientService vacinaClientService, CacheManager cacheManager, RegistroVacinacaoRepository registroVacinacaoRepository) {
        this.pacienteClientService = pacienteClientService;
        this.vacinaClientService = vacinaClientService;
        this.cacheManager = cacheManager;
        this.registroVacinacaoRepository = registroVacinacaoRepository;
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

    public List<RegistroVacinacao> listarRegistroVacinacao() {
        return registroVacinacaoRepository.findAll();
    }

    @Cacheable("registroVacinacaoCache")
    public RegistroVacinacao buscarRegistroVacinacao(String id) {

        Cache cache = cacheManager.getCache("registroVacinacaoCache");

        if (cache != null) {
            Cache.ValueWrapper valorBuscaId = cache.get(id);
            if (valorBuscaId != null) {
                RegistroVacinacao registroVacinacao = (RegistroVacinacao) valorBuscaId.get();
                return registroVacinacao;
            }
        }

        Optional<RegistroVacinacao> registroVacinacaoOptional = registroVacinacaoRepository.findById(id);

        return registroVacinacaoOptional.orElse(null);

    }

    @CacheEvict(value = "registroVacinacaoCache")
    public Map<String, Object> criarRegistroVacinacao(@NotNull RegistroVacinacao registroVacinacao) {
        Map<String, Object> resultado = new HashMap<>();
        String mensagemValidacao = validarRegistroVacinacao(registroVacinacao.getIdentificacaoVacina(), registroVacinacao.getIdentificacaoPaciente(), "criar");

        if (!mensagemValidacao.equals("sucesso")) {
            resultado.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
            resultado.put("mensagem", mensagemValidacao);
        } else {
            resultado.put("status", HttpStatus.OK.value());
            resultado.put("mensagem", mensagemValidacao);
            registroVacinacaoRepository.insert(registroVacinacao);

        }
        return resultado;
    }

    public String validarRegistroVacinacao(String vacinaId, String pacienteId, String tipo) {
        JsonNode dadosVacina = vacinaClientService.buscarVacina(vacinaId);
        JsonNode dadosPaciente = pacienteClientService.buscarPaciente(pacienteId);

        String fabricante = dadosVacina.get("fabricante").toString();
        JsonNode todasAsVacinas = vacinaClientService.listarTodasVacinas();
        List<RegistroVacinacao> dadosRegistroVacinacao = listarRegistroVacinacao();
        List<JsonNode> vacinasDoFabricante = new ArrayList<>();

        // Iterar sobre todas as vacinas e filtrar aquelas do fabricante específico
        for (JsonNode vacina : todasAsVacinas) {
            if (vacina.get("fabricante").toString().equals(fabricante)) {
                vacinasDoFabricante.add(vacina);
            }
        }

        // Encontrar registros de vacinação do paciente
        List<RegistroVacinacao> pacienteVacinacao = encontrarRegistroVacinacaoPaciente(dadosRegistroVacinacao, pacienteId, vacinasDoFabricante);

        if ((Objects.equals(tipo, "criar") && pacienteVacinacao.size() > 0) ||
                (Objects.equals(tipo, "atualizar") && pacienteVacinacao.size() > 1)) {

            LocalDate ultimaDataVacinacao = pacienteVacinacao.get(pacienteVacinacao.size() - 1).getDataVacinacao();
            int intervaloDeDoses = dadosVacina.get("intervaloDeDoses").asInt();
            LocalDate dataMinimaProximaDose = ultimaDataVacinacao.plusDays(intervaloDeDoses);
            LocalDate dataAtual = LocalDate.now();
            String nomePaciente = dadosPaciente.get("nome").asText();
            String fabricanteVacina = dadosVacina.get("fabricante").asText();

            if (!verificarDosesDeFabricantesDiferentes(pacienteVacinacao, vacinasDoFabricante)) {
                return "A primeira dose aplicada no paciente '" + nomePaciente + "' foi: '" + fabricanteVacina + "'. Todas as doses devem ser aplicadas com o mesmo medicamento!";
            }

            if (dataAtual.isBefore(dataMinimaProximaDose)) {
                String mensagem = "O paciente " + nomePaciente + " recebeu uma dose de " + fabricanteVacina + " no dia " +
                        ultimaDataVacinacao + ". A próxima dose deverá ser aplicada a partir do dia '" +
                        dataMinimaProximaDose +"!";
                return mensagem;
            }

            if (pacienteVacinacao.size() >= dadosVacina.get("numeroDeDoses").asInt()) {
                return "Não foi possível registrar sua solicitação pois o paciente "+
                        nomePaciente +  " já recebeu todas as vacinas necessárias de seu tratamento!";
            }
        }

        return "sucesso";
    }


    private @NotNull List<RegistroVacinacao> encontrarRegistroVacinacaoPaciente(@NotNull List<RegistroVacinacao> registros, String pacienteId, List<JsonNode> vacinasDoFabricante) {
        List<RegistroVacinacao> pacienteVacinacao = new ArrayList<>();

        for (RegistroVacinacao registro : registros) {
            // Verifica se o registro pertence ao paciente específico e se o ID da vacina está na lista do fabricante
            if (registro.getIdentificacaoPaciente().equals(pacienteId) && vacinasDoFabricante.stream().anyMatch(vacina -> vacina.get("id").asText().equals(registro.getIdentificacaoVacina()))) {
                pacienteVacinacao.add(registro);
            }
        }

        return pacienteVacinacao;
    }

    private boolean verificarDosesDeFabricantesDiferentes(List<RegistroVacinacao> pacienteVacinacao, List<JsonNode> vacinasDoFabricante) {
        return pacienteVacinacao.stream()
                .map(RegistroVacinacao::getIdentificacaoVacina)
                .noneMatch(idVacina -> vacinasDoFabricante.stream().noneMatch(vacina -> vacina.get("id").asText().equals(idVacina)));
    }

    private boolean obterUltimoRegistroVacinacaoDoPaciente(String pacienteId, String registroVacinacaoId) {
        // Obtém todos os registros de vacinação
        List<RegistroVacinacao> todosRegistros = listarRegistroVacinacao();

        // Filtra os registros para obter apenas aqueles do paciente específico
        List<RegistroVacinacao> registrosDoPaciente = todosRegistros.stream()
                .filter(registro -> pacienteId.equals(registro.getIdentificacaoPaciente()))
                .collect(Collectors.toList());
        if (registrosDoPaciente.isEmpty()) {
            return false;
        }
        // Ordena os registros do paciente por data de vacinação (do mais recente para o mais antigo)
        registrosDoPaciente.sort(Comparator.comparing(RegistroVacinacao::getDataVacinacao).reversed());
        return registroVacinacaoId.equals(registrosDoPaciente.get(0).getId());

    }

    @CachePut(value = "registroVacinacaoCache", key = "#id")
    public Map<String, Object> atualizarRegistroVacinacao(String id, RegistroVacinacao registroVacinacao) {
        Map<String, Object> resultado = new HashMap<>();
        String mensagemValidacao = validarRegistroVacinacao(registroVacinacao.getIdentificacaoVacina(), registroVacinacao.getIdentificacaoPaciente(), "atualizar");

        try {
            RegistroVacinacao registroVacinacaoAntigo = buscarRegistroVacinacao(id);

            if (mensagemValidacao.equals("sucesso")) {
                if (obterUltimoRegistroVacinacaoDoPaciente(registroVacinacaoAntigo.getIdentificacaoPaciente(), id)) {
                    if (registroVacinacaoAntigo.getIdentificacaoVacina().equals(registroVacinacao.getIdentificacaoVacina())) {

                        registroVacinacaoAntigo.setNomeProfissional(registroVacinacao.getNomeProfissional());
                        registroVacinacaoAntigo.setSobrenomeProfissional(registroVacinacao.getSobrenomeProfissional());
                        registroVacinacaoAntigo.setDataVacinacao(registroVacinacao.getDataVacinacao());
                        registroVacinacaoAntigo.setCpfProfissional(registroVacinacao.getCpfProfissional());
                        registroVacinacaoAntigo.setIdentificacaoPaciente(registroVacinacao.getIdentificacaoPaciente());
                        registroVacinacaoAntigo.setIdentificacaoVacina(registroVacinacao.getIdentificacaoVacina());
                        registroVacinacaoAntigo.setIdentificacaoDose(registroVacinacao.getIdentificacaoDose());

                        registroVacinacaoRepository.save(registroVacinacaoAntigo);

                        resultado.put("status", HttpStatus.OK.value());
                        resultado.put("mensagem", "Atualizado");
                        resultado.put("registroVacinacao", registroVacinacaoAntigo);

                    }
                }
            } else {
                resultado.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
                resultado.put("mensagem", "Apenas o último registro de vacinação de um paciente pode ser editado");

            }
        } catch (Exception e) {
            resultado.put("status", HttpStatus.NOT_FOUND.value());
            resultado.put("mensagem", "Erro ao atualizar registro vacinação");

        }
        return resultado;
    }

    public Map<String, Object> excluirRegistroVacinacao(String id) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            Cache cache = cacheManager.getCache("registroVacinacaoCache");
            if (cache != null) {
                cache.evict(id);
            }

            RegistroVacinacao registroVacinacao = buscarRegistroVacinacao(id);
            if (obterUltimoRegistroVacinacaoDoPaciente(registroVacinacao.getIdentificacaoPaciente(), id)) {
                registroVacinacaoRepository.delete(registroVacinacao);
                resultado.put("status", HttpStatus.NO_CONTENT.value());
                resultado.put("mensagem", "excluído com sucesso");
            } else {
                resultado.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
                resultado.put("mensagem", "Apenas o último registro de vacinação de um paciente pode ser excluído");
            }
        } catch (Exception e) {
            resultado.put("status", HttpStatus.NOT_FOUND.value());
            resultado.put("mensagem", "Erro ao excluir o registro de vacinação");
        }
        return resultado;
    }

}