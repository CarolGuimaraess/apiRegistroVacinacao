package com.registro.vacinacao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TratamentoParaErrosServidorInterno {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> lidarComProblemasInternoDeServidor(Exception ex){
        String mensagem = "Ocorreu um erro na aplicação. Nossa equipe de TI já foi notificada e em breve nossos serviços estarão restabelecidos. Para maiores informações, entre em contato pelo nosso WhatsApp 71 99999-9999. Lamentamos o ocorrido!";
        return new ResponseEntity<>(mensagem, HttpStatus.INTERNAL_SERVER_ERROR);

    }
}
