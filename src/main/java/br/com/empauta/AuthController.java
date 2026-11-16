package br.com.empauta;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final Contas contas;

  AuthController(Contas c) {
    contas = c;
  }

  @PostMapping("/cadastro")
  @Operation(summary = "Cadastrar cidadão; o papel é definido pelo servidor")
  public ResponseEntity<Dto.Conta> cadastro(@Valid @RequestBody Dto.Cadastro d) {
    return ResponseEntity.status(201).body(contas.cadastrar(d));
  }

  @PostMapping("/login")
  @Operation(summary = "Autenticar e emitir JWT com validade de 30 minutos")
  public Dto.Token login(@Valid @RequestBody Dto.Login d) {
    return contas.login(d);
  }
}
