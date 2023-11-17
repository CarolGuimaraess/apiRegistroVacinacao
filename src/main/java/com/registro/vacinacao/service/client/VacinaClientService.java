package com.registro.vacinacao.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.registro.vacinacao.exception.TratamentoErros;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VacinaClientService {
    private final RestTemplate restTemplate;
    private final String urlBaseVacina;

    @Autowired
    public VacinaClientService(@Value("${api.vacina.base.url}") String urlBaseVacina) {
        this.urlBaseVacina = urlBaseVacina;
        this.restTemplate = new RestTemplate();
    }

    public JsonNode listarTodasVacinas() {
        return restTemplate.getForObject(urlBaseVacina, JsonNode.class);
    }

    public JsonNode buscarVacina(String id) {
        return restTemplate.getForObject(urlBaseVacina + id, JsonNode.class);
    }
}
