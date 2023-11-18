package com.registro.vacinacao;

import com.registro.vacinacao.controller.VacinaController;
import com.registro.vacinacao.exception.TratamentoErros;
import com.registro.vacinacao.service.VacinaService;
import com.registro.vacinacao.service.client.PacienteClientService;
import com.registro.vacinacao.service.client.VacinaClientService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
public class VacinaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Mock
    private VacinaService vacinaService;
    @MockBean
    VacinaClientService vacinaClientService;

    @MockBean
    PacienteClientService pacienteClientService;
    @Mock
    private TratamentoErros tratamentoDeErros;

    @InjectMocks
    private VacinaController vacinaController;

    @Test
    void testeListarTotalVacinasAplicadasComEstado() {
        String estado = "SP";
        Map<String, String> requestParams;
        requestParams = Collections.singletonMap("estado", estado);

        when(vacinaService.listarTotalVacinasAplicadas(estado)).thenReturn(Collections.singletonMap("total", 100));

        ResponseEntity<?> responseEntity = vacinaController.listarTotalVacinasAplicadas(estado, requestParams);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(vacinaService).registrarLog(eq("GET"), eq("Listar Total de Vacinas Aplicadas"), anyString(), eq(HttpServletResponse.SC_OK));
    }

    @Test
    void testeListarTotalVacinasAplicadasComParametrosInvalidos() {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("parametro1", "valor1");
        requestParams.put("parametro2", "valor2");

        when(tratamentoDeErros.criarRespostaDeErro(eq(HttpStatus.BAD_REQUEST), anyString()))
                .thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<?> responseEntity = vacinaController.listarTotalVacinasAplicadas(null, requestParams);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        verify(vacinaService).registrarLog(eq("GET"), eq("Listar Total de Vacinas Aplicadas"), anyString(), eq(HttpServletResponse.SC_BAD_REQUEST));
        verify(tratamentoDeErros).criarRespostaDeErro(eq(HttpStatus.BAD_REQUEST), anyString());
    }

    @Test
    public void testListarVacinasAplicadasFabricante() {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("fabricante", "Pfizer");
        requestParams.put("estado", "SP");

        Map<String, Object> respostaMock = new HashMap<>();
        respostaMock.put("fabricante", "Pfizer");
        respostaMock.put("estado", "SP");
        when(vacinaService.listarVacinasAplicadasFabricante("Pfizer", "SP")).thenReturn(respostaMock);

        ResponseEntity<?> responseEntity = vacinaController.listarVacinasAplicadasFabricante("Pfizer", "SP", requestParams);

        verify(vacinaService).listarVacinasAplicadasFabricante("Pfizer", "SP");

        assertEquals(HttpStatus.OK.value(), responseEntity.getStatusCodeValue());

    }
}
