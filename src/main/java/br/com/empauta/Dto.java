package br.com.empauta;

import jakarta.validation.constraints.*;
import java.time.Instant;
public final class Dto {
 private Dto(){}
 public record TemaEntrada(@NotBlank @Size(max=80) String nome){}
 public record TemaSaida(Long id,String nome){}
 public record ConsultaEntrada(@NotBlank @Size(max=140) String titulo,@NotBlank @Size(max=1500) String descricao,
   @NotBlank @Size(max=80) String bairro,@NotNull @Positive Long temaId){}
 public record ConsultaSaida(Long id,String titulo,String descricao,String bairro,TemaSaida tema,Consulta.Estado estado,Instant criadaEm){}
 public record Cadastro(@NotBlank @Size(max=100) String nome,@NotBlank @Email @Size(max=150) String email,
   @NotBlank @Size(min=10,max=24) String senha){}
 public record Login(@NotBlank @Email @Size(max=150) String email,@NotBlank @Size(max=24) String senha){}
 public record Conta(Long id,String nome,String email,Usuario.Papel papel){}
 public record Token(String token,String tipo,long expiraEm,Usuario.Papel papel){}
 public record VotoEntrada(@NotNull Voto.Escolha escolha){}
 public record Recibo(Long consultaId,Voto.Escolha escolha,Instant registradoEm){}
 public record Resultado(Long consultaId,long favor,long contra,long abstencao,long total){}
}
