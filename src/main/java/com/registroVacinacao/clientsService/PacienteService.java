package com.registroVacinacao.clientsService;

import com.fasterxml.jackson.databind.JsonNode;
import com.registroVacinacao.exception.TratamentoParaErrosServidorInterno;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PacienteService {
    private final RestTemplate restTemplate;
    private final String urlBasePaciente;
    private final TratamentoParaErrosServidorInterno tratamentoDeErros;

    @Autowired
    public PacienteService(@Value("${api.paciente.base.url}") String urlBasePaciente, TratamentoParaErrosServidorInterno tratamentoDeErros) {
        this.urlBasePaciente = urlBasePaciente;
        this.tratamentoDeErros = tratamentoDeErros;
        this.restTemplate = new RestTemplate();
    }

    public JsonNode listarTodosPacientes() {
        try {
            return restTemplate.getForObject(urlBasePaciente, JsonNode.class);
        } catch (Exception e) {
            ResponseEntity<String> respostaErro = tratamentoDeErros.lidarComProblemasInternoDeServidor(e);
            throw new RuntimeException(respostaErro.getBody());
        }
    }

    public JsonNode buscarPaciente(String id) {
        try {
            return restTemplate.getForObject(urlBasePaciente + id, JsonNode.class);
        } catch (Exception e) {
            ResponseEntity<String> respostaErro = tratamentoDeErros.lidarComProblemasInternoDeServidor(e);
            throw new RuntimeException(respostaErro.getBody());
        }
    }

}
