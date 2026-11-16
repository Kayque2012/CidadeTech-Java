package br.com.empauta;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "votos",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_voto_consulta_usuario",
            columnNames = {"consulta_id", "usuario_id"}))
public class Voto {
  public enum Escolha {
    FAVOR,
    CONTRA,
    ABSTENCAO
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "consulta_id")
  Consulta consulta;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id")
  Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  Escolha escolha;

  @Column(nullable = false)
  Instant registradoEm = Instant.now();

  protected Voto() {}

  Voto(Consulta c, Usuario u, Escolha e) {
    consulta = c;
    usuario = u;
    escolha = e;
  }
}
