package br.com.empauta;

import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class Contas {
  private final Usuarios usuarios;
  private final PasswordEncoder senhas;
  private final JwtEncoder encoder;
  private final String hashFicticio;

  Contas(Usuarios u, PasswordEncoder s, JwtEncoder e) {
    usuarios = u;
    senhas = s;
    encoder = e;
    hashFicticio = s.encode("conta-inexistente-para-comparacao");
  }

  @Transactional
  Dto.Conta cadastrar(Dto.Cadastro d) {
    String email = normalizar(d.email());
    if (usuarios.findByEmail(email).isPresent()) throw Falha.conflito("E-mail já cadastrado.");
    var u =
        usuarios.saveAndFlush(
            new Usuario(email, d.nome().strip(), senhas.encode(d.senha()), Usuario.Papel.CIDADAO));
    return new Dto.Conta(u.id, u.nome, u.email, u.papel);
  }

  @Transactional(readOnly = true)
  Dto.Token login(Dto.Login d) {
    var u = usuarios.findByEmail(normalizar(d.email())).orElse(null);
    if (!senhas.matches(d.senha(), u == null ? hashFicticio : u.senhaHash) || u == null)
      throw new Falha(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    var agora = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer(Seguranca.EMISSOR)
            .subject(u.id.toString())
            .issuedAt(agora)
            .expiresAt(agora.plusSeconds(1800))
            .claim("papel", u.papel.name())
            .build();
    var header = JwsHeader.with(MacAlgorithm.HS256).build();
    return new Dto.Token(
        encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
        "Bearer",
        1800,
        u.papel);
  }

  private String normalizar(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }
}
