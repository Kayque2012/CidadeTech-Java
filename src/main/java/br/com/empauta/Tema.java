package br.com.empauta;

import jakarta.persistence.*;
@Entity @Table(name="temas")
class Tema {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,length=80) String nome;
 @Column(nullable=false,unique=true,length=160) String chave;
 protected Tema() {}
 Tema(String nome,String chave){this.nome=nome;this.chave=chave;}
}
