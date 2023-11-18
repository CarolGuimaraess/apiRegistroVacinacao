package com.registro.vacinacao;

import com.registro.vacinacao.controller.PacienteController;
import com.registro.vacinacao.dto.EnderecoDTO;
import com.registro.vacinacao.dto.ErrorDTO;
import com.registro.vacinacao.dto.PacienteDTO;
import com.registro.vacinacao.dto.PacienteDosesDTO;
import com.registro.vacinacao.exception.TratamentoErros;
import com.registro.vacinacao.service.PacienteService;
import com.registro.vacinacao.service.client.PacienteClientService;
import com.registro.vacinacao.service.client.VacinaClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Mock
    private PacienteService pacienteService;
    @MockBean
    VacinaClientService vacinaClientService;
    private final String RESOURCE_URL = "/doses/atrasadas";

    @MockBean
    PacienteClientService pacienteClientService;
    @Mock
    private TratamentoErros tratamentoDeErros;

    @InjectMocks
    private PacienteController pacienteController;

    @Test
    @DisplayName("Deve retornar pacientes registrados com parametro de estado Bahia")
    void testeListarPacientesComParametroEstadoBahia() throws Exception {
        String estado = "BA";

        List<PacienteDTO> pacientesDTO = criarListaPacientesDTO();

        List<PacienteDTO> pacientesDoEstado = pacientesDTO.stream()
                .filter(p -> estado.equals(p.getEnderecos().get(0).getEstado()))
                .collect(Collectors.toList());

        when(pacienteService.listarPacientesComDosesAtrasadas(eq(estado)))
                .thenAnswer(invocation -> pacientesDoEstado);

        mockMvc = MockMvcBuilders.standaloneSetup(pacienteController)
                .setControllerAdvice(tratamentoDeErros)
                .build();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/paciente/doses/atrasadas?estado=" + estado));

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        for (int i = 0; i < pacientesDoEstado.size(); i++) {
            PacienteDTO paciente = pacientesDoEstado.get(i);
            resultActions
                    .andExpect(jsonPath("$[" + i + "].nome").value(paciente.getNome()))
                    .andExpect(jsonPath("$[" + i + "].enderecos[0].estado").value(estado));
        }

        verify(pacienteService, times(1)).registrarLog(eq("GET"), eq("Listar Pacientes com Doses Atrasadas"), any(), eq(HttpServletResponse.SC_OK));
    }

    @Test
    @DisplayName("Deve retornar pacientes registrados sem parametro de estado Bahia")
    void testeListarPacientesSemParametroEstadoBahia() throws Exception {
        String estado = "";

        List<PacienteDTO> pacientesDTO = criarListaPacientesDTO();

        List<PacienteDTO> todosPacientes = pacientesDTO; // Sem filtro para todos os pacientes

        when(pacienteService.listarPacientesComDosesAtrasadas(eq(estado)))
                .thenAnswer(invocation -> todosPacientes);

        mockMvc = MockMvcBuilders.standaloneSetup(pacienteController)
                .setControllerAdvice(tratamentoDeErros)
                .build();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/paciente/doses/atrasadas?estado=" + estado));

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        for (int i = 0; i < todosPacientes.size(); i++) {
            PacienteDTO paciente = todosPacientes.get(i);
            resultActions
                    .andExpect(jsonPath("$[" + i + "].nome").value(paciente.getNome()))
                    .andExpect(jsonPath("$[" + i + "].enderecos[0].estado").value(paciente.getEnderecos().get(0).getEstado()));
        }

        verify(pacienteService, times(1)).registrarLog(eq("GET"), eq("Listar Pacientes com Doses Atrasadas"), any(), eq(HttpServletResponse.SC_OK));
    }

    @Test
    void testeListarDosesPacienteEncontradas() {
        when(pacienteService.listarDosesDoPaciente(anyString())).thenReturn(Collections.singletonList(new PacienteDosesDTO()));

        ResponseEntity<?> responseEntity = pacienteController.listarDosesPaciente("1");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        verify(pacienteService).registrarLog(eq("GET"), eq("Listar doses de Pacientes"), anyString(), eq(HttpServletResponse.SC_OK));
    }

    @Test
    void testeListarDosesPacienteDosesNaoEncontradas() {
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
    void testeListarDosesPacienteHttpServerErrorException() {
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

    @Test
    public void testeListarDosesPacienteErroClienteHTTP() {
        String pacienteId = "789";
        when(pacienteService.listarDosesDoPaciente(anyString())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        when(tratamentoDeErros.criarRespostaDeErro(any(), any())).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        ResponseEntity<?> response = pacienteController.listarDosesPaciente(pacienteId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(pacienteService).registrarLog(eq("GET"), eq("Listar doses de Pacientes"), eq(pacienteId), eq(HttpServletResponse.SC_NOT_FOUND));

        verify(tratamentoDeErros).criarRespostaDeErro(eq(HttpStatus.NOT_FOUND), any());

    }

    private List<PacienteDTO> criarListaPacientesDTO() {
        List<PacienteDTO> pacientes = new ArrayList<>();

        // Primeiro paciente com estado "BA"
        PacienteDTO pacienteDTO1 = new PacienteDTO();
        pacienteDTO1.setId("123456123");
        pacienteDTO1.setNome("Nome do Paciente 1");
        pacienteDTO1.setSobrenome("Sobrenome 1");
        pacienteDTO1.setCpf("12345678901");
        pacienteDTO1.setDataDeNascimento(LocalDate.now());
        pacienteDTO1.setGenero("Feminino");
        pacienteDTO1.setContatos(Collections.singletonList("contato1@teste.com"));

        // Adicione um endereço para o paciente 1
        EnderecoDTO enderecoDTO1 = new EnderecoDTO();
        enderecoDTO1.setLogradouro("Rua Teste 1");
        enderecoDTO1.setNumero(123);
        enderecoDTO1.setBairro("Bairro Teste 1");
        enderecoDTO1.setCep("12345678");
        enderecoDTO1.setMunicipio("Cidade Teste 1");
        enderecoDTO1.setEstado("BA");

        pacienteDTO1.setEnderecos(Collections.singletonList(enderecoDTO1));

        pacientes.add(pacienteDTO1);

        // Segundo paciente com estado "SP" (São Paulo)
        PacienteDTO pacienteDTO2 = new PacienteDTO();
        pacienteDTO2.setId("789012345");
        pacienteDTO2.setNome("Nome do Paciente 2");
        pacienteDTO2.setSobrenome("Sobrenome 2");
        pacienteDTO2.setCpf("98765432109");
        pacienteDTO2.setDataDeNascimento(LocalDate.now());
        pacienteDTO2.setGenero("Masculino");
        pacienteDTO2.setContatos(Collections.singletonList("contato2@teste.com"));

        // Adicione um endereço para o paciente 2
        EnderecoDTO enderecoDTO2 = new EnderecoDTO();
        enderecoDTO2.setLogradouro("Rua Teste 2");
        enderecoDTO2.setNumero(456);
        enderecoDTO2.setBairro("Bairro Teste 2");
        enderecoDTO2.setCep("87654321");
        enderecoDTO2.setMunicipio("Cidade Teste 2");
        enderecoDTO2.setEstado("SP");

        pacienteDTO2.setEnderecos(Collections.singletonList(enderecoDTO2));

        pacientes.add(pacienteDTO2);

        return pacientes;
    }

}