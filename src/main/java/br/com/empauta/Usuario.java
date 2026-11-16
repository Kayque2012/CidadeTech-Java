package br.com.empauta;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
class Usuario {
  enum Papel {
    CIDADAO,
    GESTOR
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, unique = true, length = 150)
  String email;

  @Column(nullable = false, length = 100)
  String nome;

  @Column(nullable = false, length = 100)
  String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  Papel papel;

  protected Usuario() {}

  Usuario(String e, String n, String hash, Papel p) {
    email = e;
    nome = n;
    senhaHash = hash;
    papel = p;
  }
}
