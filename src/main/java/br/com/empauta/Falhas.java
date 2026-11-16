package br.com.empauta;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
record Erro(int status,String mensagem,String caminho,Map<String,String> campos){}
class Falha extends RuntimeException {
 final HttpStatus status;
 Falha(HttpStatus status,String mensagem){super(mensagem);this.status=status;}
 static Falha ausente(String mensagem){return new Falha(HttpStatus.NOT_FOUND,mensagem);}
 static Falha conflito(String mensagem){return new Falha(HttpStatus.CONFLICT,mensagem);}
}
@RestControllerAdvice
class Falhas {
 @ExceptionHandler(Falha.class)
 ResponseEntity<Erro> negocio(Falha e,HttpServletRequest r){return montar(e.status,e.getMessage(),r,Map.of());}
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<Erro> validacao(MethodArgumentNotValidException e,HttpServletRequest r){
  Map<String,String> campos=new TreeMap<>();
  e.getBindingResult().getFieldErrors().forEach(f->campos.putIfAbsent(f.getField(),f.getDefaultMessage()));
  return montar(HttpStatus.BAD_REQUEST,"Confira os campos enviados.",r,campos);
 }
 @ExceptionHandler({HandlerMethodValidationException.class,MethodArgumentTypeMismatchException.class,HttpMessageNotReadableException.class})
 ResponseEntity<Erro> formato(Exception e,HttpServletRequest r){return montar(HttpStatus.BAD_REQUEST,"Parâmetros ou JSON inválidos.",r,Map.of());}
 @ExceptionHandler(DataIntegrityViolationException.class)
 ResponseEntity<Erro> integridade(Exception e,HttpServletRequest r){return montar(HttpStatus.CONFLICT,"O registro é duplicado ou possui vínculos.",r,Map.of());}
 private ResponseEntity<Erro> montar(HttpStatus s,String m,HttpServletRequest r,Map<String,String> campos){
  return ResponseEntity.status(s).body(new Erro(s.value(),m,r.getRequestURI(),campos));
 }
}
