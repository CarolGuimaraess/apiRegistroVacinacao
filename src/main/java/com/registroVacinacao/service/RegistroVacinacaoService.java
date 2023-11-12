package com.registroVacinacao.service;

import com.registroVacinacao.entity.Log;
import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.repository.RegistroVacinacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import java.util.Optional;

@Service
public class RegistroVacinacaoService {
    @Autowired
    RegistroVacinacaoRepository registroVacinacaoRepository;

    @Autowired
    public RegistroVacinacaoService(CacheManager cacheManager, RegistroVacinacaoRepository registroVacinacaoRepository) {
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

        if (cache != null){
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

    @CachePut(value = "registroVacinacaoCache")
    public void criarRegistroVacinacao(RegistroVacinacao registroVacinacao) {
        registroVacinacaoRepository.insert(registroVacinacao);
    }

    @CachePut(value = "registroVacinacaoCache", key = "#id")
    public RegistroVacinacao atualizarRegistroVacinacao(String id, RegistroVacinacao registroVacinacao) throws Exception {
        RegistroVacinacao registroVacinacaoAntigo = buscarRegistroVacinacao(id);

        if (registroVacinacaoAntigo.getIdentificacaoVacina().equals(registroVacinacao.getIdentificacaoVacina())){

            registroVacinacaoAntigo.setNomeProfissional(registroVacinacao.getNomeProfissional());
            registroVacinacaoAntigo.setSobrenomeProfissional(registroVacinacao.getSobrenomeProfissional());
            registroVacinacaoAntigo.setDataVacinacao(registroVacinacao.getDataVacinacao());
            registroVacinacaoAntigo.setCpfProfissional(registroVacinacao.getCpfProfissional());
            registroVacinacaoAntigo.setIdentificacaoPaciente(registroVacinacao.getIdentificacaoPaciente());
            registroVacinacaoAntigo.setIdentificacaoVacina(registroVacinacao.getIdentificacaoVacina());
            registroVacinacaoAntigo.setIdentificacaoDose(registroVacinacao.getIdentificacaoDose());

            registroVacinacaoRepository.save(registroVacinacaoAntigo);

            return registroVacinacaoAntigo;

        }else
            return (RegistroVacinacao) ResponseEntity.status(HttpStatus.NOT_FOUND);
    }

    public void excluirRegistroVacinacao(String id) throws Exception {

        Cache cache = cacheManager.getCache("registroVacinacaoCache");
        if (cache != null) {
            cache.evict(id);
        }

        RegistroVacinacao registroVacinacao = buscarRegistroVacinacao(id);

        registroVacinacaoRepository.delete(registroVacinacao);

    }

}
