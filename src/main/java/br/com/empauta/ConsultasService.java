package br.com.empauta;

import java.util.Locale;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ConsultasService {
  private final Consultas consultas;
  private final TemasService temas;
  private final Votos votos;
  private final Usuarios usuarios;

  ConsultasService(Consultas c, TemasService t, Votos v, Usuarios u) {
    consultas = c;
    temas = t;
    votos = v;
    usuarios = u;
  }

  Page<Dto.ConsultaSaida> listar(
      String titulo,
      Long temaId,
      String bairro,
      Consulta.Estado estado,
      boolean gestor,
      Pageable p) {
    Specification<Consulta> filtro = Specification.unrestricted();
    if (!gestor)
      filtro = filtro.and((r, q, b) -> b.notEqual(r.get("estado"), Consulta.Estado.RASCUNHO));
    if (titulo != null && !titulo.isBlank())
      filtro =
          filtro.and(
              (r, q, b) ->
                  b.gt(
                      b.locate(b.lower(r.get("titulo")), titulo.strip().toLowerCase(Locale.ROOT)),
                      0));
    if (temaId != null) filtro = filtro.and((r, q, b) -> b.equal(r.get("tema").get("id"), temaId));
    if (bairro != null && !bairro.isBlank())
      filtro =
          filtro.and(
              (r, q, b) ->
                  b.equal(b.lower(r.get("bairro")), bairro.strip().toLowerCase(Locale.ROOT)));
    if (estado != null) filtro = filtro.and((r, q, b) -> b.equal(r.get("estado"), estado));
    return consultas.findAll(filtro, p).map(this::saida);
  }

  Dto.ConsultaSaida obter(long id, boolean gestor) {
    var c = entidade(id);
    if (!gestor && c.estado == Consulta.Estado.RASCUNHO) throw ausente();
    return saida(c);
  }

  @Transactional
  public Dto.ConsultaSaida criar(Dto.ConsultaEntrada d) {
    var c = new Consulta();
    aplicar(c, d);
    return saida(consultas.saveAndFlush(c));
  }

  @Transactional
  public Dto.ConsultaSaida editar(long id, Dto.ConsultaEntrada d) {
    var c = bloquear(id);
    exigir(c, Consulta.Estado.RASCUNHO);
    aplicar(c, d);
    return saida(c);
  }

  @Transactional
  @CacheEvict(cacheNames = "resultados", key = "#id")
  public void excluir(long id) {
    var c = bloquear(id);
    exigir(c, Consulta.Estado.RASCUNHO);
    consultas.delete(c);
  }

  @Transactional
  @CacheEvict(cacheNames = "resultados", key = "#id")
  public Dto.ConsultaSaida abrir(long id) {
    var c = bloquear(id);
    exigir(c, Consulta.Estado.RASCUNHO);
    c.estado = Consulta.Estado.ABERTA;
    return saida(c);
  }

  @Transactional
  @CacheEvict(cacheNames = "resultados", key = "#id")
  public Dto.ConsultaSaida encerrar(long id) {
    var c = bloquear(id);
    exigir(c, Consulta.Estado.ABERTA);
    c.estado = Consulta.Estado.ENCERRADA;
    return saida(c);
  }

  @Transactional
  @CacheEvict(cacheNames = "resultados", key = "#id")
  public Dto.Recibo votar(long id, long usuarioId, Voto.Escolha escolha) {
    var c = bloquear(id);
    if (c.estado == Consulta.Estado.RASCUNHO) throw ausente();
    exigir(c, Consulta.Estado.ABERTA);
    var u =
        usuarios
            .findById(usuarioId)
            .orElseThrow(
                () ->
                    new Falha(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
    if (votos.existsByConsultaIdAndUsuarioId(id, usuarioId))
      throw Falha.conflito("Sua conta já votou nesta consulta.");
    var voto = votos.saveAndFlush(new Voto(c, u, escolha));
    return new Dto.Recibo(id, voto.escolha, voto.registradoEm);
  }

  @Cacheable(cacheNames = "resultados", key = "#id")
  public Dto.Resultado resultado(long id) {
    var c = entidade(id);
    if (c.estado == Consulta.Estado.RASCUNHO) throw ausente();
    exigir(c, Consulta.Estado.ENCERRADA);
    var contagem = new java.util.EnumMap<Voto.Escolha, Long>(Voto.Escolha.class);
    votos.contar(id).forEach(v -> contagem.put(v.getEscolha(), v.getQuantidade()));
    long f = contagem.getOrDefault(Voto.Escolha.FAVOR, 0L),
        n = contagem.getOrDefault(Voto.Escolha.CONTRA, 0L),
        a = contagem.getOrDefault(Voto.Escolha.ABSTENCAO, 0L);
    return new Dto.Resultado(id, f, n, a, f + n + a);
  }

  private void aplicar(Consulta c, Dto.ConsultaEntrada d) {
    c.titulo = d.titulo().strip();
    c.descricao = d.descricao().strip();
    c.bairro = d.bairro().strip();
    c.tema = temas.entidade(d.temaId());
  }

  private Consulta entidade(long id) {
    return consultas.findById(id).orElseThrow(ConsultasService::ausente);
  }

  private Consulta bloquear(long id) {
    return consultas.bloquear(id).orElseThrow(ConsultasService::ausente);
  }

  private static Falha ausente() {
    return Falha.ausente("Consulta não encontrada.");
  }

  private void exigir(Consulta c, Consulta.Estado estado) {
    if (c.estado != estado)
      throw Falha.conflito("Operação disponível somente no estado " + estado + ".");
  }

  private Dto.ConsultaSaida saida(Consulta c) {
    return new Dto.ConsultaSaida(
        c.id, c.titulo, c.descricao, c.bairro, TemasService.saida(c.tema), c.estado, c.criadaEm);
  }
}
