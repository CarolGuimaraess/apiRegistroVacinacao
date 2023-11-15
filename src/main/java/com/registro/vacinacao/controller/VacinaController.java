package com.registro.vacinacao.controller;

import com.registro.vacinacao.exception.TratamentoParaErrosCliente;
import com.registro.vacinacao.service.VacinaService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data

@RestController
@RequestMapping("/vacina")
public class VacinaController {
    @Autowired
    private VacinaService vacinaService;
    @Autowired
    private final TratamentoParaErrosCliente tratamentoDeErros;

    @GetMapping("/aplicadas/total")
    public ResponseEntity<?> listarTotalVacinasAplicadas(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam Map<String, String> requestParams) {

        try {
            if (requestParams.size() > 1 || (requestParams.size() == 1 && !requestParams.containsKey("estado"))) {

                int statusCode = HttpServletResponse.SC_BAD_REQUEST;
                vacinaService.registrarLog("GET", "Listar Total de Vacinas Aplicadas", requestParams.toString(), statusCode);

                return ResponseEntity.badRequest().body("Erro: Parâmetros não permitidos na solicitação.");
            }
            List<Map<String, Object>> resposta = Collections.singletonList(vacinaService.listarTotalVacinasAplicadas(estado));

            int statusCode = HttpServletResponse.SC_OK;
            vacinaService.registrarLog("GET", "Listar Total de Vacinas Aplicadas", resposta.toString(), statusCode);

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {

            int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            vacinaService.registrarLog("GET", "Listar Total de Vacinas Aplicadas", e.getMessage(), statusCode);

            return ResponseEntity.internalServerError().body("Erro ao listar o total de vacinas aplicadas: " + e.getMessage());
        }
    }

    @GetMapping("/aplicadas")
    public ResponseEntity<?> listarVacinasAplicadasFabricante(
            @RequestParam(name = "fabricante") String fabricante,
            @RequestParam(name = "estado", required = false) String estado) {
        try {
            Map<String, Object> resposta = vacinaService.listarVacinasAplicadasFabricante(fabricante, estado);

            int statusCode = HttpServletResponse.SC_OK;
            vacinaService.registrarLog("GET", "Listar Vacinas Aplicadas", resposta.toString(), statusCode);

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", e.getMessage());

            int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            vacinaService.registrarLog("GET", "Listar Vacinas Aplicadas", e.getMessage(), statusCode);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
        }
    }
}
