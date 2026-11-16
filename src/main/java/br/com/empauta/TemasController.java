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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/temas")
public class TemasController {
  private final TemasService temas;

  TemasController(TemasService t) {
    temas = t;
  }

  @GetMapping
  @Operation(summary = "Listar temas; busca ignora acentos e caixa")
  public PagedModel<EntityModel<Dto.TemaSaida>> listar(
      @RequestParam(defaultValue = "") @Size(max = 80) String busca,
      @ParameterObject @PageableDefault(sort = "nome") Pageable p,
      PagedResourcesAssembler<Dto.TemaSaida> paginas,
      Authentication auth) {
    return paginas.toModel(
        temas.listar(busca, Paginas.limitar(p, Set.of("id", "nome"))), t -> links(t, editor(auth)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detalhar um tema e acessar suas consultas")
  public EntityModel<Dto.TemaSaida> obter(@PathVariable long id, Authentication auth) {
    return links(temas.obter(id), editor(auth));
  }

  @PostMapping
  @Operation(summary = "Criar tema (gestor)", security = @SecurityRequirement(name = "jwt"))
  public ResponseEntity<EntityModel<Dto.TemaSaida>> criar(@Valid @RequestBody Dto.TemaEntrada d) {
    var model = links(temas.criar(d), true);
    return ResponseEntity.created(model.getRequiredLink("self").toUri()).body(model);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Editar tema (gestor)", security = @SecurityRequirement(name = "jwt"))
  public EntityModel<Dto.TemaSaida> editar(
      @PathVariable long id, @Valid @RequestBody Dto.TemaEntrada d) {
    return links(temas.editar(id, d), true);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Excluir tema sem consultas (gestor)",
      security = @SecurityRequirement(name = "jwt"))
  public ResponseEntity<Void> excluir(@PathVariable long id) {
    temas.excluir(id);
    return ResponseEntity.noContent().build();
  }

  private EntityModel<Dto.TemaSaida> links(Dto.TemaSaida d, boolean gestor) {
    Link self = linkTo(methodOn(TemasController.class).obter(d.id(), null)).withSelfRel();
    var m =
        EntityModel.of(
            d,
            self,
            Link.of(
                linkTo(ConsultasController.class)
                    .toUriComponentsBuilder()
                    .queryParam("temaId", d.id())
                    .build()
                    .toUriString(),
                "consultas"));
    if (gestor) m.add(Link.of(self.getHref(), "editar"), Link.of(self.getHref(), "excluir"));
    return m;
  }

  static boolean editor(Authentication a) {
    return papel(a, "GESTOR");
  }

  static boolean papel(Authentication a, String p) {
    return a != null
        && a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_" + p));
  }
}
