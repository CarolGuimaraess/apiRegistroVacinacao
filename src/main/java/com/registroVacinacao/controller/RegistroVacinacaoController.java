package com.registroVacinacao.controller;

import com.registroVacinacao.entity.RegistroVacinacao;
import com.registroVacinacao.service.RegistroVacinacaoService;
import com.registroVacinacao.service.PacienteVacinaService;
import com.registroVacinacao.wbservice.PacienteWBService;
import com.registroVacinacao.wbservice.VacinaWBService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Data

@RestController
@RequestMapping("/registro-vacinacao")
public class RegistroVacinacaoController {

    @Autowired
    RegistroVacinacaoService registroVacinacaoService;
    @Autowired
    private PacienteWBService pacienteWBService;
    @Autowired
    private VacinaWBService vacinaWBService;
    @Autowired
    private PacienteVacinaService pacienteVacinaService;

    @GetMapping
    public ResponseEntity<List<RegistroVacinacao>> listarRegistroVacinacao() {
        return ResponseEntity.ok().body(registroVacinacaoService.listarRegistroVacinacao());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarRegistroVacinacao(@PathVariable String id) {
        try {
            RegistroVacinacao registroVacinacao = registroVacinacaoService.buscarRegistroVacinacao(id);

            return ResponseEntity.ok().body(registroVacinacao);

        } catch (Exception e) {

            Map<String, String> resposta = new HashMap<>();

            resposta.put("mensagem", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resposta);
        }

    }

    @PostMapping
    public ResponseEntity<?> criarRegistroVacinacao(@RequestBody @Valid RegistroVacinacao registroVacinacao, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {

            List<String> erros = bindingResult
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.toList());

            return ResponseEntity.badRequest().body(erros.toArray());
        }
        registroVacinacaoService.criarRegistroVacinacao(registroVacinacao);

        return ResponseEntity.created(null).body(registroVacinacao);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarRegistroVacinacao(
            @PathVariable String id,
            @RequestBody RegistroVacinacao registroVacinacao
    ) {
        try {
            RegistroVacinacao registroVacinacaoAtualizado = registroVacinacaoService.atualizarRegistroVacinacao(id, registroVacinacao);

            return ResponseEntity.ok().body(registroVacinacaoAtualizado);
        } catch (Exception e) {
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirRegistroVacinacao(
            @PathVariable String id
    ) {
        try {
            registroVacinacaoService.excluirRegistroVacinacao(id);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/pacientes/{pacienteId}/doses")
    public ResponseEntity<?> listarDosesPaciente(@PathVariable String pacienteId) {
        try {
            List<Map<String, Object>> dosesInfo = pacienteVacinaService.listarDosesDoPaciente(pacienteId);
            if (dosesInfo.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(dosesInfo);
        } catch (Exception e) {
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
        }
    }

    @GetMapping("/pacientes/vacinas")
    public ResponseEntity<?> listarTotalVacinasAplicadas(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam Map<String, String> requestParams) {

        try {
            if (requestParams.size() > 1 || (requestParams.size() == 1 && !requestParams.containsKey("estado"))) {
                return ResponseEntity.badRequest().body("Erro: Parâmetros não permitidos na solicitação.");
            }
            List<Map<String, Object>> resposta = Collections.singletonList(pacienteVacinaService.listarTotalVacinasAplicadas(estado));

            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao listar o total de vacinas aplicadas: " + e.getMessage());
        }
    }

    @GetMapping("/pacientes/atrasadas")
    public ResponseEntity<?> listarPacientesComDosesAtrasadas(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam Map<String, String> requestParams) {
        try {
            if (requestParams.size() > 1 || (requestParams.size() == 1 && !requestParams.containsKey("estado"))) {
                return ResponseEntity.badRequest().body("Erro: Parâmetros não permitidos na solicitação.");
            }
            List<Map<String, Object>> resposta = pacienteVacinaService.listarPacientesComDosesAtrasadas(estado);
            return ResponseEntity.ok(resposta);
        }catch (Exception e) {
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
        }
    }
}