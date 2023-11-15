package com.registro.vacinacao.controller;

import com.registro.vacinacao.service.PacienteService;
import com.registro.vacinacao.exception.TratamentoParaErrosCliente;
import dto.PacienteDosesAtrasadasDTO;
import dto.PacienteDosesDTO;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data

@RestController
@RequestMapping("/paciente")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;
    @Autowired
    private final TratamentoParaErrosCliente tratamentoDeErros;

    @GetMapping("/{pacienteId}/doses")
    public ResponseEntity<?> listarDosesPaciente(@PathVariable String pacienteId) {
        try {
            List<PacienteDosesDTO> dosesInfo = pacienteService.listarDosesDoPaciente(pacienteId);
            if (dosesInfo.isEmpty()) {

                int statusCode = HttpServletResponse.SC_NOT_FOUND;
                pacienteService.registrarLog("GET", "Listar doses de Pacientes", pacienteId, statusCode);

                return ResponseEntity.notFound().build();
            }

            int statusCode = HttpServletResponse.SC_OK;
            pacienteService.registrarLog("GET", "Listar doses de Pacientes", dosesInfo.toString(), statusCode);

            return ResponseEntity.ok(dosesInfo);
        } catch (Exception e) {
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", e.getMessage());

            int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            pacienteService.registrarLog("GET", "Listar doses de Pacientes", e.getMessage(), statusCode);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
        }
    }

    @GetMapping("/atrasadas")
    public ResponseEntity<?> listarPacientesComDosesAtrasadas(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam Map<String, String> requestParams) {
        try {
            if (requestParams.size() > 1 || (requestParams.size() == 1 && !requestParams.containsKey("estado"))) {

                int statusCode = HttpServletResponse.SC_BAD_REQUEST;
                pacienteService.registrarLog("GET", "Listar Pacientes com Doses Atrasadas", requestParams.toString(), statusCode);

                return ResponseEntity.badRequest().body("Erro: Parâmetros não permitidos na solicitação.");
            }
            @NotNull List<PacienteDosesAtrasadasDTO> resposta = pacienteService.listarPacientesComDosesAtrasadas(estado);

            int statusCode = HttpServletResponse.SC_OK;
            pacienteService.registrarLog("GET", "Listar Pacientes com Doses Atrasadas", resposta.toString(), statusCode);

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", e.getMessage());

            int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            pacienteService.registrarLog("GET", "Listar Pacientes com Doses Atrasadas", e.getMessage(), statusCode);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
        }
    }
}
