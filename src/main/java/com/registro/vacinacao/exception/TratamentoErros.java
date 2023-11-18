package com.registro.vacinacao.exception;

import com.registro.vacinacao.dto.ErrorDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@ControllerAdvice
public class TratamentoErros {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorDTO> lidarComErroDeValidacao(@NotNull MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldError().getDefaultMessage();
        return criarRespostaDeErro(HttpStatus.BAD_REQUEST, mensagem);
    }
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorDTO> lidarComErroDeNulos(NullPointerException e) {
        String mensagem = "Parece que algo deu errado. Encontramos uma referência nula, o que causou o erro. Para maiores informações, entre em contato pelo nosso WhatsApp 71 99999-9999. Lamentamos o ocorrido!";
        return criarRespostaDeErro(HttpStatus.INTERNAL_SERVER_ERROR, mensagem);
    }
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorDTO> lidarComErroDoCliente(@NotNull HttpClientErrorException e) {
        return criarRespostaDeErro(e.getStatusCode(), "Erro do cliente: " + e.getResponseBodyAsString());
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorDTO> lidarComErroDoServidor(@NotNull HttpServerErrorException e) {
        String mensagem = "Ocorreu um erro na aplicação. Nossa equipe de TI já foi notificada e em breve nossos serviços estarão restabelecidos. Para maiores informações, entre em contato pelo nosso WhatsApp 71 99999-9999. Lamentamos o ocorrido!";
        return criarRespostaDeErro(e.getStatusCode(), mensagem);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorDTO> lidarComErroDeConexao(@NotNull ResourceAccessException e) {
        String mensagem = "Ocorreu um erro na aplicação. Nossa equipe de TI já foi notificada e em breve nossos serviços estarão restabelecidos. Para maiores informações, entre em contato pelo nosso WhatsApp 71 99999-9999. Lamentamos o ocorrido!";
        return criarRespostaDeErro(HttpStatus.SERVICE_UNAVAILABLE, mensagem);
    }

    @NotNull
    public ResponseEntity<ErrorDTO> criarRespostaDeErro(@NotNull HttpStatus status, String mensagem) {
        ErrorDTO erro = new ErrorDTO(mensagem);
        return ResponseEntity.status(status).body(erro);
    }

}
