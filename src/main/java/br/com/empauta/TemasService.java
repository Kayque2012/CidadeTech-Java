package br.com.empauta;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TemasService {
  private final Temas temas;
  private final Consultas consultas;

  TemasService(Temas t, Consultas c) {
    temas = t;
    consultas = c;
  }

  Page<Dto.TemaSaida> listar(String busca, Pageable pagina) {
    return temas.findByChaveContaining(chave(busca), pagina).map(TemasService::saida);
  }

  @Cacheable(cacheNames = "temas", key = "#id")
  public Dto.TemaSaida obter(long id) {
    return saida(entidade(id));
  }

  Tema entidade(long id) {
    return temas.findById(id).orElseThrow(() -> Falha.ausente("Tema não encontrado."));
  }

  @Transactional
  public Dto.TemaSaida criar(Dto.TemaEntrada d) {
    String chave = validarChave(d.nome());
    if (temas.existsByChave(chave)) throw Falha.conflito("Já existe um tema com esse nome.");
    return saida(temas.saveAndFlush(new Tema(d.nome().strip(), chave)));
  }

  @Transactional
  @CacheEvict(cacheNames = "temas", key = "#id")
  public Dto.TemaSaida editar(long id, Dto.TemaEntrada d) {
    var t = entidade(id);
    String chave = validarChave(d.nome());
    if (temas.existsByChaveAndIdNot(chave, id))
      throw Falha.conflito("Já existe um tema com esse nome.");
    t.nome = d.nome().strip();
    t.chave = chave;
    return saida(temas.saveAndFlush(t));
  }

  @Transactional
  @CacheEvict(cacheNames = "temas", key = "#id")
  public void excluir(long id) {
    var tema = entidade(id);
    if (consultas.existsByTemaId(id)) throw Falha.conflito("O tema possui consultas vinculadas.");
    temas.delete(tema);
    temas.flush();
  }

  private static String validarChave(String nome) {
    String chave = chave(nome);
    if (chave.isBlank() || chave.length() > 160)
      throw new Falha(org.springframework.http.HttpStatus.BAD_REQUEST, "Nome de tema inválido.");
    return chave;
  }

  static String chave(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .strip()
        .replaceAll("(?U)\\s+", " ")
        .toLowerCase(Locale.ROOT);
  }

  static Dto.TemaSaida saida(Tema t) {
    return new Dto.TemaSaida(t.getId(), t.getNome());
  }
}
