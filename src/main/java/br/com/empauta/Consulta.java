package br.com.empauta;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "consultas")
class Consulta {
  enum Estado {
    RASCUNHO,
    ABERTA,
    ENCERRADA
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, length = 140)
  String titulo;

  @Column(nullable = false, length = 1500)
  String descricao;

  @Column(nullable = false, length = 80)
  String bairro;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tema_id")
  Tema tema;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  Estado estado = Estado.RASCUNHO;

  @Column(nullable = false)
  Instant criadaEm = Instant.now();

  protected Consulta() {}
}
