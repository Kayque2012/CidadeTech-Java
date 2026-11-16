package br.com.empauta;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consultas")
public class ConsultasController {
  private final ConsultasService consultas;

  ConsultasController(ConsultasService c) {
    consultas = c;
  }

  @GetMapping
  @Operation(
      summary = "Pesquisar consultas por título, tema, bairro ou estado; filtros combináveis")
  public PagedModel<EntityModel<Dto.ConsultaSaida>> listar(
      @RequestParam(required = false) @Size(max = 140) String titulo,
      @RequestParam(required = false) @Positive Long temaId,
      @RequestParam(required = false) @Size(max = 80) String bairro,
      @RequestParam(required = false) Consulta.Estado estado,
      @ParameterObject @PageableDefault(sort = "criadaEm", direction = Sort.Direction.DESC)
          Pageable p,
      PagedResourcesAssembler<Dto.ConsultaSaida> paginas,
      Authentication auth) {
    boolean g = TemasController.editor(auth);
    boolean c = TemasController.papel(auth, "CIDADAO");
    return paginas.toModel(
        consultas.listar(
            titulo,
            temaId,
            bairro,
            estado,
            g,
            Paginas.limitar(p, Set.of("id", "titulo", "bairro", "estado", "criadaEm"))),
        d -> links(d, g, c));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar proposta, estado e ações disponíveis")
  public EntityModel<Dto.ConsultaSaida> obter(@PathVariable long id, Authentication auth) {
    boolean g = TemasController.editor(auth);
    return links(consultas.obter(id, g), g, TemasController.papel(auth, "CIDADAO"));
  }

  @PostMapping
  @Operation(
      summary = "Criar proposta em rascunho (gestor)",
      security = @SecurityRequirement(name = "jwt"))
  public ResponseEntity<EntityModel<Dto.ConsultaSaida>> criar(
      @Valid @RequestBody Dto.ConsultaEntrada d) {
    var m = links(consultas.criar(d), true, false);
    return ResponseEntity.created(m.getRequiredLink("self").toUri()).body(m);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Editar proposta em rascunho (gestor)",
      security = @SecurityRequirement(name = "jwt"))
  public EntityModel<Dto.ConsultaSaida> editar(
      @PathVariable long id, @Valid @RequestBody Dto.ConsultaEntrada d) {
    return links(consultas.editar(id, d), true, false);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Excluir proposta em rascunho (gestor)",
      security = @SecurityRequirement(name = "jwt"))
  public ResponseEntity<Void> excluir(@PathVariable long id) {
    consultas.excluir(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/abrir")
  @Operation(
      summary = "Publicar a proposta e abrir a votação (gestor)",
      security = @SecurityRequirement(name = "jwt"))
  public EntityModel<Dto.ConsultaSaida> abrir(@PathVariable long id) {
    return links(consultas.abrir(id), true, false);
  }

  @PatchMapping("/{id}/encerrar")
  @Operation(
      summary = "Encerrar a votação e disponibilizar resultado (gestor)",
      security = @SecurityRequirement(name = "jwt"))
  public EntityModel<Dto.ConsultaSaida> encerrar(@PathVariable long id) {
    return links(consultas.encerrar(id), true, false);
  }

  @PostMapping("/{id}/votos")
  @Operation(
      summary = "Registrar voto único (cidadão)",
      security = @SecurityRequirement(name = "jwt"))
  public ResponseEntity<Dto.Recibo> votar(
      @PathVariable long id,
      @Valid @RequestBody Dto.VotoEntrada d,
      @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.status(201)
        .body(consultas.votar(id, Long.parseLong(jwt.getSubject()), d.escolha()));
  }

  @GetMapping("/{id}/resultado")
  @Operation(summary = "Consultar total agregado da consulta encerrada")
  public EntityModel<Dto.Resultado> resultado(@PathVariable long id) {
    return EntityModel.of(
        consultas.resultado(id),
        linkTo(methodOn(ConsultasController.class).resultado(id)).withSelfRel(),
        linkTo(methodOn(ConsultasController.class).obter(id, null)).withRel("consulta"));
  }

  private EntityModel<Dto.ConsultaSaida> links(
      Dto.ConsultaSaida d, boolean gestor, boolean cidadao) {
    Link self = linkTo(methodOn(ConsultasController.class).obter(d.id(), null)).withSelfRel();
    var m =
        EntityModel.of(
            d,
            self,
            linkTo(methodOn(TemasController.class).obter(d.tema().id(), null)).withRel("tema"));
    if (d.estado() == Consulta.Estado.RASCUNHO && gestor) {
      m.add(
          Link.of(self.getHref(), "editar"),
          Link.of(self.getHref(), "excluir"),
          Link.of(self.getHref() + "/abrir", "abrir"));
    }
    if (d.estado() == Consulta.Estado.ABERTA) {
      if (gestor) m.add(Link.of(self.getHref() + "/encerrar", "encerrar"));
      if (cidadao) m.add(Link.of(self.getHref() + "/votos", "votar"));
    }
    if (d.estado() == Consulta.Estado.ENCERRADA)
      m.add(Link.of(self.getHref() + "/resultado", "resultado"));
    return m;
  }
}
