package com.registro.vacinacao.entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "log")
public class Log {
    @Id
    private String id;
    private String timestamp;
    private String level;
    private String method;
    private String action;
    private int statusCode;
    private String message;

}