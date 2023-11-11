package com.registroVacinacao.clientsService;

import com.fasterxml.jackson.databind.JsonNode;
import com.registroVacinacao.exception.TratamentoParaErrosServidorInterno;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VacinaService {
    private final RestTemplate restTemplate;
    private final String urlBaseVacina;
    private final TratamentoParaErrosServidorInterno tratamentoDeErros;

    @Autowired
    public VacinaService(@Value("${api.vacina.base.url}") String urlBaseVacina, TratamentoParaErrosServidorInterno tratamentoDeErros) {
        this.urlBaseVacina= urlBaseVacina;
        this.tratamentoDeErros = tratamentoDeErros;
        this.restTemplate = new RestTemplate();
    }
    public JsonNode listarTodasVacinas() {
        try {
            return restTemplate.getForObject(urlBaseVacina, JsonNode.class);
        } catch (Exception e) {
            ResponseEntity<String> respostaErro = tratamentoDeErros.lidarComProblemasInternoDeServidor(e);
            throw new RuntimeException(respostaErro.getBody());
        }
    }

    public JsonNode buscarVacina(String id) {
        try {
            return restTemplate.getForObject(urlBaseVacina + id, JsonNode.class);
        } catch (Exception e) {
            ResponseEntity<String> respostaErro = tratamentoDeErros.lidarComProblemasInternoDeServidor(e);
            throw new RuntimeException(respostaErro.getBody());
        }
    }
}
