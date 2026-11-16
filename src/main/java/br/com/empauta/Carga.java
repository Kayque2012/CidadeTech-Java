package br.com.empauta;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
class Carga implements ApplicationRunner {
  private final jakarta.validation.Validator validator;
  private final Usuarios usuarios;
  private final PasswordEncoder encoder;
  private final TemasService temas;
  private final ConsultasService consultas;

  Carga(
      Usuarios u,
      PasswordEncoder p,
      TemasService t,
      ConsultasService c,
      jakarta.validation.Validator v) {
    validator = v;
    usuarios = u;
    encoder = p;
    temas = t;
    consultas = c;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    var gestor =
        new Dto.Cadastro("Gestor de demonstração", "gestor@example.test", "GestorDemo2026");
    var cidadao =
        new Dto.Cadastro("Cidadão de demonstração", "cidadao@example.test", "CidadaoDemo2026");
    validar(gestor);
    validar(cidadao);
    usuarios.save(
        new Usuario(
            gestor.email(), gestor.nome(), encoder.encode(gestor.senha()), Usuario.Papel.GESTOR));
    var u =
        usuarios.save(
            new Usuario(
                cidadao.email(),
                cidadao.nome(),
                encoder.encode(cidadao.senha()),
                Usuario.Papel.CIDADAO));
    long mobilidade = temas.criar(validar(new Dto.TemaEntrada("Mobilidade"))).id();
    long espacos = temas.criar(validar(new Dto.TemaEntrada("Espaços públicos"))).id();
    long aberta =
        consultas
            .criar(
                validar(
                    new Dto.ConsultaEntrada(
                        "Faixa elevada perto da escola",
                        "Você apoia a instalação de uma faixa elevada na entrada da escola do"
                            + " bairro?",
                        "Bairro Modelo",
                        mobilidade)))
            .id();
    consultas.abrir(aberta);
    long encerrada =
        consultas
            .criar(
                validar(
                    new Dto.ConsultaEntrada(
                        "Reforma da praça comunitária",
                        "Proposta de recuperar bancos e iluminação na praça do bairro.",
                        "Bairro Modelo",
                        espacos)))
            .id();
    consultas.abrir(encerrada);
    consultas.votar(encerrada, u.id, Voto.Escolha.FAVOR);
    consultas.encerrar(encerrada);
  }

  private <T> T validar(T d) {
    if (!validator.validate(d).isEmpty())
      throw new IllegalStateException("Carga de demonstração inválida.");
    return d;
  }
}
