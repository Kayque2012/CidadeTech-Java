package br.com.empauta;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

@SpringBootApplication @EnableCaching
@OpenAPIDefinition(info=@Info(title="Cidadão em Pauta",version="1.0",description="Consultas comunitárias, participação e resultados. Use Authorize com o token do login."))
@SecurityScheme(name="jwt",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
public class Aplicacao {
 public static void main(String[] args) { SpringApplication.run(Aplicacao.class,args); }
}
