package br.com.empauta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
class Seguranca {
  static final String EMISSOR = "cidadao-em-pauta";

  @Bean
  SecretKeySpec chave(@Value("${app.jwt.secret}") String segredo, Environment env) {
    byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
    if (bytes.length == 0 && env.acceptsProfiles(Profiles.of("demo"))) {
      bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
    } else if (bytes.length < 32)
      throw new IllegalStateException("Defina JWT_SECRET com no mínimo 32 bytes aleatórios.");
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder encoder(SecretKeySpec key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder decoder(SecretKeySpec key) {
    var decoder = NimbusJwtDecoder.withSecretKey(key).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
    return decoder;
  }

  @Bean
  PasswordEncoder senha() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource cors(@Value("${app.cors.origins}") String origens) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(
        Arrays.stream(origens.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    c.setExposedHeaders(List.of("Location"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  @Bean
  SecurityFilterChain filtro(
      HttpSecurity http,
      ObjectMapper mapper,
      @org.springframework.beans.factory.annotation.Qualifier("cors")
          CorsConfigurationSource origem)
      throws Exception {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("papel");
    roles.setAuthorityPrefix("ROLE_");
    var conversor = new JwtAuthenticationConverter();
    conversor.setJwtGrantedAuthoritiesConverter(roles);
    return http.csrf(c -> c.disable())
        .cors(c -> c.configurationSource(origem))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/auth/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/actuator/health/readiness",
                        "/actuator/health/liveness",
                        "/actuator/info")
                    .permitAll()
                    .requestMatchers("/actuator/metrics", "/actuator/metrics/**")
                    .hasRole("GESTOR")
                    .requestMatchers(HttpMethod.POST, "/api/consultas/*/votos")
                    .hasRole("CIDADAO")
                    .requestMatchers(HttpMethod.GET, "/api/temas/**", "/api/consultas/**")
                    .permitAll()
                    .requestMatchers("/api/temas/**", "/api/consultas/**")
                    .hasRole("GESTOR")
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (r, s, x) -> {
                          s.setHeader("WWW-Authenticate", "Bearer");
                          erro(
                              mapper,
                              s,
                              401,
                              "Autenticação necessária ou token inválido.",
                              r.getRequestURI());
                        })
                    .accessDeniedHandler(
                        (r, s, x) ->
                            erro(
                                mapper,
                                s,
                                403,
                                "Seu papel não permite esta operação.",
                                r.getRequestURI())))
        .oauth2ResourceServer(
            o ->
                o.authenticationEntryPoint(
                        (r, s, x) -> {
                          s.setHeader("WWW-Authenticate", "Bearer");
                          erro(
                              mapper,
                              s,
                              401,
                              "Autenticação necessária ou token inválido.",
                              r.getRequestURI());
                        })
                    .jwt(j -> j.jwtAuthenticationConverter(conversor)))
        .build();
  }

  private static void erro(
      ObjectMapper mapper, HttpServletResponse s, int codigo, String msg, String path)
      throws java.io.IOException {
    s.setStatus(codigo);
    s.setContentType("application/json");
    mapper.writeValue(s.getOutputStream(), new Erro(codigo, msg, path, Map.of()));
  }
}
