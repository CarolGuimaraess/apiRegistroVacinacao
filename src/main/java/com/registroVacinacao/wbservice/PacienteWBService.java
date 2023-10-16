package com.registroVacinacao.wbservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registroVacinacao.ExcecaoPersonalizada;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PacienteWBService {
    private final RestTemplate restTemplate;

    @Autowired
    public PacienteWBService() {
        this.restTemplate = new RestTemplate();
    }

    public JsonNode listarTodosPacientes() {
        String projectBUrl = "http://localhost:8082/pacientes";
        try {
            String pacienteData = restTemplate.getForObject(projectBUrl, String.class);
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readTree(pacienteData);
        } catch (Exception e) {
            ExcecaoPersonalizada excecao = ExcecaoPersonalizada.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }

    public JsonNode buscarPaciente(String id) {
        String projectBUrl = "http://localhost:8082/pacientes/" + id;
        try {
            String pacienteData = restTemplate.getForObject(projectBUrl, String.class);
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readTree(pacienteData);
        } catch (Exception e) {
            ExcecaoPersonalizada excecao = ExcecaoPersonalizada.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }

}
