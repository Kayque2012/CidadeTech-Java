package br.com.empauta;

import java.util.Set;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;

final class Paginas {
  private Paginas() {}

  static Pageable limitar(Pageable p, Set<String> campos) {
    for (var o : p.getSort())
      if (!campos.contains(o.getProperty()))
        throw new Falha(HttpStatus.BAD_REQUEST, "Ordenação inválida. Campos: " + campos);
    var sort = p.getSort();
    if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
    return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort);
  }
}
