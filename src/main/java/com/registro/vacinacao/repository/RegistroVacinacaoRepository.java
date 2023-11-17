package com.registro.vacinacao.repository;

import com.registro.vacinacao.entity.RegistroVacinacao;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistroVacinacaoRepository extends MongoRepository<RegistroVacinacao, String> {
    Optional<RegistroVacinacao> findById(String id);
}
