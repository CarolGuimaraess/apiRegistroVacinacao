# apiRegistroVacinacao

Este é um projeto de API desenvolvido em Spring Boot para gerenciamento de informações sobre vacinas.

## Pré-requisitos

Certifique-se de ter as seguintes ferramentas instaladas em sua máquina:

- [Java Development Kit (JDK) 8](https://www.oracle.com/java/technologies/downloads/#java8.html)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
- [MongoDB Community Server](https://www.mongodb.com/try/download/community)
- [Postman](https://www.postman.com/downloads/)

## Configuração do Ambiente

### MongoDB

1. Instale o MongoDB Community Server [aqui](https://www.mongodb.com/try/download/community). (Pressione select package em Community Community Server, em seguida pressione download)
2. Abra o MongoDB Compass e crie um banco de dados chamado `registro-vacinacao`.

### IntelliJ IDEA

1. Instale o IntelliJ IDEA seguindo as instruções [aqui](https://www.jetbrains.com/idea/download/).
2. Abra o IntelliJ IDEA e importe o projeto.

### Postman

1. Baixe e instale o Postman através do [site oficial](https://www.postman.com/downloads/).
2. Abra o Postman e configure-o conforme necessário.

## Clone e Execução

1. Clone o projeto:

```bash
git clone https://github.com/CarolGuimaraess/apiRegistroVacinacao.git
```

2. Abra o projeto no IntelliJ IDEA.
3. Configure o MongoDB host e porta no arquivo `src/main/resources/application.properties`:

```properties
# Configurações do MongoDB
server.port=8085
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=registro-vacinacao

# Configurações das APIs
api.paciente.base.url = http://localhost:8082/pacientes/
api.vacina.base.url = http://localhost:8081/vacina/
```

4. Execute o aplicativo a partir do IntelliJ IDEA.

## Endpoints

Após cadastro Pacientes e Vacinas, acesse os seguintes endpoints da API:

- POST - Cadastra 6 registros:
  ``` http://localhost:8085/registro-vacinacao/adicionar-registro-vacinacao```

Exemplo de JSON:
  ```json
  {
      "nomeProfissional":"julia",
      "sobrenomeProfissional":"Guimarães",
      "dataVacinacao":"2022-10-07",
      "cpfProfissional":"838.326.240-07",
      "identificacaoPaciente":"65285720cc781c257bc483ba",
      "identificacaoVacina":"652857ae4f1af0601e896dca",
      "identificacaoDose":"1"
  }
  ```

- POST - Criar registro vacinação:
  ```http://localhost:8085/registro-vacinacao/```


- GET - listar todos os registros vacinais:
  ```http://localhost:8085/registro-vacinacao/```


- GET - listar registros vacinais por id (informar o id do registro vacinação):
  ```http://localhost:8085/registro-vacinacao/id```


- PUT - Atualizar registro vacinação (informar o id do registro vacinação):
  ```http://localhost:8085/registro-vacinacao/id```

Exemplo de JSON:
  ```json
  {
      "nomeProfissional":"julia",
      "sobrenomeProfissional":"Guimarães",
      "dataVacinacao":"2022-10-07",
      "cpfProfissional":"838.326.240-07",
      "identificacaoPaciente":"65285720cc781c257bc483ba",
      "identificacaoVacina":"652857ae4f1af0601e896dca",
      "identificacaoDose":"1"
  }
  ```

- DELETE - Deletar registro vacinação (informar o id do registro vacinação):
  ```http://localhost:8085/registro-vacinacao/id```


- GET: listar doses do paciente (informar o id do cliente):
  ```http://localhost:8085/registro-paciente/id/doses```


- GET: listar pacientes com doses atrasadas (estado é opcional):
  ```http://localhost:8085/registro-paciente/doses/atrasadas?estado=ba```


- GET: listar o total de vacinas aplicadas de cada fabricante (estado é opcional):
  ```http://localhost:8085/registro-vacina/aplicadas?fabricante=pfizer&estado=ba```


- GET: listar o total de vacinas aplicadas (estado é opcional):
  ```http://localhost:8085/registro-vacina/aplicadas/total?estado=ba```

- ## Licença MIT

Este projeto é licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE.txt) para detalhes.

---
