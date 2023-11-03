package com.registroVacinacao;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.service.RegistroVacinacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class RegistroVacinacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistroVacinacaoService registroVacinacaoService;

    @Test
    @DisplayName("Deve ser possível obter todos os registros de vacinação")
    public void testObterTodosOsRegistroVacinacao() throws java.lang.Exception {

        RegistroVacinacao registro1 = new RegistroVacinacao();
        registro1.setNomeProfissional("João");
        registro1.setSobrenomeProfissional("Silva");
        registro1.setDataVacinacao(LocalDate.of(2023, 1, 15));
        registro1.setCpfProfissional("12345678900");
        registro1.setIdentificacaoPaciente("Paciente1");
        registro1.setIdentificacaoVacina("VacinaA");
        registro1.setIdentificacaoDose("Primeira Dose");

        RegistroVacinacao registro2 = new RegistroVacinacao();
        registro2.setNomeProfissional("Maria");
        registro2.setSobrenomeProfissional("Santos");
        registro2.setDataVacinacao(LocalDate.of(2023, 2, 20));
        registro2.setCpfProfissional("98765432100");
        registro2.setIdentificacaoPaciente("Paciente2");
        registro2.setIdentificacaoVacina("VacinaB");
        registro2.setIdentificacaoDose("Segunda Dose");


        List<RegistroVacinacao> registrosVacinacao = Arrays.asList(registro1, registro2);

        Mockito.when(registroVacinacaoService.listarRegistroVacinacao()).thenReturn(registrosVacinacao);

        mockMvc.perform(get("/registro-vacinacao"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].nomeProfissional").value(registro1.getNomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].sobrenomeProfissional").value(registro1.getSobrenomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].dataVacinacao").value(registro1.getDataVacinacao().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].cpfProfissional").value(registro1.getCpfProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].identificacaoPaciente").value(registro1.getIdentificacaoPaciente()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].identificacaoVacina").value(registro1.getIdentificacaoVacina()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].identificacaoDose").value(registro1.getIdentificacaoDose()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].nomeProfissional").value(registro2.getNomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].sobrenomeProfissional").value(registro2.getSobrenomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].dataVacinacao").value(registro2.getDataVacinacao().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].cpfProfissional").value(registro2.getCpfProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].identificacaoPaciente").value(registro2.getIdentificacaoPaciente()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].identificacaoVacina").value(registro2.getIdentificacaoVacina()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].identificacaoDose").value(registro2.getIdentificacaoDose()));
        // Verificar se o serviço mock foi chamado corretamente
        Mockito.verify(registroVacinacaoService, times(2)).listarRegistroVacinacao();
    }

    @Test
    @DisplayName("Deve ser possível obter um registro de vacinação pelo ID")
    public void testObterRegistroVacinacaoPeloId() throws java.lang.Exception {
        RegistroVacinacao registro = new RegistroVacinacao();
        registro.setId("123S123SA56");
        registro.setNomeProfissional("João");
        registro.setSobrenomeProfissional("Silva");
        registro.setDataVacinacao(LocalDate.of(2023, 1, 15));
        registro.setCpfProfissional("12345678900");
        registro.setIdentificacaoPaciente("Paciente1");
        registro.setIdentificacaoVacina("VacinaA");
        registro.setIdentificacaoDose("Primeira Dose");

        when(registroVacinacaoService.buscarRegistroVacinacao(registro.getId())).thenReturn(registro);

        mockMvc.perform(get("/registro-vacinacao/{id}", registro.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(registro.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.nomeProfissional").value(registro.getNomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sobrenomeProfissional").value(registro.getSobrenomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.dataVacinacao").value(registro.getDataVacinacao().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cpfProfissional").value(registro.getCpfProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.identificacaoPaciente").value(registro.getIdentificacaoPaciente()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.identificacaoVacina").value(registro.getIdentificacaoVacina()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.identificacaoDose").value(registro.getIdentificacaoDose()));

        Mockito.verify(registroVacinacaoService, times(1)).buscarRegistroVacinacao(registro.getId());
    }
    @Test
    @DisplayName("Deve adicionar um registro de vacinação")
    public void testAdicionarRegistroVacincao() throws java.lang.Exception {
        RegistroVacinacao registro = new RegistroVacinacao();
        registro.setId("123S123SA57");
        registro.setNomeProfissional("Marcos");
        registro.setSobrenomeProfissional("Silva");
        registro.setDataVacinacao(LocalDate.of(2023, 1, 15));
        registro.setCpfProfissional("12345677900");
        registro.setIdentificacaoPaciente("Paciente4");
        registro.setIdentificacaoVacina("VacinaD");
        registro.setIdentificacaoDose("Primeira Dose");

        doNothing().when(registroVacinacaoService).criarRegistroVacinacao(registro);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        String novoRegistroVacinacaoJson;
        novoRegistroVacinacaoJson = objectMapper.writeValueAsString(registro);

        mockMvc.perform(MockMvcRequestBuilders.post("/registro-vacinacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoRegistroVacinacaoJson))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(registro.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.nomeProfissional").value(registro.getNomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.sobrenomeProfissional").value(registro.getSobrenomeProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.dataVacinacao").value(String.valueOf(registro.getDataVacinacao())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.cpfProfissional").value(registro.getCpfProfissional()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.identificacaoPaciente").value(registro.getIdentificacaoPaciente()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.identificacaoVacina").value(registro.getIdentificacaoVacina()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.identificacaoDose").value(registro.getIdentificacaoDose()));

        // Verify
        verify(registroVacinacaoService, times(1)).criarRegistroVacinacao(registro);
    }
}
