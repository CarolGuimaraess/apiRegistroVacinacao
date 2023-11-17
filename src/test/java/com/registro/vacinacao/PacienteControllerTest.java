package com.registro.vacinacao;

import com.registro.vacinacao.controller.PacienteController;
import com.registro.vacinacao.dto.ErrorDTO;
import com.registro.vacinacao.dto.PacienteDosesDTO;
import com.registro.vacinacao.exception.TratamentoErros;
import com.registro.vacinacao.service.PacienteService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Mock
    private PacienteService pacienteService;

    @Mock
    private TratamentoErros tratamentoDeErros;

    @InjectMocks
    private PacienteController pacienteController;


    @Test
    void testListarDosesPacienteEncontradas() {
        when(pacienteService.listarDosesDoPaciente(anyString())).thenReturn(Collections.singletonList(new PacienteDosesDTO()));

        ResponseEntity<?> responseEntity = pacienteController.listarDosesPaciente("1");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(pacienteService).registrarLog(eq("GET"), eq("Listar doses de Pacientes"), anyString(), eq(HttpServletResponse.SC_OK));
    }

    @Test
    void testListarDosesPacienteDosesNaoEncontradas() {
        when(pacienteService.listarDosesDoPaciente(anyString())).thenReturn(Collections.emptyList());

        String mensagemErro = "Doses não encontrada.";

        when(tratamentoDeErros.criarRespostaDeErro(eq(HttpStatus.NOT_FOUND), anyString()))
                .thenAnswer(invocation -> {
                    assertEquals(HttpStatus.NOT_FOUND, invocation.getArgument(0));
                    assertEquals(mensagemErro, invocation.getArgument(1));
                    return new ResponseEntity<>(mensagemErro, HttpStatus.NOT_FOUND);
                });

        ResponseEntity<?> responseEntity = pacienteController.listarDosesPaciente("1");

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        verify(pacienteService).registrarLog(eq("GET"), eq("Listar doses de Pacientes"), eq("1"), eq(HttpServletResponse.SC_NOT_FOUND));
        verify(tratamentoDeErros).criarRespostaDeErro(eq(HttpStatus.NOT_FOUND), eq(mensagemErro));
    }

    @Test
    void testListarDosesPacienteHttpServerErrorException() {
        HttpServerErrorException httpServerErrorException = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro no servidor");
        when(pacienteService.listarDosesDoPaciente(anyString())).thenThrow(httpServerErrorException);

        when(tratamentoDeErros.lidarComErroDoServidor(eq(httpServerErrorException)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorDTO("Ocorreu um erro na aplicação. Nossa equipe de TI já foi notificada e em breve nossos serviços estarão restabelecidos. Para maiores informações, entre em contato pelo nosso WhatsApp 71 99999-9999. Lamentamos o ocorrido!")));

        ResponseEntity<?> responseEntity = pacienteController.listarDosesPaciente("1");

        verify(pacienteService).registrarLog(eq("GET"), eq("Listar doses de Pacientes"), eq("1"), eq(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        verify(tratamentoDeErros).lidarComErroDoServidor(eq(httpServerErrorException));

        ErrorDTO errorDTO = new ErrorDTO("Ocorreu um erro na aplicação. Nossa equipe de TI já foi notificada e em breve nossos serviços estarão restabelecidos. Para maiores informações, entre em contato pelo nosso WhatsApp 71 99999-9999. Lamentamos o ocorrido!");

        assertEquals(errorDTO, responseEntity.getBody());
    }

}