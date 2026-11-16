package br.com.empauta;

import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

@Configuration
class OpenApiConfig {
  @Bean
  OpenApiCustomizer respostas() {
    return api -> {
      var schema = io.swagger.v3.core.converter.ModelConverters.getInstance().read(Erro.class);
      schema.forEach(api.getComponents()::addSchemas);
      api.getPaths()
          .forEach(
              (path, item) ->
                  item.readOperationsMap()
                      .forEach(
                          (metodo, op) -> {
                            var responses = op.getResponses();
                            if (path.startsWith("/actuator/metrics")) {
                              op.addSecurityItem(
                                  new io.swagger.v3.oas.models.security.SecurityRequirement()
                                      .addList("jwt"));
                              responses.putIfAbsent("401", erro("Token ausente ou inválido"));
                              responses.putIfAbsent("403", erro("Acesso restrito ao gestor"));
                            }
                            if (path.startsWith("/api/")) {
                              boolean post = metodo.name().equals("POST"),
                                  del = metodo.name().equals("DELETE");
                              if ((post && !path.endsWith("/login")) || del) {
                                var sucesso = responses.remove("200");
                                if (sucesso != null) {
                                  sucesso.description(del ? "Excluído, sem conteúdo" : "Criado");
                                  if (del) sucesso.setContent(null);
                                  if (post
                                      && (path.equals("/api/temas")
                                          || path.equals("/api/consultas")))
                                    sucesso.addHeaderObject(
                                        "Location",
                                        new io.swagger.v3.oas.models.headers.Header()
                                            .description("URL do recurso criado")
                                            .schema(new StringSchema().format("uri")));
                                  responses.putIfAbsent(del ? "204" : "201", sucesso);
                                }
                              }
                              responses.putIfAbsent("400", erro("Parâmetros ou corpo inválidos"));
                              if (op.getSecurity() != null && !op.getSecurity().isEmpty()) {
                                responses.putIfAbsent("401", erro("Token ausente ou inválido"));
                                responses.putIfAbsent("403", erro("Papel sem permissão"));
                              }
                              if (path.endsWith("/login"))
                                responses.putIfAbsent("401", erro("Credenciais inválidas"));
                              if (path.contains("{id}") || path.equals("/api/consultas") && post)
                                responses.putIfAbsent("404", erro("Recurso não encontrado"));
                              if (post
                                  || del
                                  || metodo.name().equals("PUT")
                                  || metodo.name().equals("PATCH")
                                  || path.endsWith("/resultado"))
                                responses.putIfAbsent(
                                    "409", erro("Conflito de estado, vínculo ou duplicidade"));
                            }
                          }));
    };
  }

  private ApiResponse erro(String descricao) {
    return new ApiResponse()
        .description(descricao)
        .content(
            new Content()
                .addMediaType(
                    "application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/Erro"))));
  }
}
