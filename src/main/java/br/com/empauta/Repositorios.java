package br.com.empauta;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

interface Temas extends JpaRepository<Tema,Long> {
 Page<Tema> findByChaveContaining(String chave,Pageable p);
 boolean existsByChaveAndIdNot(String chave,Long id);
 boolean existsByChave(String chave);
}
interface Consultas extends JpaRepository<Consulta,Long>,JpaSpecificationExecutor<Consulta> {
 boolean existsByTemaId(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select c from Consulta c where c.id=:id")
 Optional<Consulta> bloquear(@Param("id") long id);
}
interface Usuarios extends JpaRepository<Usuario,Long> { Optional<Usuario> findByEmail(String email); }
interface Votos extends JpaRepository<Voto,Long> {
 boolean existsByConsultaIdAndUsuarioId(Long consulta,Long usuario);
 interface Total { Voto.Escolha getEscolha(); long getQuantidade(); }
 @Query("select v.escolha as escolha, count(v) as quantidade from Voto v where v.consulta.id=:id group by v.escolha")
 List<Total> contar(@Param("id") long id);
}
