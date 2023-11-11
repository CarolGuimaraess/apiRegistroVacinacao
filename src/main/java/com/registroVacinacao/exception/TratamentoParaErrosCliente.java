package com.registroVacinacao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TratamentoParaErrosCliente {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> lidarComErrosNotFound(Exception ex){
        String mensagem = "O registro de vacinação não foi encontrado.";
        return new ResponseEntity<>(mensagem, HttpStatus.NOT_FOUND);

    }

}
