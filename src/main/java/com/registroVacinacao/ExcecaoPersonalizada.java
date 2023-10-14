package com.registroVacinacao;

public class ExcecaoPersonalizada extends Exception {
    private String mensagem;

    public ExcecaoPersonalizada(String mensagem) {
        super(mensagem);
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }

    public static ExcecaoPersonalizada Erro500() {
        String mensagem = "Ocorreu um erro na aplicação. Nossa equipe de TI já foi notificada e em breve nossos serviços estarão restabelecidos. Para maiores informações, entre em contato pelo nosso WhatsApp 71 99999-9999. Lamentamos o ocorrido!";
        return new ExcecaoPersonalizada(mensagem);
    }
}
