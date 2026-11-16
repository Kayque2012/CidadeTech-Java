package br.com.empauta;

import jakarta.validation.Validator;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("postgres")
class GestorInicial implements ApplicationRunner {
  private final Usuarios usuarios;
  private final Validator validador;
  private final PasswordEncoder senhas;
  private final String email, senha;

  GestorInicial(
      Usuarios u,
      Validator v,
      PasswordEncoder p,
      @Value("${GESTOR_INICIAL_EMAIL:}") String email,
      @Value("${GESTOR_INICIAL_SENHA:}") String senha) {
    usuarios = u;
    validador = v;
    senhas = p;
    this.email = email;
    this.senha = senha;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (email.isEmpty() && senha.isEmpty()) return;
    var d = new Dto.Cadastro("Gestor inicial", email.strip().toLowerCase(Locale.ROOT), senha);
    if (!validador.validate(d).isEmpty())
      throw new IllegalStateException("Configuração do gestor inicial inválida.");
    var existente = usuarios.findByEmail(d.email());
    if (existente.isPresent()) {
      if (existente.get().papel != Usuario.Papel.GESTOR)
        throw new IllegalStateException(
            "O e-mail configurado já pertence a uma conta sem papel de gestor.");
      return;
    }
    usuarios.save(new Usuario(d.email(), d.nome(), senhas.encode(d.senha()), Usuario.Papel.GESTOR));
  }
}
