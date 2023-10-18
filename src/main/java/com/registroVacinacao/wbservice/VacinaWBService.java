package com.registroVacinacao.wbservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registroVacinacao.Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VacinaWBService {
    private final RestTemplate restTemplate;

    @Autowired
    public VacinaWBService() {
        this.restTemplate = new RestTemplate();
    }

    public JsonNode listarTodasVacinas() {
        String projectBUrl = "http://localhost:8081/vacina";
        try {
            String vacinaData = restTemplate.getForObject(projectBUrl, String.class);
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readTree(vacinaData);
        } catch (java.lang.Exception e) {
            Exception excecao = Exception.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }

    public String buscarVacina(String id) {
        String projectBUrl = "http://localhost:8081/vacina/" + id;
        try {
            String pacienteData = restTemplate.getForObject(projectBUrl, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(pacienteData);
        } catch (java.lang.Exception e) {
            Exception excecao = Exception.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }

}
