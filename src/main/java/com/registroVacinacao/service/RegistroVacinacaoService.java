package com.registroVacinacao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.registroVacinacao.clientsService.PacienteService;
import com.registroVacinacao.clientsService.VacinaService;
import com.registroVacinacao.entity.Log;
import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.repository.RegistroVacinacaoRepository;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
    private final PacienteService pacienteService;
    private final VacinaService vacinaService;
    @Autowired
    RegistroVacinacaoRepository registroVacinacaoRepository;

    @Autowired
    public RegistroVacinacaoService(PacienteService pacienteService, VacinaService vacinaService, CacheManager cacheManager, RegistroVacinacaoRepository registroVacinacaoRepository) {
        this.pacienteService = pacienteService;
        this.vacinaService = vacinaService;
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
    public RegistroVacinacao buscarRegistroVacinacao(String id) throws Exception {

        Cache cache = cacheManager.getCache("registroVacinacaoCache");

        if (cache != null) {
            Cache.ValueWrapper valorBuscaId = cache.get(id);
            if (valorBuscaId != null) {
                RegistroVacinacao registroVacinacao = (RegistroVacinacao) valorBuscaId.get();
                return registroVacinacao;
            }
        }

        Optional<RegistroVacinacao> registroVacinacaoOptional = registroVacinacaoRepository.findById(id);

        if (!registroVacinacaoOptional.isPresent()) {
            throw new Exception("Registro de Vacinação não encontrado!");
        }

        return registroVacinacaoOptional.get();
    }

    @CacheEvict(value = "registroVacinacaoCache", key = "#id")
    @CachePut(value = "registroVacinacaoCache")
    public Map<String, Object> criarRegistroVacinacao(@NotNull RegistroVacinacao registroVacinacao) {
        Map<String, Object> resultado = new HashMap<>();

        if (validarRegistroVacinacao(registroVacinacao.getIdentificacaoVacina(), registroVacinacao.getIdentificacaoPaciente(), "criar")) {
            resultado.put("status", HttpStatus.OK.value());
            resultado.put("mensagem", "Registro de vacinação criado com sucesso!");
            registroVacinacaoRepository.insert(registroVacinacao);
        } else {
            resultado.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
            resultado.put("mensagem", "Não foi possível criar o registro de vacinação. O paciente já recebeu todas as vacinas necessárias do tratamento!");
        }
        return resultado;
    }

    public boolean validarRegistroVacinacao(String vacinaId, String pacienteId, String tipo) {
            JsonNode dadosVacina = vacinaService.buscarVacina(vacinaId);
            JsonNode dadosPaciente = pacienteService.buscarPaciente(pacienteId);
            if (dadosVacina == null || dadosPaciente == null) {
                return false;
            }
            String fabricante = dadosVacina.get("fabricante").toString();

            JsonNode todasAsVacinas = vacinaService.listarTodasVacinas();

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
            if (Objects.equals(tipo, "criar")) {
                // Verificar se o paciente recebeu doses anteriores
                if (pacienteVacinacao.size() > 0) {
                    LocalDate ultimaDataVacinacao = pacienteVacinacao.get(pacienteVacinacao.size() - 1).getDataVacinacao();

                    int intervaloDeDoses = dadosVacina.get("intervaloDeDoses").asInt();
                    LocalDate dataMinimaProximaDose = ultimaDataVacinacao.plusDays(intervaloDeDoses);
                    LocalDate dataAtual = LocalDate.now();

                    // Verificar se a data atual está antes da data mínima para a próxima dose
                    if (dataAtual.isBefore(dataMinimaProximaDose)) {
                        return false;
                    }

                    // Verificar se o paciente atingiu o número máximo de doses
                    if (pacienteVacinacao.size() >= dadosVacina.get("numeroDeDoses").asInt()) {
                        return false;
                    }
                }
            } else if (Objects.equals(tipo, "atualizar")) {
                // Verificar se o paciente possui mais de uma dose para atualização
                if (pacienteVacinacao.size() > 1) {
                    LocalDate ultimaDataVacinacao = pacienteVacinacao.get(pacienteVacinacao.size() - 1).getDataVacinacao();

                    int intervaloDeDoses = dadosVacina.get("intervaloDeDoses").asInt();
                    LocalDate dataMinimaProximaDose = ultimaDataVacinacao.plusDays(intervaloDeDoses);
                    LocalDate dataAtual = LocalDate.now();

                    // Verificar se a data atual está antes da data mínima para a próxima dose
                    if (dataAtual.isBefore(dataMinimaProximaDose)) {
                        return false;
                    }

                    // Verificar se o paciente atingiu o número máximo de doses
                    if (pacienteVacinacao.size() >= dadosVacina.get("numeroDeDoses").asInt()) {
                        return false;
                    }
                }
            }
            // Se todas as verificações passarem, retornar verdadeiro
            return true;
        }

    private List<RegistroVacinacao> encontrarRegistroVacinacaoPaciente(List<RegistroVacinacao> registros, String pacienteId, List<JsonNode> vacinasDoFabricante) {
        List<RegistroVacinacao> pacienteVacinacao = new ArrayList<>();

        for (RegistroVacinacao registro : registros) {
            // Verifica se o registro pertence ao paciente específico e se o ID da vacina está na lista do fabricante
            if (registro.getIdentificacaoPaciente().equals(pacienteId) && vacinasDoFabricante.stream().anyMatch(vacina -> vacina.get("id").asText().equals(registro.getIdentificacaoVacina()))) {
                pacienteVacinacao.add(registro);
            }
        }

        return pacienteVacinacao;
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

        try {
            RegistroVacinacao registroVacinacaoAntigo = buscarRegistroVacinacao(id);

            if (validarRegistroVacinacao(registroVacinacao.getIdentificacaoVacina(), registroVacinacao.getIdentificacaoPaciente(), "atualizar")) {
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
