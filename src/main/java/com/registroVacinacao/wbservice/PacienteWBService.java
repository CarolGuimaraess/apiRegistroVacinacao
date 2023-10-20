package com.registroVacinacao.wbservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.registroVacinacao.Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PacienteWBService {
    private final RestTemplate restTemplate;
    private final String urlBasePaciente;

    @Autowired
    public PacienteWBService(@Value("${api.paciente.base.url}") String urlBasePaciente) {
        this.urlBasePaciente = urlBasePaciente;
        this.restTemplate = new RestTemplate();
    }
    public JsonNode listarTodosPacientes() {
        try {
            return restTemplate.getForObject(urlBasePaciente, JsonNode.class);
        } catch (java.lang.Exception e) {
            Exception excecao = Exception.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }
    public JsonNode buscarPaciente(String id) {
        try {
            return restTemplate.getForObject(urlBasePaciente + id, JsonNode.class);
        } catch (java.lang.Exception e) {
            Exception excecao = Exception.Erro500();
            throw new RuntimeException(excecao.getMensagem());
        }
    }

}
