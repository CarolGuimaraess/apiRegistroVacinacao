package com.registro.vacinacao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class TratamentoParaErrosCliente {


    public ResponseEntity<String> lidarComErrosNotFound(Exception ex){
        String mensagem = "O registro de vacinação não foi encontrado.";
        return new ResponseEntity<>(mensagem, HttpStatus.NOT_FOUND);

    }

}
