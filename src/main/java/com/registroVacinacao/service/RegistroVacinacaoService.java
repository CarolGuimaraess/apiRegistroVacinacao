package com.registroVacinacao.service;

import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.repository.RegistroVacinacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

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

        registroVacinacaoAntigo.setNomeProfissional(registroVacinacao.getNomeProfissional());
        registroVacinacaoAntigo.setSobrenomeProfissional(registroVacinacao.getSobrenomeProfissional());
        registroVacinacaoAntigo.setDataVacinacao(registroVacinacao.getDataVacinacao());
        registroVacinacaoAntigo.setCpfProfissional(registroVacinacao.getCpfProfissional());
        registroVacinacaoAntigo.setIdentificacaoPaciente(registroVacinacao.getIdentificacaoPaciente());
        registroVacinacaoAntigo.setIdentificacaoVacina(registroVacinacao.getIdentificacaoVacina());
        registroVacinacaoAntigo.setIdentificacaoDose(registroVacinacao.getIdentificacaoDose());

        registroVacinacaoRepository.save(registroVacinacaoAntigo);

        return registroVacinacaoAntigo;
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
