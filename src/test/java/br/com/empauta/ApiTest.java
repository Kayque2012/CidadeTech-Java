package br.com.empauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@TestExecutionListeners(
    listeners = DependencyInjectionTestExecutionListener.class,
    mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class ApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CacheManager cache;
  @Autowired Usuarios usuarios;
  @Autowired PasswordEncoder senhas;

  String login(String email, String senha) throws Exception {
    var response =
        mvc.perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content(json.writeValueAsString(Map.of("email", email, "senha", senha))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return "Bearer " + json.readTree(response.getContentAsByteArray()).path("token").asText();
  }

  String gestor() throws Exception {
    return login("gestor@example.test", "GestorDemo2026");
  }

  String cidadao() throws Exception {
    return login("cidadao@example.test", "CidadaoDemo2026");
  }

  long tema(String token, String nome) throws Exception {
    var r =
        mvc.perform(
                post("/api/temas")
                    .header("Authorization", token)
                    .contentType("application/json")
                    .content(json.writeValueAsString(Map.of("nome", nome))))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse();
    return json.readTree(r.getContentAsByteArray()).path("id").asLong();
  }

  String dados(long tema, String titulo) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "titulo",
            titulo,
            "descricao",
            "Proposta de demonstração do projeto.",
            "bairro",
            "Bairro Teste",
            "temaId",
            tema));
  }

  long consulta(String g, long t, String titulo) throws Exception {
    var res =
        mvc.perform(
                post("/api/consultas")
                    .header("Authorization", g)
                    .contentType("application/json")
                    .content(dados(t, titulo)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$._links.abrir.href").exists())
            .andReturn()
            .getResponse();
    return json.readTree(res.getContentAsByteArray()).path("id").asLong();
  }

  @Test
  void cadastroLoginEHash() throws Exception {
    String email = "novo@example.test", senha = "SenhaExemplo2026";
    String body =
        json.writeValueAsString(Map.of("nome", "Nova pessoa", "email", email, "senha", senha));
    mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.papel").value("CIDADAO"))
        .andExpect(jsonPath("$.senha").doesNotExist());
    assertThat(senhas.matches(senha, usuarios.findByEmail(email).orElseThrow().senhaHash)).isTrue();
    assertThat(login(email, senha)).startsWith("Bearer ey");
    mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(body))
        .andExpect(status().isConflict());
    String escalacao =
        json.writeValueAsString(
            Map.of(
                "nome",
                "Segunda conta",
                "email",
                "segunda@example.test",
                "senha",
                senha,
                "papel",
                "GESTOR"));
    mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(escalacao))
        .andExpect(status().isBadRequest());
  }

  @Test
  void segurancaAplica401403ETokenInvalido() throws Exception {
    mvc.perform(post("/api/temas").contentType("application/json").content("{\"nome\":\"Teste\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
    mvc.perform(
            post("/api/temas")
                .header("Authorization", cidadao())
                .contentType("application/json")
                .content("{\"nome\":\"Teste\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
    mvc.perform(get("/api/temas").header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"gestor@example.test\",\"senha\":\"incorreta\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void temaNormalizadoECrudComCache() throws Exception {
    String g = gestor();
    long id = tema(g, "Educação cívica");
    String rota = "/api/temas/" + id;
    mvc.perform(
            post("/api/temas")
                .header("Authorization", g)
                .contentType("application/json")
                .content("{\"nome\":\"  educacao   CIVICA  \"}"))
        .andExpect(status().isConflict());
    mvc.perform(get(rota))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._links.consultas.href")
                .value(org.hamcrest.Matchers.endsWith("?temaId=" + id)));
    assertThat(cache.getCache("temas").get(id)).isNotNull();
    mvc.perform(
            put(rota)
                .header("Authorization", g)
                .contentType("application/json")
                .content("{\"nome\":\"Educação popular\"}"))
        .andExpect(status().isOk());
    assertThat(cache.getCache("temas").get(id)).isNull();
    mvc.perform(get(rota)).andExpect(jsonPath("$.nome").value("Educação popular"));
    mvc.perform(delete(rota).header("Authorization", g)).andExpect(status().isNoContent());
    mvc.perform(get(rota)).andExpect(status().isNotFound());
  }

  @Test
  void consultaCrudRascunhoERelacionamento() throws Exception {
    String g = gestor();
    long t = tema(g, "Acessibilidade");
    long id = consulta(g, t, "Calçadas acessíveis");
    String rota = "/api/consultas/" + id;
    mvc.perform(get(rota)).andExpect(status().isNotFound());
    mvc.perform(get(rota).header("Authorization", g))
        .andExpect(jsonPath("$.tema.nome").value("Acessibilidade"));
    mvc.perform(
            put(rota)
                .header("Authorization", g)
                .contentType("application/json")
                .content(dados(t, "Calçadas revisadas")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titulo").value("Calçadas revisadas"));
    mvc.perform(delete("/api/temas/" + t).header("Authorization", g))
        .andExpect(status().isConflict());
    mvc.perform(delete(rota).header("Authorization", g)).andExpect(status().isNoContent());
    mvc.perform(get(rota).header("Authorization", g)).andExpect(status().isNotFound());
  }

  @Test
  void votoUnicoEncerramentoEApuracao() throws Exception {
    String g = gestor(), c = cidadao();
    long t = tema(g, "Cultura"), id = consulta(g, t, "Biblioteca comunitária");
    String rota = "/api/consultas/" + id;
    mvc.perform(patch(rota + "/abrir").header("Authorization", g))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ABERTA"));
    mvc.perform(get(rota).header("Authorization", c))
        .andExpect(jsonPath("$._links.votar.href").exists());
    mvc.perform(get(rota + "/resultado")).andExpect(status().isConflict());
    mvc.perform(
            put(rota)
                .header("Authorization", g)
                .contentType("application/json")
                .content(dados(t, "Nova versão")))
        .andExpect(status().isConflict());
    mvc.perform(
            post(rota + "/votos")
                .header("Authorization", g)
                .contentType("application/json")
                .content("{\"escolha\":\"FAVOR\"}"))
        .andExpect(status().isForbidden());
    for (int statusEsperado : List.of(201, 409))
      mvc.perform(
              post(rota + "/votos")
                  .header("Authorization", c)
                  .contentType("application/json")
                  .content("{\"escolha\":\"FAVOR\"}"))
          .andExpect(status().is(statusEsperado));
    mvc.perform(patch(rota + "/encerrar").header("Authorization", g))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links.resultado.href").exists());
    mvc.perform(
            post(rota + "/votos")
                .header("Authorization", c)
                .contentType("application/json")
                .content("{\"escolha\":\"CONTRA\"}"))
        .andExpect(status().isConflict());
    mvc.perform(get(rota + "/resultado"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.favor").value(1))
        .andExpect(jsonPath("$.contra").value(0))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.usuario").doesNotExist());
    assertThat(cache.getCache("resultados").get(id)).isNotNull();
  }

  @Test
  void specificationPaginaOrdenaEOcultaRascunho() throws Exception {
    String g = gestor();
    long t = tema(g, "Resíduos");
    for (String nome : List.of("Coleta Zona B", "Coleta Zona A")) {
      long id = consulta(g, t, nome);
      mvc.perform(patch("/api/consultas/" + id + "/abrir").header("Authorization", g))
          .andExpect(status().isOk());
    }
    consulta(g, t, "Coleta Zona 0");
    mvc.perform(
            get("/api/consultas")
                .param("temaId", Long.toString(t))
                .param("titulo", "COLETA")
                .param("bairro", "BAIRRO TESTE")
                .param("size", "1")
                .param("sort", "titulo,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(2))
        .andExpect(jsonPath("$.page.totalPages").value(2))
        .andExpect(jsonPath("$._embedded.*[0].titulo").value("Coleta Zona A"))
        .andExpect(jsonPath("$._links.next.href").exists());
    mvc.perform(get("/api/consultas").param("sort", "campoInexistente"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void entradaInvalidaEhPadronizada() throws Exception {
    String g = gestor();
    mvc.perform(
            post("/api/consultas")
                .header("Authorization", g)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.titulo").exists());
    mvc.perform(
            post("/api/consultas")
                .header("Authorization", g)
                .contentType("application/json")
                .content(dados(Long.MAX_VALUE, "Sem tema")))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/consultas/1/votos")
                .header("Authorization", cidadao())
                .contentType("application/json")
                .content("{\"escolha\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensagem").exists());
  }

  @Test
  void corsEActuator() throws Exception {
    mvc.perform(
            options("/api/consultas")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    mvc.perform(
            options("/api/consultas")
                .header("Origin", "https://origem-invalida.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/info")).andExpect(jsonPath("$.app.name").exists());
    mvc.perform(get("/actuator/metrics").header("Authorization", cidadao()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/actuator/metrics").header("Authorization", gestor()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.names").isArray());
  }

  @Test
  void swaggerExibeRotasRespostasESeguranca() throws Exception {
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/consultas/{id}/votos'].post.responses['201']").exists())
        .andExpect(jsonPath("$.paths['/api/consultas/{id}/votos'].post.responses['409']").exists())
        .andExpect(jsonPath("$.paths['/api/consultas/{id}/votos'].post.security[0].jwt").exists());
  }
}
