package com.registroVacinacao.wbservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registroVacinacao.Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VacinaWBService {
    private final RestTemplate restTemplate;
    private final String urlBaseVacina;

    @Autowired
    public VacinaWBService(@Value("${api.vacina.base.url}") String urlBaseVacina) {
        this.urlBaseVacina= urlBaseVacina;
        this.restTemplate = new RestTemplate();
    }
    public JsonNode listarTodasVacinas() {
        try {
            return restTemplate.getForObject(urlBaseVacina, JsonNode.class);
        } catch (java.lang.Exception e) {
            Exception excecao = Exception.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }

    public JsonNode buscarVacina(String id) {
        try {
            return restTemplate.getForObject(urlBaseVacina + id, JsonNode.class);
        } catch (java.lang.Exception e) {
            Exception excecao = Exception.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }
}
