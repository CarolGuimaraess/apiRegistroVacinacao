package com.registro.vacinacao.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.registro.vacinacao.exception.TratamentoErros;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PacienteClientService {
    private final RestTemplate restTemplate;
    private final String urlBasePaciente;
    private final TratamentoErros tratamentoDeErros;

    @Autowired
    public PacienteClientService(@Value("${api.paciente.base.url}") String urlBasePaciente, TratamentoErros tratamentoDeErros) {
        this.urlBasePaciente = urlBasePaciente;
        this.tratamentoDeErros = tratamentoDeErros;
        this.restTemplate = new RestTemplate();
    }

    public JsonNode listarTodosPacientes() {
        return restTemplate.getForObject(urlBasePaciente, JsonNode.class);
    }


    public JsonNode buscarPaciente(String id) {
        return restTemplate.getForObject(urlBasePaciente + id, JsonNode.class);
    }

}
